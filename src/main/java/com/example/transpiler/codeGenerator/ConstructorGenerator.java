package com.example.transpiler.codeGenerator;

import com.example.transpiler.codeGenerator.model.Constructor;
import com.example.transpiler.syntaxer.CompilationException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConstructorGenerator {

    public void generateConstructor(CompilationUnit cu, Constructor constructor) {

        ClassOrInterfaceDeclaration clazz = cu.getClassByName(constructor.getClassName())
            .orElseThrow(() -> new CompilationException("class name wasn't specified for provided constructor"));

        ConstructorDeclaration constructorDeclaration = clazz.addConstructor(Modifier.Keyword.PUBLIC);
        constructor.getParameters()
            .forEach(parameter -> constructorDeclaration.addAndGetParameter(
                parameter.getTypeName(),
                parameter.getName())
            );
        BlockStmt blockStmt = new BlockStmt();
        constructor.getAssignments()
            .forEach(assignment -> blockStmt.addStatement(
                new ExpressionStmt(
                    new AssignExpr(
                        new FieldAccessExpr(new ThisExpr(), assignment.getVarName()),
                        new NameExpr(assignment.getExpression()),
                        AssignExpr.Operator.ASSIGN
                        )
                )
            ));
        constructorDeclaration.setBody(blockStmt);

    }

    public void generateConstructor(ClassOrInterfaceDeclaration clazz, Constructor constructor) {

        ConstructorDeclaration constructorDeclaration = clazz.addConstructor(Modifier.Keyword.PUBLIC);
        constructor.getParameters()
            .forEach(parameter -> constructorDeclaration.addAndGetParameter(
                parameter.getTypeName(),
                parameter.getName())
            );
        BlockStmt blockStmt = new BlockStmt();
        constructor.getAssignments()
            .forEach(assignment -> blockStmt.addStatement(
                new ExpressionStmt(
                    new AssignExpr(
                        new FieldAccessExpr(new ThisExpr(), assignment.getVarName()),
                        new NameExpr(assignment.getExpression()),
                        AssignExpr.Operator.ASSIGN
                    )
                )
            ));
        constructorDeclaration.setBody(blockStmt);

    }

}
