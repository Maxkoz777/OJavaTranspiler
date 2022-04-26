package com.example.transpiler.typeChecker;

import com.example.transpiler.codeGenerator.model.Assignment;
import com.example.transpiler.codeGenerator.model.JavaType;
import com.example.transpiler.codeGenerator.model.Variable;
import com.example.transpiler.codeGenerator.model.VariableDeclaration;
import com.example.transpiler.syntaxer.FormalGrammar;
import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.syntaxer.TreeUtil;
import com.example.transpiler.util.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeChecker {

    public int treesCount = 0;
    private List<Tree> trees = new ArrayList<>();
    private boolean isTreeArrayReady = false;
    private List<DebtVariable> variablesToCheck = new ArrayList<>();

    public void check(Tree tree) {
        trees.add(tree);
        checkIfReadyToParseAllVariables();
        processCheckUnitForClass(TreeUtil.getAllVariablesForProgram(tree));

    }

    private void checkIfReadyToParseAllVariables() {
        treesCount--;
        if (treesCount == 0) {
            isTreeArrayReady = true;
        }
    }

    private void processCheckUnitForClass(CheckUnit unit) {
        List<Variable> variables = unit.getVariableDeclarations().stream()
            .map(variableDeclaration -> {
                Variable variable = new Variable(
                    variableDeclaration.getName(),
                    variableDeclaration.getType()
                );
                if (variableDeclaration.getType().equals(JavaType.UNDEFINED)) {
                    variable.setExpression(variable.getExpression());
                }
                return variable;
            })
            .toList();
        List<Pair<Variable, Long>> variableWithOccurrences = getVariableWithNumberOfAssignmentsInCode(variables, unit.getAssignments());
        List<Variable> problemVariables = variableWithOccurrences.stream()
            .filter(isNotStrictlyDefined)
            .map(Pair::getFirst)
            .toList();
        checkProblemVariables(problemVariables, unit.getAssignments());
        if (isTreeArrayReady) {
            majorCheck();
        }
    }

    private void majorCheck() {
        variablesToCheck.forEach(debtVariable -> {
            Set<String> types = debtVariable.getExpressionsWithTypes().stream()
                .map(x -> TypeChecker.typeForExpressionWithTypes(x, debtVariable.getName()))
                .collect(Collectors.toSet());
            if (types.size() > 1) {
                throw new TypeCheckerException(
                    String.format("Multiple types for variable %s provided", debtVariable.getName())
                );
            }
            if (types.isEmpty()) {
                throw new TypeCheckerException(
                    String.format("No type for variable %s found", debtVariable.getName())
                );
            }
        });

    }

    private String typeForExpressionWithTypes(Pair<String, ExpressionResult> pair, String name) {
        ExpressionResult expressionResult = pair.getSecond();
        String expression = pair.getFirst();
        List<FormalGrammar> filter;
        boolean isVariable = expressionResult.equals(ExpressionResult.VARIABLE);
        if (isVariable) {
            filter = List.of(FormalGrammar.ASSIGNMENT, FormalGrammar.VARIABLE_DECLARATION, FormalGrammar.MEMBER_DECLARATION);
        } else {
            filter = List.of(FormalGrammar.METHOD_DECLARATION);
        }
        List<Node> nodes = new ArrayList<>();
        trees.forEach(tree -> nodes.addAll(TreeUtil.inOrderSearch(tree, filter)));
        if (isVariable) {
            return typeForVariableDeclaration(nodes, expression, name);
        } else {
            return typeForMethodDeclaration(nodes, expression);
        }
    }

    private String typeForMethodDeclaration(List<Node> nodes, String name) {
        List<Node> properNodes = nodes.stream()
            .filter(node -> node.getChildNodes().get(0).getValue().equals(name))
            .toList();
        Set<String> types = properNodes.stream()
            .map(node -> node.getChildNodes().get(2).getValue())
            .collect(Collectors.toSet());
        if (types.size() > 1) {
            throw new TypeCheckerException(String.format("Multiple types for method %s provided", name));
        }
        return types.stream().findFirst()
            .orElseThrow(() -> new TypeCheckerException(String.format("No type for method %s provided", name)));
    }

    private String typeForVariableDeclaration(List<Node> nodes, String name, String initVariableName) {
        List<Node> notParameterNodes;
        List<String> declarations = TreeUtil.getVariableDeclarationsFromNodes(nodes.stream()
                                                                                      .filter(TreeUtil.isVariableDeclaration)
                                                                                      .toList())
            .stream().filter(variableDeclaration -> variableDeclaration.getName().equals(name))
            .map(VariableDeclaration::getExpression)
            .toList();
        List<String> assignments = TreeUtil.assignmentsFromNodes(nodes.stream()
                                                                     .filter(TreeUtil.isAssignment)
                                                                     .toList())
            .stream().filter(assignment -> assignment.getVarName().equals(name))
            .map(Assignment::getExpression)
            .toList();
        List<String> typesFromParameter;
        Set<String> types = new HashSet<>();
        types.addAll(declarations);
        types.addAll(assignments);
        if (types.size() > 1) {
            throw new TypeCheckerException(String.format("Multiple types for method %s provided", name));
        }
        return types.stream().findFirst()
            .orElseThrow(() -> new TypeCheckerException(String.format("No type for method %s provided", name)));
    }

    private void checkProblemVariables(List<Variable> problemVariables,
                                              List<Assignment> assignments) {
        problemVariables.forEach(variable -> {
            List<Assignment> assignmentsForName = assignments.stream()
                .filter(assignment -> assignment.getVarName().equals(variable.getName()))
                .toList();
            checkVariableAgainstAssignments(variable, assignmentsForName);
        });
    }

    private void checkVariableAgainstAssignments(Variable variable, List<Assignment> assignments) {
        List<String> expressions = new ArrayList<>();

        expressions.addAll(assignments.stream().map(Assignment::getExpression).toList());
        if (variable.getType().equals(JavaType.UNDEFINED)) {
            expressions.add(variable.getExpression());
        }
        populateDebtVariables(variable, expressions);
    }

    private void populateDebtVariables(Variable variable, List<String> expressions) {
        DebtVariable debtVariable = new DebtVariable(
            variable.getName(),
            expressions.stream().map(TypeChecker::getNameWithTypeOfExpression).toList()
        );
        variablesToCheck.add(debtVariable);
    }

    private Pair<String, ExpressionResult> getNameWithTypeOfExpression(String expression) {
        String[] elements = expression.split("\\.");
        String lastTerm = elements[elements.length - 1];
        ExpressionResult result = !lastTerm.contains("(") ? ExpressionResult.VARIABLE : ExpressionResult.METHOD;
        String name = result.equals(ExpressionResult.METHOD) ? lastTerm.substring(0, lastTerm.indexOf("(")) : lastTerm;
        return new Pair<>(name, result);
    }

    private final Predicate<Pair<Variable, Long>> isNotStrictlyDefined = pair -> pair.getSecond() > 0;

    private List<Pair<Variable, Long>> getVariableWithNumberOfAssignmentsInCode(List<Variable> variables, List<Assignment> assignments) {
        return variables.stream()
            .map(variable -> new Pair<>(variable, numberOfAssignmentsForVariable(variable, assignments)))
            .toList();
    }

    private Long numberOfAssignmentsForVariable(Variable variable, List<Assignment> assignments) {
        return assignments.stream()
            .filter(assignment -> assignment.getVarName().equals(variable.getName()))
            .count();
    }

}
