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

    // number of trees left to parse before global check
    public int treesCount = 0;
    // all trees for compilation
    public List<Tree> trees = new ArrayList<>();
    // flag showing necessity to start global check
    public boolean isTreeArrayReady = false;
    // all variables that will be checked
    private final List<DebtVariable> variablesToCheck = new ArrayList<>();
    // all type from all trees
    public List<String> knownTypes = new ArrayList<>();
    private Tree currentTree;
    // regex for real/integer values
    private final String NUMBER = "^(?=.)([+-]?([0-9]*)(\\.([0-9]+))?)$";

    public void check(Tree tree) {
        currentTree = tree;
        analyseTree(tree);
        checkIfReadyToParseAllVariables();
        processCheckUnitForClass(TreeUtil.getAllVariablesForProgram(tree));
    }

    /**
     * fills main fields
     * @param tree
     */
    private void analyseTree(Tree tree) {
        tree.setClassName(TreeUtil.getNameForTree(tree));
        trees.add(tree);
        knownTypes.addAll(TreeUtil.getAllTypesForTree(tree));
    }

    /**
     * Checks for necessity for global check
     */
    private void checkIfReadyToParseAllVariables() {
        treesCount--;
        if (treesCount == 0) {
            isTreeArrayReady = true;
        }
    }

    /**
     * Analyses all checkUnits for potential variables to check
     * @param unit
     */
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
        List<Pair<Variable, Long>> variableWithOccurrences = getVariableWithNumberOfAssignmentsInCode(variables,
                                                                                                      unit.getAssignments());
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
                VariableDeclaration declaration = TreeUtil.variableDeclarationFromNode(
                    debtVariable.getDeclarationNode());
                if (!knownTypes.contains(declaration.getTypeName())) {
                    throw new TypeCheckerException(
                        String.format("No defined type for variable %s found", debtVariable.getName())
                    );
                }
            }
        });

    }

    /**
     *
     * @param variableExpression
     * @param debtVariable
     * @return type for expression
     */
    private String typeForExpressionWithTypes(VariableExpression variableExpression, DebtVariable debtVariable) {

        if (isOperationIncluded(variableExpression)) {
            return typeForExpressionWithOperation(variableExpression, debtVariable);
        }

        if (variableExpression.getWholeExpression().matches(NUMBER)) {
            return typeForDigits(variableExpression.getWholeExpression());
        }

        if (isAnotherTreeSearch(variableExpression.getTerm())) {
            return typeFromSearchInAnotherTree(variableExpression);
        }

        if (variableExpression.getTerm().equals("this")) {
            String expression = variableExpression.getWholeExpression();
            TypeRecursiveDefinitionDto recursiveDefinitionDto = getTypeRecursiveDefinitionDto(
                expression.substring(expression.indexOf('.') + 1)
            );
            String term = recursiveDefinitionDto.getTerm();
            Tree tree = debtVariable.getTree();
            Node decl = recursiveDefinitionDto.getType().equals(ExpressionResult.METHOD)
                ? TreeUtil.getMethodDeclarationNodeByMethodName(term, tree)
                : TreeUtil.getVariableDeclarationByVariableName(
                    term,
                    TreeUtil.getMainClassNode(tree),
                    tree
                );
            return getTypeRecursively(
                term,
                decl,
                recursiveDefinitionDto.getExpression(),
                tree
            );
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

        TypeRecursiveDefinitionDto typeRecursiveDefinitionDto = getTypeRecursiveDefinitionDto(
            variableExpression.getWholeExpression());

        return getTypeRecursively(
            typeRecursiveDefinitionDto.getTerm(),
            termDeclaration,
            typeRecursiveDefinitionDto.getExpression(),
            debtVariable.getTree()
        );
    }

    /**
     *
     * @param variableExpression
     * @return type for expression if searching in another tree
     */
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

    /**
     *
     * @param term
     * @return true if need to search type in another tree
     */
    private boolean isAnotherTreeSearch(String term) {
        return knownTypes.contains(term);
    }

    /**
     *
     * @param expression
     * @return integer/real type for provided number
     */
    private String typeForDigits(String expression) {
        return expression.contains(".") ? "Real" : "Integer";
    }

    /**
     *
     * @param variableExpression
     * @param debtVariable
     * @return type for variableExpression inside debtVariable
     */
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
                () -> new TypeCheckerException(
                    "No valid operation provided for expression " + variableExpression.getWholeExpression())
            );
        String[] expressions = variableExpression.getWholeExpression().split(" ");

        TypeRecursiveDefinitionDto first = getTypeRecursiveDefinitionDto(expressions[0]);
        TypeRecursiveDefinitionDto second = getTypeRecursiveDefinitionDto(expressions[2]);

        String firstType = getTypeRecursively(
            first.getTerm(),
            first.getType().equals(ExpressionResult.METHOD) ?
                TreeUtil.getMethodDeclarationNodeByMethodName(first.getTerm(), tree) :
                TreeUtil.getVariableDeclarationByVariableName(first.getTerm(),
                                                              TreeUtil.getNodeScope(tree,
                                                                                    variableExpression.getAssignmentNode()),
                                                              tree
                ),
            first.getExpression(),
            tree
        );

        String secondType = getTypeRecursively(
            second.getTerm(),
            second.getType().equals(ExpressionResult.METHOD) ?
                TreeUtil.getMethodDeclarationNodeByMethodName(second.getTerm(), tree) :
                TreeUtil.getVariableDeclarationByVariableName(second.getTerm(),
                                                              TreeUtil.getNodeScope(tree,
                                                                                    variableExpression.getAssignmentNode()),
                                                              tree
                ),
            second.getExpression(),
            tree
        );

        Set<String> set = new HashSet<>();
        set.add(firstType.toUpperCase(Locale.ROOT));
        set.add(secondType.toUpperCase(Locale.ROOT));

        if (
            !firstType.equalsIgnoreCase(secondType)
                && !set.equals(Set.of("INTEGER", "REAL"))
        ) {
            throw new TypeCheckerException(
                "Trying to apply operation \"" + operation + "\" to types: " + firstType + " & " + secondType);
        }

        if (boolOperations.contains(operation)) {
            return "Boolean";
        } else {
            if (Stream.of("Real", "Char", "Integer", "String").anyMatch(x -> x.equalsIgnoreCase(firstType))) {
                return firstType;
            } else {
                throw new TypeCheckerException(
                    "Trying to apply " + operation + " to entities of non-lib type: " + firstType);
            }
        }

    }

    /**
     *
     * @param variableExpression
     * @return true if our expression is a math expression
     */
    private boolean isOperationIncluded(VariableExpression variableExpression) {
        List<String> operations = List.of("+", "-", "/", "==", ">=", ">", "<=", "<", "*");
        return operations.stream().anyMatch(op -> variableExpression.getWholeExpression().contains(op));
    }

    /**
     * Main method for type-checking process
     * @param term
     * @param termDeclaration
     * @param wholeExpression
     * @param tree
     * @return type for given expression
     */
    private String getTypeRecursively(String term, Node termDeclaration, String wholeExpression, Tree tree) {

        if (
            term.matches(NUMBER) ||
                wholeExpression.matches(NUMBER)) {

            return typeForDigits(wholeExpression);
        }

        String type;

        if (Objects.isNull(termDeclaration)) {
            throw new TypeCheckerException("No definition for variable " + term);
        }

        switch (termDeclaration.getType()) {
            case FUNCTION_DECLARATION, METHOD_DECLARATION -> {
                String returnType = getMethodReturnTypeByDeclaration(termDeclaration);
                if (wholeExpression.isEmpty()) {
                    type = returnType;
                } else {
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
            case PARAMETER_DECLARATION -> {
                String parameterType = getParameterTypeByDeclaration(termDeclaration);
                if (wholeExpression.isEmpty()) {
                    type = parameterType;
                } else {
                    TypeRecursiveDefinitionDto recursiveDefinitionDto = getTypeRecursiveDefinitionDto(wholeExpression);
                    String newTerm = recursiveDefinitionDto.getTerm();
                    Tree newTree;
                    try {
                        newTree = TreeUtil.getTreeForClassName(parameterType);
                    } catch (TypeCheckerException e) {
                        newTree = tree;
                    }
                    return getTypeRecursively(
                        newTerm,
                        recursiveDefinitionDto.getType().equals(ExpressionResult.METHOD) ?
                            TreeUtil.getMethodDeclarationNodeByMethodName(newTerm, newTree) :
                            TreeUtil.getVariableDeclarationByVariableName(newTerm,
                                                                          TreeUtil.getNodeScope(newTree,
                                                                                                TreeUtil.getMainClassNode(
                                                                                                    newTree)),
                                                                          newTree
                            ),
                        recursiveDefinitionDto.getExpression(),
                        newTree
                    );
                }
            }
            case VARIABLE_DECLARATION -> {
                if (wholeExpression.isEmpty()) {
                    VariableDeclaration declaration = TreeUtil.getVariableDeclarationsFromNodes(
                        List.of(termDeclaration)).get(0);
                    TypeRecursiveDefinitionDto recursiveDefinitionDto = getTypeRecursiveDefinitionDto(
                        declaration.getExpression());
                    String newTerm = recursiveDefinitionDto.getTerm();
                    if (Objects.nonNull(recursiveDefinitionDto.getTree()) && recursiveDefinitionDto.getTerm()
                        .isEmpty()) {
                        if (knownTypes.contains(newTerm)) {
                            return newTerm;
                        } else {
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
                                                                            TreeUtil.getMainClassNode(
                                                                                recursiveDefinitionDto.getTree()),
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
                        } else if (!declaration.getTypeName().isEmpty()) {
                            return declaration.getTypeName();
                        } else {
                            throw new TypeCheckerException(
                                "No type defined for variable " + term + " in class " + tree.getClassName());
                        }
                    }
                } else {
                    Tree treeForFutureSearch = TreeUtil.getTreeForClassName(
                        getTypeRecursively(
                            term,
                            termDeclaration,
                            "",
                            tree
                        )
                    );
                    TypeRecursiveDefinitionDto recursiveTypeDto = getTypeRecursiveDefinitionDto(wholeExpression);
                    if (recursiveTypeDto.getTree() == null) {
                        recursiveTypeDto.setTree(treeForFutureSearch);
                    }
                    if (recursiveTypeDto.getType().equals(ExpressionResult.METHOD)) {
                        return getTypeRecursively(
                            recursiveTypeDto.getTerm(),
                            TreeUtil.getMethodDeclarationNodeByMethodName(recursiveTypeDto.getTerm(),
                                                                          treeForFutureSearch),
                            recursiveTypeDto.getExpression(),
                            treeForFutureSearch
                        );
                    } else {
                        if (termDeclaration.getType().equals(FormalGrammar.PARAMETER_DECLARATION)) {
                            return getParameterTypeByDeclaration(termDeclaration);
                        } else {
                            return getTypeRecursively(
                                recursiveTypeDto.getTerm(),
                                termDeclaration,
                                recursiveTypeDto.getExpression(),
                                recursiveTypeDto.getTree()
                            );
                        }
                    }
                }

            }
            default -> throw new TypeCheckerException("Unsupported term type");
        }

        return type;
    }

    /**
     *
     * @param input
     * @return DTO for recursive type definition process
     */
    private static TypeRecursiveDefinitionDto getTypeRecursiveDefinitionDto(String input) {
        String wholeExpression = input;
        int dotPosition = wholeExpression.indexOf('.');
        if (dotPosition != -1 && wholeExpression.substring(0, dotPosition).equals("this")) {
            wholeExpression = input.substring(dotPosition + 1);
            dotPosition = wholeExpression.indexOf('.');
        }
        int bracketPosition = wholeExpression.indexOf('(');
        // if no "." and "(" inside expression => the last iteration
        if (dotPosition * bracketPosition == 1) {
            return new TypeRecursiveDefinitionDto(
                wholeExpression,
                "",
                ExpressionResult.VARIABLE,
                null
            );
        } else if (bracketPosition == -1 || dotPosition > -1 && dotPosition < bracketPosition) {
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
                } else if (updateBracketPosition == -1
                    || updatedDotPosition > -1 && updatedDotPosition < updateBracketPosition) {
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
        } else {
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

    /**
     *
     * @param parameterDeclaration
     * @return parameter type
     */
    private String getParameterTypeByDeclaration(Node parameterDeclaration) {
        return parameterDeclaration.getChildNodes().get(1)
            .getChildNodes().get(0).getValue();
    }

    /**
     *
     * @param methodDeclaration
     * @return type for declaration node based on type(function/method)
     */
    private String getMethodReturnTypeByDeclaration(Node methodDeclaration) {
        int index;
        if (methodDeclaration.getType().equals(FormalGrammar.METHOD_DECLARATION)) {
            index = 2;
        } else {
            index = 1;
        }
        return methodDeclaration.getChildNodes().get(index).getValue();
    }

    /**
     * groups problem variables with its assignments
     * @param problemVariables
     * @param assignments
     */
    private void checkProblemVariables(List<Variable> problemVariables,
                                       List<Assignment> assignments) {
        problemVariables.forEach(variable -> {
            List<Assignment> assignmentsForName = assignments.stream()
                .filter(assignment -> assignment.getVarName().equals(variable.getName()))
                .toList();
            checkVariableAgainstAssignments(variable, assignmentsForName);
        });
    }

    /**
     * groups variable with its assignments
     * @param variable
     * @param assignments
     */
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

    /**
     * fills List of conflicting variables
     * @param variable
     * @param expressionsWithNodes
     */
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

    /**
     *
     * @param expressionWithNode expressions with their definitions in tree
     * @return DTO for more convenient work with problem variables
     */
    private VariableExpression getNameWithTypeOfExpression(Map.Entry<Node, String> expressionWithNode) {
        String expression = expressionWithNode.getValue();
        Node node = expressionWithNode.getKey();
        String[] elements = expression.split("\\.");
        String firstTerm = elements[0];
        ExpressionResult result = !firstTerm.contains("(") ? ExpressionResult.VARIABLE : ExpressionResult.METHOD;
        String name =
            result.equals(ExpressionResult.METHOD) ? firstTerm.substring(0, firstTerm.indexOf("(")) : firstTerm;
        boolean isAssignment = !Objects.isNull(node);
        return new VariableExpression(name, result, expression, node, isAssignment);
    }

    /**
     * Predicate for choosing only potential conflicting types
     */
    private final Predicate<Pair<Variable, Long>> isNotStrictlyDefined = pair -> pair.getSecond() > 0;

    /**
     *
     * @param variables
     * @param assignments
     * @return List of pairs of variables with number of their assignments in code
     */
    private List<Pair<Variable, Long>> getVariableWithNumberOfAssignmentsInCode(List<Variable> variables,
                                                                                List<Assignment> assignments) {
        return variables.stream()
            .map(variable -> new Pair<>(variable, numberOfAssignmentsForVariable(variable, assignments)))
            .toList();
    }

    /**
     *
     * @param variable
     * @param assignments
     * @return number of assignments for current variable in code
     */
    private Long numberOfAssignmentsForVariable(Variable variable, List<Assignment> assignments) {
        return assignments.stream()
            .filter(assignment -> assignment.getVarName().equals(variable.getName()))
            .count();
    }

}
