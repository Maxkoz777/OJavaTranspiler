package com.example.transpiler.typeChecker;

import com.example.transpiler.codeGenerator.model.Assignment;
import com.example.transpiler.codeGenerator.model.JavaType;
import com.example.transpiler.codeGenerator.model.Variable;
import com.example.transpiler.syntaxer.FormalGrammar;
import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.syntaxer.TreeUtil;
import com.example.transpiler.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeChecker {

    private String className;
    private List<Tree> trees = new ArrayList<>();
    private boolean isTreeArrayReady = false;
    private List<DebtVariable> variablesToCheck = new ArrayList<>();

    public void setIsTreeArrayReady() {
        isTreeArrayReady = true;
    }

    public void check(Tree tree) {
        trees.add(tree);
        className = TreeUtil.getClassSignature(TreeUtil.getMainClassNode(tree)).getFirst();
        processCheckUnitForClass(TreeUtil.getAllVariablesForProgram(tree));

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
            List<String> types = debtVariable.getExpressionsWithTypes().stream()
                .map(TypeChecker::typeForExpressionWithTypes)
                .toList();

        });

    }

    private String typeForExpressionWithTypes(Pair<String, ExpressionResult> pair) {
        ExpressionResult expressionResult = pair.getSecond();
        String expression = pair.getFirst();
        List<FormalGrammar> filter;
        boolean isVariable = expressionResult.equals(ExpressionResult.VARIABLE);
        if (isVariable) {
            filter = List.of(FormalGrammar.ASSIGNMENT, FormalGrammar.VARIABLE_DECLARATION);
        } else {
            filter = List.of(FormalGrammar.METHOD_DECLARATION);
        }
        List<Node> nodes = new ArrayList<>();
        trees.forEach(tree -> nodes.addAll(TreeUtil.inOrderSearch(tree, filter)));
        if (isVariable) {
            return typeForVariableDeclaration(nodes, expression);
        } else {
            return typeForMethodDeclaration(nodes, expression);
        }
    }

    // todo implement logic

    private String typeForMethodDeclaration(List<Node> nodes, String name) {
        return null;
    }

    private String typeForVariableDeclaration(List<Node> nodes, String name) {
        return null;
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
        List<String> expressions = assignments.stream().map(Assignment::getExpression).toList();
        if (variable.getType().equals(JavaType.UNDEFINED)) {
            expressions.add(variable.getExpression());
        }
        populateDebtVariables(variable, expressions);
        // todo remove logic below to another method
        Set<String> variantsForType = expressions.stream().map(TypeChecker::getTypeFromExpression).collect(Collectors.toSet());
        if (variantsForType.size() > 1) {
            throw new TypeCheckerException(
                String.format(
                    "Variable %s has more than 1 type for assignment in code: %n %s",
                    variable.getName(),
                    variantsForType
                )
            );
        }
    }

    private void populateDebtVariables(Variable variable, List<String> expressions) {
        DebtVariable debtVariable = new DebtVariable(
            variable.getName(),
            expressions.stream().map(TypeChecker::getNameWithTypeOfExpression).toList()
        );
        variablesToCheck.add(debtVariable);
    }

    private String getTypeFromExpression(String expression) {
        // todo implement logic
        List<Predicate<Node>> filter = new ArrayList<>();
        Pair<String, ExpressionResult> pair = getNameWithTypeOfExpression(expression);
        if (pair.getSecond().equals(ExpressionResult.VARIABLE)) {
            filter.addAll(List.of(TreeUtil.isVariableDeclaration, TreeUtil.isAssignment));
        }
        else {
            filter.add(TreeUtil.isMethodDeclaration);
        }
        List<Node> expectedNodes = new ArrayList<>();

        return null;
    }

    private Pair<String, ExpressionResult> getNameWithTypeOfExpression(String expression) {
        String[] elements = expression.split("\\.");
        String lastTerm = elements[elements.length - 1];
        ExpressionResult result = !lastTerm.contains("(") ? ExpressionResult.VARIABLE : ExpressionResult.METHOD;
        String name = result.equals(ExpressionResult.METHOD) ? lastTerm.substring(0, lastTerm.indexOf("(")) : lastTerm;
        return new Pair<>(name, result);
    }

    private final Predicate<Pair<Variable, Long>> isNotStrictlyDefined = pair -> pair.getSecond() == 0;

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
