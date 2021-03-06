package com.example.transpiler.codeGenerator;

import com.example.transpiler.codeGenerator.model.FirstClassFunction;
import com.example.transpiler.codeGenerator.model.Method;
import com.example.transpiler.codeGenerator.model.VariableDeclaration;
import com.example.transpiler.syntaxer.CompilationException;
import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.TreeUtil;
import com.example.transpiler.typeChecker.TypeCheckerException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MethodGenerator {


    /**
     * Generates method from provided components
     * @param cu
     * @param method
     * @param className
     */
    public void generateMethod(CompilationUnit cu, Method method, String className) {
        ClassOrInterfaceDeclaration clazz = cu.getClassByName(className)
            .orElseThrow(() -> new CompilationException("class name wasn't specified for provided method"));
        generateMethod(clazz, method, className);
    }

    /**
     * Generates method from provided components
     * @param clazz
     * @param method
     * @param className
     */
    public void generateMethod(ClassOrInterfaceDeclaration clazz, Method method, String className) {
        List<Node> bodyNodes = method.getBody().getChildNodes();
        MethodDeclaration methodDeclaration = clazz.addMethod(method.getName(), Keyword.PUBLIC);
        method.getParameters().forEach(parameter -> methodDeclaration.addAndGetParameter(
            parameter.getType(),
            parameter.getName()
        ));
        methodDeclaration.setType(method.getType());
        BlockStmt blockStmt = generateBody(bodyNodes);
        methodDeclaration.setBody(blockStmt);
    }

    private BlockStmt generateBody(List<Node> nodes) {
        BlockStmt blockStmt = new BlockStmt();
        nodes.stream()
            .map(MethodGenerator::generateStatement)
            .forEach(blockStmt::addStatement);
        return blockStmt;
    }

    private Statement generateStatement(Node node) {
        switch (node.getType()) {
            case VARIABLE_DECLARATION -> {
                return variableDeclarationStatement(node);
            }
            case FUNCTION_DECLARATION -> {
                return firstClassFunctionStatement(node);
            }
            case STATEMENT -> {
                return generateStatementCodeForMethod(node.getChildNodes().get(0));
            }
            default -> throw new CompilationException("Unsupported type for node in METHOD: " + node.getType());
        }
    }

    private Statement firstClassFunctionStatement(Node node) {
        FirstClassFunction function = TreeUtil.getFunctionFromDeclarationNode(node);
        String type = "Function<" + function.getInputType() + ", " + function.getOutputType() + ">";
        String expression = function.getVariable() + " -> " + function.getExpression();
        String statement = String.join(" ", type, function.getName(), "=", expression);
        Expression e = new NameExpr(statement);
        return new ExpressionStmt(e);
    }

    private Statement variableDeclarationStatement(Node node) {
        VariableDeclaration variableDeclaration = TreeUtil.variableDeclarationFromNode(node);
        Type type = new ClassOrInterfaceType("var");
        boolean hasExpression = !variableDeclaration.getExpression().isEmpty();
        Expression expression = hasExpression ? expressionFromString(variableDeclaration.getExpression()) : null;
        VariableDeclarator declarator;
        if (Objects.isNull(expression)) {
            declarator = new VariableDeclarator(
                type,
                variableDeclaration.getName()
            );
        }
        else {
            declarator = new VariableDeclarator(
                type,
                variableDeclaration.getName(),
                expression
            );
        }
        VariableDeclarationExpr expr = new VariableDeclarationExpr(declarator);
        return new ExpressionStmt(expr);
    }

    private Statement generateStatementCodeForMethod(Node node) {
        switch (node.getType()) {

            case ASSIGNMENT -> {
                return generateAssignmentForMethod(node);
            }
            case WHILE_LOOP -> {
                return generateWhileLoopForMethod(node);
            }
            case IF_STATEMENT -> {
                return generateConditionForMethod(node);
            }
            case RETURN_STATEMENT -> {
                return generateReturnForMethod(node);
            }
            default -> throw new CompilationException("Unsupported type for node in STATEMENT: " + node.getType());

        }
    }

    private Statement generateReturnForMethod(Node node) {
        ReturnStmt stmt = new ReturnStmt();
        stmt.setExpression(expressionFromNode(node.getChildNodes().get(0)));
        return stmt;
    }

    private Statement generateConditionForMethod(Node node) {
        IfStmt ifStmt = new IfStmt();
        List<Node> children = node.getChildNodes();
        Node expression = children.get(0);
        Expression e = expressionFromNode(expression);
        ifStmt.setCondition(e);
        ifStmt.setThenStmt(
            generateBody(children.get(1).getChildNodes())
        );
        if (TreeUtil.isElseCondition(node)) {
            ifStmt.setElseStmt(
                generateBody(children.get(2).getChildNodes())
            );
        }
        return ifStmt;
    }

    private Expression expressionFromNode(Node expression) {

        switch (expression.getType()) {
            case EXPRESSION -> {
                String line = TreeUtil.expressionTypeToString(expression);
                return expressionFromString(line);
            }
            case MATH_EXPRESSION -> {
                String first = TreeUtil.expressionTypeToString(expression.getChildNodes().get(0));
                String operation = expression.getChildNodes().get(1).getValue();
                String second = TreeUtil.expressionTypeToString(expression.getChildNodes().get(2));
                return expressionFromString(first + " " + operation + " " + second);
            }
            default -> throw new TypeCheckerException("Provided node should be an expression");
        }


    }

    private Expression expressionFromString(String line) {
        return new NameExpr(line);
    }

    private Statement generateWhileLoopForMethod(Node node) {
        WhileStmt whileStmt = new WhileStmt();
        Node expression = node.getChildNodes().get(0);
        List<Node> bodyNodes = node.getChildNodes().get(1).getChildNodes();
        whileStmt.setCondition(expressionFromNode(expression));
        whileStmt.setBody(generateBody(bodyNodes));
        return whileStmt;
    }

    private Statement generateAssignmentForMethod(Node node) {
        AssignExpr assignExpr = new AssignExpr();
        String name = node.getChildNodes().get(0).getValue();
        assignExpr.setTarget(new NameExpr(name));
        assignExpr.setValue(expressionFromNode(node.getChildNodes().get(1)));
        return new ExpressionStmt(assignExpr);
    }


}
