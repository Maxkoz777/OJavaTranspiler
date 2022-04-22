package com.example.transpiler.syntaxer;

import com.example.transpiler.codeGenerator.model.Assignment;
import com.example.transpiler.codeGenerator.model.Constructor;
import com.example.transpiler.codeGenerator.model.Variable;
import com.example.transpiler.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TreeUtil {

    private final Predicate<Node> isVariableDeclaration = node -> node.getType().equals(FormalGrammar.VARIABLE_DECLARATION);
    private final Predicate<Node> isIdentifier = node -> node.getType().equals(FormalGrammar.IDENTIFIER);
    private final Predicate<Node> isExpression = node -> node.getType().equals(FormalGrammar.EXPRESSION);
    private final Predicate<Node> isMember = node -> node.getType().equals(FormalGrammar.MEMBER_DECLARATION);
    private final Predicate<Node> isConstructor = node -> node.getType().equals(FormalGrammar.CONSTRUCTOR_DECLARATION);
    private final Predicate<Node> isParameterDeclaration = node -> node.getType().equals(FormalGrammar.PARAMETER_DECLARATION);
    private final Predicate<Node> isClassName = node -> node.getType().equals(FormalGrammar.CLASS_NAME);
    private final Predicate<Node> isStatement = node -> node.getType().equals(FormalGrammar.STATEMENT);
    private final Predicate<Node> isAssignment = node -> node.getType().equals(FormalGrammar.ASSIGNMENT);
    private final Function<Node, Stream<Node>> convertToChildNodes = nodes -> nodes.getChildNodes().stream();

    /**
     *
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
            .collect(Collectors.toList()));
        if (names.size() == 1) {
            names.add(null);
        }
        return new Pair<>(names.get(0), names.get(1));
    }

    /**
     *
     * @param tree is initial tree
     * @return node of main class
     */
    public Node getMainClassNode(Tree tree) {
        return tree.getRoot().getChildNodes().get(0);
    }

    /**
     *
     * @param node is a class node
     * @return list of class variables
     */
    public List<Variable> classVariables(Node node) {
        List<Node> variableNodes = node.getChildNodes().stream()
            .filter(isMember)
            .flatMap(convertToChildNodes)
            .filter(isVariableDeclaration)
            .collect(Collectors.toList());
        return variablesFromNodes(variableNodes);
    }

    /**
     *
     * @param variableNodes is a list of variable nodes
     * @return list of variables from list of nodes
     */
    private List<Variable> variablesFromNodes(List<Node> variableNodes) {
        List<Variable> variables = new ArrayList<>();
        List<String> names = variableNodes.stream()
            .flatMap(convertToChildNodes)
            .filter(isIdentifier)
            .map(Node::getValue)
            .collect(Collectors.toList());
        List<String> types = variableNodes.stream()
            .flatMap(convertToChildNodes)
            .filter(isExpression)
            .flatMap(convertToChildNodes)
            .map(Node::getValue)
            .collect(Collectors.toList());
        for (int i = 0; i < variableNodes.size(); i++) {
            variables.add(new Variable(names.get(i), types.get(i)));
        }
        return variables;
    }

    /**
     *
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
            .collect(Collectors.toList());
        for (Node cons : constructorNodes) {
            // Collecting parameters
            List<Variable> parameters = getParameters(cons.getChildNodes().get(0));
            Node body = cons.getChildNodes().get(1);
            // Collecting Assignments declarations inside constructor
            List<Node> assignments = cons.getChildNodes().get(1).getChildNodes().stream()
                .filter(isStatement)
                .flatMap(convertToChildNodes)
                .filter(isAssignment)
                .collect(Collectors.toList());
            List<Assignment> declaredAssignments = assignmentsFromNodes(assignments);
            constructors.add(new Constructor(parameters, declaredAssignments, body, className));
        }
        return constructors;

    }

    private String getClassNameForClassDeclarationNode(Node node) {
        if (node.getType() != FormalGrammar.CLASS_DECLARATION) {
            throw new CompilationException("Analysed node is not a class declaration node");
        }
        return node.getChildNodes().stream()
            .filter(isClassName)
            .findFirst().get()
            .getChildNodes().get(0)
            .getValue();
    }

    private List<Assignment> assignmentsFromNodes(List<Node> assignments) {
        List<Assignment> declaredAssignments = new ArrayList<>();
        assignments.forEach(assignment ->
                            {
                                String varName = assignment.getChildNodes().get(0).getValue();
                                String expression = expressionTypeToString(assignment.getChildNodes().get(1));
                                declaredAssignments.add(new Assignment(
                                    varName,
                                    expression
                                ));
                            });
        return declaredAssignments;
    }

    private String expressionTypeToString(Node node) {
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
                    if (i == childNodes.size() - 1) {
                        break;
                    } else {
                        builder.append(".");
                    }
                }
                case IDENTIFIER -> {
                    builder.append(child.getValue());
                    builder.append("(");
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
            }
        }
        return builder.toString();
    }



    /**
     *
     * @param cons is a member node
     * @return list of all parameters from member node
     */
    public List<Variable> getParameters(Node cons) {
        List<Variable> parameters = new ArrayList<>();
        List<Node> parameterDeclarations = cons.getChildNodes().stream()
            .filter(isParameterDeclaration)
            .flatMap(convertToChildNodes)
            .collect(Collectors.toList());
        List<String> parameterNames = parameterDeclarations.stream()
            .filter(isIdentifier)
            .map(Node::getValue)
            .collect(Collectors.toList());
        List<String> parameterTypes = parameterDeclarations.stream()
            .filter(isClassName)
            .flatMap(convertToChildNodes)
            .map(Node::getValue)
            .collect(Collectors.toList());
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.add(new Variable(parameterNames.get(i), parameterTypes.get(i)));
        }
        return parameters;
    }

}