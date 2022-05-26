package com.example.transpiler.syntaxer;

import com.example.transpiler.codeGenerator.model.*;
import com.example.transpiler.typeChecker.CheckUnit;
import com.example.transpiler.typeChecker.TypeChecker;
import com.example.transpiler.typeChecker.TypeCheckerException;
import com.example.transpiler.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TreeUtil {

    public final Predicate<Node> isVariableDeclaration = node -> node.getType()
        .equals(FormalGrammar.VARIABLE_DECLARATION);
    public final Predicate<Node> isIdentifier = node -> node.getType().equals(FormalGrammar.IDENTIFIER);
    public final Predicate<Node> isExpression = node -> node.getType().equals(FormalGrammar.EXPRESSION);
    public final Predicate<Node> isMember = node -> node.getType().equals(FormalGrammar.MEMBER_DECLARATION);
    public final Predicate<Node> isConstructor = node -> node.getType().equals(FormalGrammar.CONSTRUCTOR_DECLARATION);
    public final Predicate<Node> isBody = node -> node.getType().equals(FormalGrammar.BODY);

    public final Predicate<Node> isParameterDeclaration = node -> node.getType()
        .equals(FormalGrammar.PARAMETER_DECLARATION);
    public final Predicate<Node> isClassDeclaration = node -> node.getType().equals(FormalGrammar.CLASS_DECLARATION);
    public final Predicate<Node> isParameters = node -> node.getType().equals(FormalGrammar.PARAMETERS);
    public final Predicate<Node> isClassName = node -> node.getType().equals(FormalGrammar.CLASS_NAME);
    public final Predicate<Node> isStatement = node -> node.getType().equals(FormalGrammar.STATEMENT);
    public final Predicate<Node> isAssignment = node -> node.getType().equals(FormalGrammar.ASSIGNMENT);
    public final Predicate<Node> isFunctionDeclaration = node -> node.getType()
        .equals(FormalGrammar.FUNCTION_DECLARATION);
    public final Function<Node, Stream<Node>> convertToChildNodes = nodes -> nodes.getChildNodes().stream();

    /**
     * @param classNode is a class
     * @return pair of ClassName + Extended ClassName(can be null)
     */
    public Pair<String, String> getClassSignature(Node classNode) {
        List<String> names = new ArrayList<>(2);
        names.addAll(classNode.getChildNodes()
                         .stream()
                         .filter(node -> node.getType().equals(FormalGrammar.CLASS_NAME))
                         .flatMap(convertToChildNodes)
                         .map(Node::getValue)
                         .toList());
        if (names.size() == 1) {
            names.add(null);
        }
        return new Pair<>(names.get(0), names.get(1));
    }

    /**
     * @param tree is initial tree
     * @return node of main class
     */
    public Node getMainClassNode(Tree tree) {
        return tree.getRoot().getChildNodes().get(0);
    }

    /**
     * @param node is a class node
     * @return list of class variables
     */
    public List<Variable> getClassVariables(Node node) {
        List<Node> variableNodes = node.getChildNodes().stream()
            .filter(isMember)
            .flatMap(convertToChildNodes)
            .filter(isVariableDeclaration)
            .toList();
        return variablesFromNodes(variableNodes);
    }

    /**
     * @param variableNodes is a list of variable nodes
     * @return list of variables from list of nodes
     */
    private List<Variable> variablesFromNodes(List<Node> variableNodes) {
        List<Variable> variables = new ArrayList<>();
        List<String> names = variableNodes.stream()
            .flatMap(convertToChildNodes)
            .filter(isIdentifier)
            .map(Node::getValue)
            .toList();
        List<String> types = variableNodes.stream()
            .flatMap(convertToChildNodes)
            .filter(isExpression)
            .flatMap(convertToChildNodes)
            .map(Node::getValue)
            .toList();
        for (int i = 0; i < variableNodes.size(); i++) {
            variables.add(new Variable(names.get(i), types.get(i)));
        }
        return variables;
    }

    /**
     * @param node is a class node
     * @return list of all class constructors
     */
    public List<Constructor> getConstructors(Node node) {
        String className = getClassNameForClassDeclarationNode(node);
        List<Constructor> constructors = new ArrayList<>();
        List<Node> constructorNodes = node.getChildNodes().stream()
            .filter(isMember)
            .flatMap(convertToChildNodes)
            .filter(isConstructor)
            .toList();
        for (Node cons : constructorNodes) {
            // Collecting parameters
            List<Variable> parameters = getParameters(cons.getChildNodes().get(0));
            Node body = cons.getChildNodes().get(1);
            // Collecting Assignments declarations inside constructor
            List<Node> assignments = cons.getChildNodes().get(1).getChildNodes().stream()
                .filter(isStatement)
                .flatMap(convertToChildNodes)
                .filter(isAssignment)
                .toList();
            List<Assignment> declaredAssignments = assignmentsFromNodes(assignments);
            constructors.add(new Constructor(parameters, declaredAssignments, body, className));
        }
        return constructors;

    }

    public String getClassNameForClassDeclarationNode(Node node) {
        if (node.getType() != FormalGrammar.CLASS_DECLARATION) {
            throw new CompilationException("Analysed node is not a class declaration node");
        }
        return node.getChildNodes().stream()
            .filter(isClassName)
            .findFirst().orElseThrow(() -> new CompilationException("Wrong AST construction"))
            .getChildNodes().get(0)
            .getValue();
    }

    public List<Assignment> assignmentsFromNodes(List<Node> assignments) {
        List<Assignment> declaredAssignments = new ArrayList<>();
        assignments.forEach(assignment ->
                            {
                                String varName = assignment.getChildNodes().get(0).getValue();
                                String expression;
                                if (assignment.getChildNodes().get(1).getType().equals(FormalGrammar.EXPRESSION)) {
                                    expression = expressionTypeToString(assignment.getChildNodes().get(1));
                                } else {
                                    List<Node> children = assignment.getChildNodes().get(1).getChildNodes();
                                    expression = expressionTypeToString(children.get(0)) + " " +
                                        children.get(1).getValue() + " " + expressionTypeToString(children.get(2));
                                }
                                declaredAssignments.add(new Assignment(
                                    varName,
                                    expression,
                                    assignment
                                ));
                            });
        return declaredAssignments;
    }

    public String expressionTypeToString(Node node) {
        if (node.getType() != FormalGrammar.EXPRESSION) {
            throw new CompilationException("Analysed node is not a statement node");
        }
        List<Node> childNodes = node.getChildNodes();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < childNodes.size(); i++) {
            Node child = childNodes.get(i);
            switch (child.getType()) {
                case PRIMARY -> {
                    builder.append(child.getValue());
                    if (i != childNodes.size() - 1) {
                        builder.append(".");
                    }
                }
                case IDENTIFIER -> {
                    builder.append(child.getValue());
                    if (i != childNodes.size() - 1) {
                        if (childNodes.get(i + 1).getType().equals(FormalGrammar.IDENTIFIER)) {
                            builder.append(".");
                        } else {
                            builder.append("(");
                        }
                    }

                }
                case ARGUMENTS -> {
                    List<String> args = child.getChildNodes().stream()
                        .map(ex -> ex.getChildNodes().get(0).getValue())
                        .toList();
                    builder.append(String.join(", ", args));
                    builder.append(")");
                    if (i != childNodes.size() - 1) {
                        builder.append(".");
                    }
                }
                default -> throw new CompilationException("Unsupported AST type for expression");
            }
        }
        return builder.toString();
    }


    /**
     * @param cons is a member node
     * @return list of all parameters from member node
     */
    public List<Variable> getParameters(Node cons) {
        List<Variable> parameters = new ArrayList<>();
        List<Node> parameterDeclarations = cons.getChildNodes().stream()
            .filter(isParameterDeclaration)
            .flatMap(convertToChildNodes)
            .toList();
        List<String> parameterNames = parameterDeclarations.stream()
            .filter(isIdentifier)
            .map(Node::getValue)
            .toList();
        List<String> parameterTypes = parameterDeclarations.stream()
            .filter(isClassName)
            .flatMap(convertToChildNodes)
            .map(Node::getValue)
            .toList();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.add(new Variable(parameterNames.get(i), parameterTypes.get(i)));
        }
        return parameters;
    }

    public List<Node> inOrderSearch(Tree tree, List<FormalGrammar> filters) {
        filteredNodes = new ArrayList<>();

        findFilters(tree.getRoot(), filters);

        return filteredNodes;
    }

    private List<Node> filteredNodes;

    public void findFilters(Node node, List<FormalGrammar> filters) {
        if (filters.contains(node.getType())) {
            filteredNodes.add(node);
        }
        List<Node> childNodes = node.getChildNodes();
        for (Node childNode : childNodes) {
            findFilters(childNode, filters);
        }
    }

    public List<VariableDeclaration> getVariableDeclarationsFromNodes(List<Node> nodes) {
        if (!nodes.stream().allMatch(isVariableDeclaration)) {
            throw new CompilationException("Analysed node is not a variable declaration node");
        }
        List<VariableDeclaration> variableDeclarations = new ArrayList<>();
        nodes.forEach(node -> {
            String name = node.getChildNodes().get(0).getValue();
            JavaType type = JavaType.UNDEFINED;
            String evaluation = "";
            String typeName = "";
            Node expression = node.getChildNodes().get(1);
            if (expression.getChildNodes().size() == 1) {
                String potentialType = expression.getChildNodes().get(0).getValue();
                type = getTypeFromString(potentialType);
                if (TypeChecker.knownTypes.contains(potentialType)) {
                    typeName = potentialType;
                }
            } else {
                evaluation = expressionTypeToString(expression);
            }
            VariableDeclaration declaration = new VariableDeclaration(
                name,
                type,
                evaluation,
                node
            );
            declaration.setTypeName(typeName);
            variableDeclarations.add(declaration);
        });
        return variableDeclarations;
    }

    private JavaType getTypeFromString(String type) {
        return switch (type) {
            case "Integer" -> JavaType.INTEGER;
            case "Real" -> JavaType.REAL;
            case "Boolean" -> JavaType.BOOLEAN;
            default -> JavaType.UNDEFINED;
        };
    }

    public CheckUnit getAllVariablesForProgram(Tree tree) {
        List<Node> nodesForCheck = inOrderSearch(tree,
                                                 List.of(FormalGrammar.ASSIGNMENT, FormalGrammar.VARIABLE_DECLARATION));
        List<VariableDeclaration> declarations = getVariableDeclarationsFromNodes(nodesForCheck.stream()
                                                                                      .filter(isVariableDeclaration)
                                                                                      .toList());
        List<Assignment> assignments = assignmentsFromNodes(nodesForCheck.stream()
                                                                .filter(isAssignment)
                                                                .toList());
        return new CheckUnit(assignments, declarations);
    }

    private static List<Method> classMethods;

    public List<Method> getClassMethods(Node classNode) {
        classMethods = new ArrayList<>();
        for (Node node : classNode.getChildNodes()) {
            if (node.getType() == FormalGrammar.MEMBER_DECLARATION) {
                if (node.getChildNodes().get(0).getType() == FormalGrammar.METHOD_DECLARATION) {
                    Node methodNode = node.getChildNodes().get(0);
                    String methodName = methodNode.getChildNodes().get(0).getValue();
                    List<Node> methodParamsNodeList = methodNode.getChildNodes()
                        .stream()
                        .filter(isParameters).toList();
                    Node paramNode = methodParamsNodeList.isEmpty() ? null : methodParamsNodeList.get(0);

                    List<Node> methodBodyNodeList = methodNode.getChildNodes()
                        .stream()
                        .filter(isBody).toList();
                    Node bodyNode = methodBodyNodeList.isEmpty() ? null : methodBodyNodeList.get(0);

                    List<Node> methodIdentifierList = methodNode.getChildNodes()
                        .stream()
                        .filter(isIdentifier).skip(1).toList();
                    String identifier = methodIdentifierList.isEmpty() ? null : methodIdentifierList.get(0).getValue();

                    List<Parameter> parameters = new ArrayList<>();
                    if (paramNode != null) {
                        parameters = parameterNodeToParameters(paramNode);
                    }

                    Method method = new Method(methodName, parameters, identifier, bodyNode);
                    classMethods.add(method);
                }
            }
        }
        return classMethods;
    }

    public List<Parameter> parameterNodeToParameters(Node node) {
        return node.getChildNodes().stream()
            .map(param -> {
                List<Node> children = param.getChildNodes();
                String name = children.get(0).getValue();
                String type = children.get(1).getChildNodes().get(0).getValue();
                return new Parameter(name, type);
            })
            .toList();
    }

    /**
     * @param tree is a tree containing possible types
     * @return list of all types met in our tree (e.g. main class name + class name for all nested classes )
     */
    public List<String> getAllTypesForTree(Tree tree) {
        List<Node> classNodes = inOrderSearch(tree, List.of(FormalGrammar.CLASS_DECLARATION));
        return classNodes.stream().map(TreeUtil::getClassNameForClassDeclarationNode).toList();
    }


    /**
     *
     * @param tree
     * @param node
     * @return scope for node in tree
     */
    public Node getNodeScope(Tree tree, Node node) {
        return findNodeScope(tree.getRoot(), node, null);
    }

    public Node findNodeScope(Node n, Node s, Node currentScope) {
        if (n == s) {
            return currentScope;
        } else {
            List<FormalGrammar> declarations = List.of(
                FormalGrammar.METHOD_DECLARATION,
                FormalGrammar.CLASS_DECLARATION,
                FormalGrammar.CONSTRUCTOR_DECLARATION
            );
            if (declarations.contains(n.getType())) {
                currentScope = n;
            }

            for (Node child : n.getChildNodes()) {
                Node result = findNodeScope(child, s, currentScope);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * returns true if this IF_STATEMENT has else branch
     */
    public boolean isElseCondition(Node node) {
        int bodyCount = 0;
        for (Node childNode : node.getChildNodes()) {
            if (childNode.getType() == FormalGrammar.BODY) {
                bodyCount = bodyCount + 1;
            }
        }
        return bodyCount == 2;
    }

    /**
     *
     * @param node
     * @return variable declaration from variable declaration node
     */
    public static VariableDeclaration variableDeclarationFromNode(Node node) {
        List<VariableDeclaration> variableDeclarations = getVariableDeclarationsFromNodes(List.of(node));
        return variableDeclarations.get(0);
    }

    /**
     *
     * @param classNode
     * @return all nested classes for classnode
     */
    public static List<Node> getNestedClasses(Node classNode) {
        return classNode.getChildNodes().stream()
            .filter(isMember)
            .flatMap(convertToChildNodes)
            .filter(isClassDeclaration)
            .toList();
    }

    /**
     *
     * @param tree
     * @return name for tree
     */
    public String getNameForTree(Tree tree) {
        Node classNode = TreeUtil.getMainClassNode(tree);
        return TreeUtil.getClassSignature(classNode).getFirst();
    }

    /**
     *
     * @param className
     * @return tree for provided className
     */
    public Tree getTreeForClassName(String className) {
        return TypeChecker.trees.stream()
            .filter(tree -> tree.getClassName().equalsIgnoreCase(className))
            .findFirst()
            .orElseThrow(
                () -> new TypeCheckerException("No tree with class named: " + className)
            );
    }

    /**
     *
     * @param name
     * @param tree
     * @return method declaration entity with given name from provided tree
     */
    public Node getMethodDeclarationNodeByMethodName(String name, Tree tree) {
        methodDeclarations = new ArrayList<>();
        searchMethods(tree.getRoot(), name);
        if (methodDeclarations.size() > 1) {
            throw new CompilationException("Duplicated method declarations detected. Overloading is not supported.");
        } else if (methodDeclarations.isEmpty()) {
            return null;
        } else {
            return methodDeclarations.get(0);
        }
    }

    public List<Node> methodDeclarations;

    public void searchMethods(Node node, String name) {
        if (node.getType().equals(FormalGrammar.METHOD_DECLARATION) || node.getType()
            .equals(FormalGrammar.FUNCTION_DECLARATION)) {
            int index = node.getType().equals(FormalGrammar.METHOD_DECLARATION) ? 0 : 2;
            if (Objects.equals(node.getChildNodes().get(index).getValue(), name)) {
                methodDeclarations.add(node);
            }
            return;
        }

        List<Node> childNodes = node.getChildNodes();
        for (Node childNode : childNodes) {
            searchMethods(childNode, name);
        }
    }

    /**
     *
     * @param node
     * @return first-class function entity from node
     */
    public FirstClassFunction getFirstClassFunctionFromNode(Node node) {
        if (!node.getType().equals(FormalGrammar.FUNCTION_DECLARATION)) {
            throw new CompilationException("Analysed node is not a first-class function declaration node");
        }
        return null;
    }

    /**
     *
     * @param name
     * @param scope
     * @param tree
     * @return variable/parameter declaration for variable/parameter name in given scope
     * if no such variable in scope- returns null
     */
    public Node getVariableDeclarationByVariableName(String name, Node scope, Tree tree) {
        Node result;
        Node currentScope = scope;
        if (Objects.isNull(currentScope)) {
            currentScope = TreeUtil.getMainClassNode(tree);
        }
        do {
            result = findVariableDeclarationNodeInScopeByName(name, currentScope);
            if (result == null) {
                currentScope = getNodeScope(tree, currentScope);
                break;
            }
        } while (result == null && !currentScope.getType().equals(FormalGrammar.CLASS_DECLARATION));

        variableDeclarations = new ArrayList<>();
        parameterDeclarations = new ArrayList<>();

        searchParameters(currentScope, name);
        if (!parameterDeclarations.isEmpty()) {
            return parameterDeclarations.get(0);
        }

        searchVariables(currentScope, name);
        if (!variableDeclarations.isEmpty()) {
            return variableDeclarations.get(0);
        } else {
            return null;
        }
    }

    public List<Node> variableDeclarations;
    public List<Node> parameterDeclarations;

    /**
     *
     * @param name
     * @param scope
     * @return variable/parameter declaration for given name inside provided tree starting from inner scope
     *          and then going to CLASS_DECLARATION node if not found earlier
     */
    public Node findVariableDeclarationNodeInScopeByName(String name, Node scope) {
        variableDeclarations = new ArrayList<>();
        parameterDeclarations = new ArrayList<>();

        if (Objects.isNull(scope)) {
            throw new TypeCheckerException("No variable with name " + name);
        }

        // if scope is a method - check parameters first
        if (scope.getType() == FormalGrammar.METHOD_DECLARATION
            || scope.getType() == FormalGrammar.CONSTRUCTOR_DECLARATION) {
            searchParameters(scope, name);
            if (parameterDeclarations.isEmpty()) {
                searchVariables(scope, name);
                if (variableDeclarations.isEmpty()) {
                    return null;
                } else {
                    return variableDeclarations.get(0);
                }
            } else {
                return parameterDeclarations.get(0);
            }
        }
        if (scope.getType() == FormalGrammar.CLASS_DECLARATION) {
            searchVariables(scope, name);
            if (variableDeclarations.isEmpty()) {
                return null;
            } else {
                return variableDeclarations.get(0);
            }
        } else {
            return null;
        }
    }

    //........................................................................//
    /**
     *
     * @param name
     * @param node
     * @return fill the variable appearence in node by its declarations
     *
     */

    public void searchVariables(Node node, String name) {
        if (node.getType().equals(FormalGrammar.VARIABLE_DECLARATION)) {
            if (Objects.equals(node.getChildNodes().get(0).getValue(), name)) {
                variableDeclarations.add(node);
            }
            return;
        }

        List<Node> childNodes = node.getChildNodes();
        for (Node childNode : childNodes) {
            searchVariables(childNode, name);
        }
    }

    public void searchParameters(Node node, String name) {
        if (node.getType().equals(FormalGrammar.PARAMETER_DECLARATION)) {
            if (node.getChildNodes().get(0).getValue().equals(name)) {
                parameterDeclarations.add(node);
            }
            return;
        }

        List<Node> childNodes = node.getChildNodes();
        for (Node childNode : childNodes) {
            searchParameters(childNode, name);
        }
    }

    public FirstClassFunction getFunctionFromDeclarationNode(Node node) {
        if (!node.getType().equals(FormalGrammar.FUNCTION_DECLARATION)) {
            throw new CompilationException("Current node isn't a first-class function declaration node");
        }
        List<Node> children = node.getChildNodes();
        return new FirstClassFunction(
            children.get(2).getValue(),
            children.get(0).getValue(),
            children.get(1).getValue(),
            node,
            children.get(3).getValue(),
            TreeUtil.expressionTypeToString(children.get(4))
        );
    }

    public static List<FirstClassFunction> getFunctions(Node classNode) {
        return classNode.getChildNodes().stream()
            .filter(isMember)
            .flatMap(convertToChildNodes)
            .filter(isFunctionDeclaration)
            .map(TreeUtil::getFunctionFromDeclarationNode)
            .toList();
    }
}
