package com.example.transpiler.typeChecker;

import com.example.transpiler.codeGenerator.model.Assignment;
import com.example.transpiler.codeGenerator.model.ClassDeclaration;
import com.example.transpiler.codeGenerator.model.JavaType;
import com.example.transpiler.codeGenerator.model.Method;
import com.example.transpiler.codeGenerator.model.Variable;
import com.example.transpiler.codeGenerator.model.VariableDeclaration;
import com.example.transpiler.syntaxer.FormalGrammar;
import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.syntaxer.TreeUtil;
import com.example.transpiler.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeChecker {

    public int treesCount = 0;
    public List<Tree> trees = new ArrayList<>();
    private List<ClassDeclaration> classDeclarations = new ArrayList<>();
    private boolean isTreeArrayReady = false;
    private List<DebtVariable> variablesToCheck = new ArrayList<>();
    private List<String> knownTypes = new ArrayList<>();
    private Tree currentTree;

    // Maps will consume memory, but we will have linear search for types among classes
    // instead of O(n^2)

    private Map<ClassDeclaration, List<Variable>> classAtributesMap = new HashMap<>();
    private Map<ClassDeclaration, List<Method>> classMethodsMap = new HashMap<>();

    public void check(Tree tree) {
        currentTree = tree;
        analyseTree(tree);
        checkIfReadyToParseAllVariables();
        processCheckUnitForClass(TreeUtil.getAllVariablesForProgram(tree));

    }

    private void analyseTree(Tree tree) {
        tree.setClassName(TreeUtil.getNameForTree(tree));
        trees.add(tree);
        knownTypes.addAll(TreeUtil.getAllTypesForTree(tree));
        List<Node> classNodes = TreeUtil.inOrderSearch(tree, List.of(FormalGrammar.CLASS_DECLARATION));
        classNodes.forEach(node -> {
            ClassDeclaration classDeclaration = new ClassDeclaration(node);
            classDeclarations.add(classDeclaration);
//            classAtributesMap.put(classDeclaration, TreeUtil.);
//            classMethodsMap.put(classDeclaration, TreeUtil.);
        });
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
                variable.setScope(
                    TreeUtil.getNodeScope(currentTree, variableDeclaration.getNode())
                );
                if (variableDeclaration.getType().equals(JavaType.UNDEFINED)) {
                    variable.setExpression(variableDeclaration.getExpression());
                }
                variable.setDeclarationNode(variableDeclaration.getNode());
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
                .map(x -> TypeChecker.typeForExpressionWithTypes(x, debtVariable))
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

    private String typeForExpressionWithTypes(VariableExpression variableExpression, DebtVariable debtVariable) {

        Node termDeclaration = TreeUtil.findVariableDeclarationNodeInScopeByName(
            variableExpression.getTerm(),
            debtVariable.getScope()
        );

        if (Objects.isNull(termDeclaration)) {

            switch (variableExpression.getType()) {
                case VARIABLE -> termDeclaration = TreeUtil.getVariableDeclarationByVariableName(
                    variableExpression.getTerm(),
                    debtVariable.getTree()
                );
                case METHOD -> termDeclaration = TreeUtil.getMethodDeclarationNodeByMethodName(
                    variableExpression.getTerm(),
                    debtVariable.getTree()
                );
            }

        }

        return getTypeRecursively(
            variableExpression.getTerm(),
            termDeclaration,
            variableExpression.getWholeExpression(),
            debtVariable.getTree()
        );
    }

    private String getTypeRecursively(String term, Node termDeclaration, String wholeExpression, Tree tree) {

        String type = null;

        if (wholeExpression.isEmpty()) {
            return tree.getClassName();
        }

        switch (termDeclaration.getType()) {
            case METHOD_DECLARATION -> type = getMethodReturnTypeByDeclaration(termDeclaration);
            case PARAMETER_DECLARATION -> type = getParameterTypeByDeclaration(termDeclaration);
            case VARIABLE_DECLARATION -> {
                // todo implement recursive logic
                type = null;
            }
        }

        return type;
    }

    private static String getParameterTypeByDeclaration(Node parameterDeclaration) {
        return parameterDeclaration.getChildNodes().get(1)
            .getChildNodes().get(0).getValue();
    }

    private static String getMethodReturnTypeByDeclaration(Node methodDeclaration) {
        return methodDeclaration.getChildNodes().get(2).getValue();
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
        Map<Node, String> expressionWithNodeMap = new HashMap<>();
        assignments.forEach(assignment -> expressionWithNodeMap.put(
            assignment.getNode(),
            assignment.getExpression()
        ));
        if (variable.getType().equals(JavaType.UNDEFINED)) {
            expressionWithNodeMap.put(null, variable.getExpression());
        }
        populateDebtVariables(variable, expressionWithNodeMap);
    }

    private void populateDebtVariables(Variable variable, Map<Node, String> expressionsWithNodes) {
        DebtVariable debtVariable = new DebtVariable(
            variable.getName(),
            currentTree,
            expressionsWithNodes.entrySet().stream().map(TypeChecker::getNameWithTypeOfExpression).toList(),
            variable.getDeclarationNode(),
            variable.getScope()
        );
        variablesToCheck.add(debtVariable);
    }

    private VariableExpression getNameWithTypeOfExpression(Map.Entry<Node, String> expressionWithNode) {
        String expression = expressionWithNode.getValue();
        Node node = expressionWithNode.getKey();
        String[] elements = expression.split("\\.");
        String lastTerm = elements[elements.length - 1];
        ExpressionResult result = !lastTerm.contains("(") ? ExpressionResult.VARIABLE : ExpressionResult.METHOD;
        String name = result.equals(ExpressionResult.METHOD) ? lastTerm.substring(0, lastTerm.indexOf("(")) : lastTerm;
        boolean isAssignment = !Objects.isNull(node);
        return new VariableExpression(name, result, expression, node, isAssignment);
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
            .filter(assignment -> TreeUtil.inScope(assignment.getNode(), variable.getScope()))
            .count();
    }

}
