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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
                .map(x -> TypeChecker.typeForExpressionWithTypes(x, debtVariable).toUpperCase(Locale.ROOT))
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

        if (isOperationIncluded(variableExpression)) {
            return typeForExpressionWithOperation(variableExpression, debtVariable);
        }

        if (variableExpression.getWholeExpression().matches("^(?=.)([+-]?([0-9]*)(\\.([0-9]+))?)$")) {
            return typeForDigits(variableExpression.getWholeExpression());
        }

        if (isAnotherTreeSearch(variableExpression.getTerm())) {
            return typeFromSearchInAnotherTree(variableExpression);
        }

        Node termDeclaration = TreeUtil.findVariableDeclarationNodeInScopeByName(
            variableExpression.getTerm(),
            TreeUtil.getNodeScope(
                debtVariable.getTree(),
                variableExpression.isAssignment()
                    ? variableExpression.getAssignmentNode()
                    : debtVariable.getDeclarationNode()
            )
        );

        if (Objects.isNull(termDeclaration)) {

            switch (variableExpression.getType()) {
                case VARIABLE -> termDeclaration = TreeUtil.getVariableDeclarationByVariableName(
                    variableExpression.getTerm(),
                    debtVariable.getScope(),
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

    private static String typeFromSearchInAnotherTree(VariableExpression variableExpression) {
        TypeRecursiveDefinitionDto typeRecursiveDefinitionDto = getTypeRecursiveDefinitionDto(
            variableExpression.getWholeExpression()
        );
        Tree tree = typeRecursiveDefinitionDto.getTree();
        String term = typeRecursiveDefinitionDto.getTerm();
        Node termDeclaration = typeRecursiveDefinitionDto.getType().equals(ExpressionResult.METHOD)
            ? TreeUtil.getMethodDeclarationNodeByMethodName(term, tree)
            : TreeUtil.getVariableDeclarationByVariableName(
                term,
                TreeUtil.getMainClassNode(tree),
                tree
            );
        return getTypeRecursively(
            term,
            termDeclaration,
            typeRecursiveDefinitionDto.getExpression(),
            tree
        );
    }

    private boolean isAnotherTreeSearch(String term) {
        return knownTypes.contains(term);
    }

    private String typeForDigits(String expression) {
        return expression.contains(".") ? "Real" : "Integer";
    }

    private String typeForExpressionWithOperation(VariableExpression variableExpression,
                                                  DebtVariable debtVariable) {
        Tree tree = debtVariable.getTree();
        List<String> boolOperations = List.of("==", ">=", ">", "<=", "<");
        List<String> valueOperations = List.of("+", "-", "/", "*");
        List<String> operations = Stream.of(boolOperations, valueOperations)
            .flatMap(Collection::stream)
            .toList();
        String operation = operations.stream()
            .filter(op -> variableExpression.getWholeExpression().contains(op))
            .findFirst()
            .orElseThrow(
                () -> new TypeCheckerException("No valid operation provided for expression " + variableExpression.getWholeExpression())
            );
        String[] expressions = variableExpression.getWholeExpression().split(" ");

        TypeRecursiveDefinitionDto first = getTypeRecursiveDefinitionDto(expressions[0]);
        TypeRecursiveDefinitionDto second = getTypeRecursiveDefinitionDto(expressions[2]);

        String firstType = getTypeRecursively(
            first.getTerm(),
            first.getType().equals(ExpressionResult.METHOD) ?
                TreeUtil.getMethodDeclarationNodeByMethodName(first.getTerm(), tree) :
                TreeUtil.getVariableDeclarationByVariableName(first.getTerm(),
                                                              TreeUtil.getNodeScope(tree, variableExpression.getAssignmentNode()),
                                                              tree
                ),
            expressions[0],
            tree
        );

        String secondType = getTypeRecursively(
            second.getTerm(),
            second.getType().equals(ExpressionResult.METHOD) ?
                TreeUtil.getMethodDeclarationNodeByMethodName(second.getTerm(), tree) :
                TreeUtil.getVariableDeclarationByVariableName(second.getTerm(),
                                                              TreeUtil.getNodeScope(tree, variableExpression.getAssignmentNode()),
                                                              tree
                ),
            expressions[2],
            tree
        );

        if (
            !firstType.equals(secondType)
            && !Set.of(firstType.toUpperCase(Locale.ROOT), secondType.toUpperCase(Locale.ROOT))
                .equals(Set.of("INTEGER", "REAL"))
        ) {
            throw new TypeCheckerException("Trying to apply " + operation + " to types: " + firstType + " & " + secondType);
        }

        if (boolOperations.contains(operation)) {
            return "Boolean";
        } else {
            if (List.of("Real", "Char", "Integer", "String").contains(firstType)) {
                return firstType;
            } else {
                throw new TypeCheckerException("Trying to apply " + operation + " to entities of non-lib type: " + firstType);
            }
        }

    }

    private boolean isOperationIncluded(VariableExpression variableExpression) {
        List<String> operations = List.of("+", "-", "/", "==", ">=", ">", "<=", "<");
        return operations.stream().anyMatch(op -> variableExpression.getWholeExpression().contains(op));
    }

    private String getTypeRecursively(String term, Node termDeclaration, String wholeExpression, Tree tree) {

        if (
            term.matches("^(?=.)([+-]?([0-9]*)(\\.([0-9]+))?)$") ||
                wholeExpression.matches("^(?=.)([+-]?([0-9]*)(\\.([0-9]+))?)$")) {

            return typeForDigits(wholeExpression);
        }

        String type = null;

        switch (termDeclaration.getType()) {
            case METHOD_DECLARATION -> {
                String returnType = getMethodReturnTypeByDeclaration(termDeclaration);
                if (wholeExpression.isEmpty()) {
                    type = returnType;
                }
                else {
                    TypeRecursiveDefinitionDto recursiveTypeDto = getTypeRecursiveDefinitionDto(wholeExpression);
                    Tree newTree = trees.stream()
                        .filter(tree1 -> tree1.getClassName().equals(returnType))
                        .findFirst()
                        .orElseThrow(() -> new TypeCheckerException("No tree for class name: " + returnType));
                    String newTerm = recursiveTypeDto.getTerm();

                    type = getTypeRecursively(
                        newTerm,
                        recursiveTypeDto.getType().equals(ExpressionResult.METHOD) ?
                            TreeUtil.getMethodDeclarationNodeByMethodName(newTerm, newTree) :
                            TreeUtil.getVariableDeclarationByVariableName(newTerm,
                                                                          TreeUtil.getNodeScope(newTree,
                                                                                                TreeUtil.getMainClassNode(
                                                                                                    newTree)),
                                                                          newTree
                            ),
                        recursiveTypeDto.getExpression(),
                        newTree
                    );
                }
            }
            case PARAMETER_DECLARATION -> type = getParameterTypeByDeclaration(termDeclaration);
            case VARIABLE_DECLARATION -> {
                if (wholeExpression.isEmpty()) {
                    VariableDeclaration declaration = TreeUtil.getVariableDeclarationsFromNodes(List.of(termDeclaration)).get(0);
                    TypeRecursiveDefinitionDto recursiveDefinitionDto = getTypeRecursiveDefinitionDto(declaration.getExpression());
                    String newTerm = recursiveDefinitionDto.getTerm();
                    if (Objects.nonNull(recursiveDefinitionDto.getTree()) && recursiveDefinitionDto.getTerm().isEmpty()) {
                        if (knownTypes.contains(newTerm)) {
                            return newTerm;
                        }
                        else {
                            throw new TypeCheckerException("Unable to determine type from variable declaration");
                        }
                    }
                    if (Objects.isNull(recursiveDefinitionDto.getTree())) {
                        recursiveDefinitionDto.setTree(tree);
                    }
                    if (!newTerm.isEmpty()) {
                        Node newTermDeclaration = recursiveDefinitionDto.getType().equals(ExpressionResult.METHOD)
                            ? TreeUtil.getMethodDeclarationNodeByMethodName(newTerm, recursiveDefinitionDto.getTree())
                            : TreeUtil.getVariableDeclarationByVariableName(newTerm,
                                                                            TreeUtil.getMainClassNode(recursiveDefinitionDto.getTree()),
                                                                            recursiveDefinitionDto.getTree());
                        return getTypeRecursively(
                            newTerm,
                            newTermDeclaration,
                            recursiveDefinitionDto.getExpression(),
                            recursiveDefinitionDto.getTree()
                        );
                    } else {
                        if (!declaration.getType().equals(JavaType.UNDEFINED)) {
                            return declaration.getType().name();
                        } else {
                            throw new TypeCheckerException("No type defined for variable " + term + " in class " + tree.getClassName());
                        }
                    }
                }
                else {
                    TypeRecursiveDefinitionDto recursiveTypeDto = getTypeRecursiveDefinitionDto(wholeExpression);
                    if (recursiveTypeDto.getTree() == null) {
                        recursiveTypeDto.setTree(tree);
                    }
                    if (recursiveTypeDto.getType().equals(ExpressionResult.METHOD)) {
                        return getTypeRecursively(
                            recursiveTypeDto.getTerm(),
                            TreeUtil.getMethodDeclarationNodeByMethodName(recursiveTypeDto.getTerm(), tree),
                            recursiveTypeDto.getExpression(),
                            tree
                        );
                    } else {
                        Node declaration = TreeUtil.getDeclarationNodeForLocalName(
                            recursiveTypeDto.getTerm(),
                            termDeclaration,
                            recursiveTypeDto.getTree()
                        );
                        if (declaration.getType().equals(FormalGrammar.PARAMETER_DECLARATION)) {
                            return getParameterTypeByDeclaration(declaration);
                        } else {
                            return getTypeRecursively(
                                recursiveTypeDto.getTerm(),
                                declaration,
                                recursiveTypeDto.getExpression(),
                                recursiveTypeDto.getTree()
                            );
                        }
                    }
                }

            }
        }

        return type;
    }

    private static TypeRecursiveDefinitionDto getTypeRecursiveDefinitionDto(String wholeExpression) {
        int dotPosition = wholeExpression.indexOf('.');
        int bracketPosition = wholeExpression.indexOf('(');
        // if no "." and "(" inside expression => the last iteration
        if (dotPosition * bracketPosition == 1) {
            return new TypeRecursiveDefinitionDto(
                wholeExpression,
                "",
                ExpressionResult.VARIABLE,
                null
            );
        }
        else if (bracketPosition == -1 || dotPosition > -1 && dotPosition < bracketPosition) {
            String newTerm = wholeExpression.substring(0, dotPosition);
            String expression = wholeExpression.substring(dotPosition + 1);
            Tree tree = null;
            ExpressionResult expressionResult = ExpressionResult.VARIABLE;
            if (knownTypes.contains(newTerm)) {
                String finalNewTerm = newTerm;
                tree = trees.stream()
                    .filter(t -> t.getClassName().equals(finalNewTerm))
                    .findFirst()
                    .orElseThrow(
                        () -> new TypeCheckerException("No tree with name " + finalNewTerm + " exist in trees")
                    );
                int updatedDotPosition = expression.indexOf('.');
                int updateBracketPosition = expression.indexOf('(');
                if (updatedDotPosition * updateBracketPosition == 1) {
                    newTerm = expression;
                    expression = "";
                } else if (updateBracketPosition == -1 || updatedDotPosition > -1 && updatedDotPosition < updateBracketPosition) {
                    newTerm = expression.substring(0, updatedDotPosition);
                    expression = expression.substring(updatedDotPosition + 1);
                } else {
                    newTerm = expression.substring(0, updateBracketPosition);
                    expression = expression.substring(expression.indexOf(')') + 1);
                    expressionResult = ExpressionResult.METHOD;
                }
            }
            return new TypeRecursiveDefinitionDto(
                newTerm,
                expression,
                expressionResult,
                tree
            );
        }
        else {
            String newTerm = wholeExpression.substring(0, bracketPosition);
            int closingBracketPosition = wholeExpression.indexOf(')');
            String expression = wholeExpression.substring(closingBracketPosition + 1);
            return new TypeRecursiveDefinitionDto(
                newTerm,
                expression,
                ExpressionResult.METHOD,
                null
            );
        }
    }

    private String getParameterTypeByDeclaration(Node parameterDeclaration) {
        return parameterDeclaration.getChildNodes().get(1)
            .getChildNodes().get(0).getValue();
    }

    private String getMethodReturnTypeByDeclaration(Node methodDeclaration) {
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
        String firstTerm = elements[0];
        ExpressionResult result = !firstTerm.contains("(") ? ExpressionResult.VARIABLE : ExpressionResult.METHOD;
        String name = result.equals(ExpressionResult.METHOD) ? firstTerm.substring(0, firstTerm.indexOf("(")) : firstTerm;
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
