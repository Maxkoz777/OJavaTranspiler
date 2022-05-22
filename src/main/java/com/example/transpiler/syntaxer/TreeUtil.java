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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TreeUtil {

    public final Predicate<Node> isVariableDeclaration = node -> node.getType().equals(FormalGrammar.VARIABLE_DECLARATION);
    public final Predicate<Node> isIdentifier = node -> node.getType().equals(FormalGrammar.IDENTIFIER);
    public final Predicate<Node> isExpression = node -> node.getType().equals(FormalGrammar.EXPRESSION);
    public final Predicate<Node> isMember = node -> node.getType().equals(FormalGrammar.MEMBER_DECLARATION);
    public final Predicate<Node> isConstructor = node -> node.getType().equals(FormalGrammar.CONSTRUCTOR_DECLARATION);
    public final Predicate<Node> isBody = node -> node.getType().equals(FormalGrammar.BODY);

    public final Predicate<Node> isParameterDeclaration = node -> node.getType().equals(FormalGrammar.PARAMETER_DECLARATION);

    public final Predicate<Node> isParameters = node -> node.getType().equals(FormalGrammar.PARAMETERS);
    public final Predicate<Node> isClassName = node -> node.getType().equals(FormalGrammar.CLASS_NAME);
    public final Predicate<Node> isStatement = node -> node.getType().equals(FormalGrammar.STATEMENT);
    public final Predicate<Node> isAssignment = node -> node.getType().equals(FormalGrammar.ASSIGNMENT);
    public final Predicate<Node> isMethodDeclaration = node -> node.getType().equals(FormalGrammar.METHOD_DECLARATION);
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
            String expression = expressionTypeToString(assignment.getChildNodes().get(1));
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
        List<Node> result = new ArrayList<>();
        List<Node> nodes = tree.getRoot().getChildNodes();
        result.addAll(nodes.stream().filter(node -> filters.contains(node.getType())).toList());
        filteredNodes = new ArrayList<>();

        findFilters(tree.getRoot(), filters);

        return filteredNodes;
    }

    public List<Node> filteredNodes;

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
            Node expression = node.getChildNodes().get(1);
            if (expression.getChildNodes().size() == 1) {
                type = getTypeFromString(expression.getChildNodes().get(0).getValue());
            } else {
                evaluation = expressionTypeToString(expression);
            }
            variableDeclarations.add(new VariableDeclaration(
                    name,
                    type,
                    evaluation,
                    node
            ));
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
        List<Node> nodesForCheck = inOrderSearch(tree, List.of(FormalGrammar.ASSIGNMENT, FormalGrammar.VARIABLE_DECLARATION));
        List<VariableDeclaration> declarations = getVariableDeclarationsFromNodes(nodesForCheck.stream()
                .filter(isVariableDeclaration)
                .toList());
        List<Assignment> assignments = assignmentsFromNodes(nodesForCheck.stream()
                .filter(isAssignment)
                .toList());
        return new CheckUnit(assignments, declarations);
    }

//    public static ArrayList<Node> methods;
//    public ArrayList<Node> getMethods(Node classNode) {
//        System.out.println(classNode);
//
//        filteredNodes = new ArrayList<Node>();
//        List<FormalGrammar> filters = new ArrayList<FormalGrammar>();
//        filters.add(FormalGrammar.METHOD_DECLARATION);
//        findFilters(classNode, filters);
//
//        return (ArrayList<Node>) filteredNodes;
//
//    }


    public static ArrayList<Method> classMethods;

    public ArrayList<Method> getClassMethods(Node classNode) {
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
     * @return list of all types met in our tree (e.g. main class name + class name fro )
     */
    public List<String> getAllTypesForTree(Tree tree) {
        List<Node> classNodes = inOrderSearch(tree, List.of(FormalGrammar.CLASS_DECLARATION));
        return classNodes.stream().map(TreeUtil::getClassNameForClassDeclarationNode).toList();
    }


    public Node getNodeScope(Tree tree, Node node) {
        Node res = findNodeScope(tree.getRoot(), node, null);
        return res;
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
        Integer bodyCount = 0;
        for (Node childNode : node.getChildNodes()) {
            if (childNode.getType() == FormalGrammar.BODY) {
                bodyCount = bodyCount + 1;
            }
        }
        return bodyCount == 2;
    }

    public static VariableDeclaration variableDeclarationFromNode(Node node) {
        List<VariableDeclaration> variableDeclarations = getVariableDeclarationsFromNodes(List.of(node));
        return variableDeclarations.get(0);
    }

    public static List<Node> getNestedClasses(Node classNode) {
        // todo get all class declarations for given class
        return null;
    }


    public boolean inScope(Node target, Node scope) {
        // todo return true if target is inside scope
        // todo it can be a child of a child of provided scope with any depth of such including
        return true;
    }

    public String getNameForTree(Tree tree) {
        Node classNode = TreeUtil.getMainClassNode(tree);
        return TreeUtil.getClassSignature(classNode).getFirst();
    }

    public Tree getTreeForClassName(String className) {
        return TypeChecker.trees.stream()
                .filter(tree -> tree.getClassName().equals(className))
                .findFirst()
                .orElseThrow(
                        () -> new TypeCheckerException("No tree with class named: " + className)
                );
    }

    public String getInferredTypeForNodeInClass(Node node, String className) {
        Tree tree = TreeUtil.getTreeForClassName(className);
        // todo implement type inference (I'll do it myself later)
        return "var";
    }

    public Node getMethodDeclarationNodeByMethodName(String name, Tree tree) {
        methodDeclarations = new ArrayList<>();
        searchMethods(tree.getRoot(), name);
        if (methodDeclarations.size()>1){
            throw new CompilationException("Duplicated method declarations detected. Overloading is not supported.");
        }
        else if (methodDeclarations.size()==0){
            return null;
        }
        else return methodDeclarations.get(0);
    }

    public List<Node> methodDeclarations;

    public void searchMethods(Node node, String name) {
        if (node.getType().equals(FormalGrammar.METHOD_DECLARATION)) {
            if (Objects.equals(node.getChildNodes().get(0).getValue(), name)) {
                methodDeclarations.add(node);
            }
            return;
        }

        List<Node> childNodes = node.getChildNodes();
        for (Node childNode : childNodes) {
            searchMethods(childNode, name);
        }
    }

    public Node getVariableDeclarationByVariableName(String name, Tree tree) {
        // todo find decl node for method name if such method exist

        return null;
    }

    public Node findVariableDeclarationNodeInScopeByName(String name, Node scope) {
        // todo in scope-Node find variable declaration node with provided name
        // if scope is a method - check parameters first
        return null;
    }

    public Node getDeclarationNodeForLocalName(String term, Node expressionDeclaration, Tree tree) {
        // todo term - variable name inside some expression from "expression declaration"
        // todo find a declaration(VARIABLE_DECLARATION or PARAMETER_DECLARATION) for term
        return null;
    }
}
