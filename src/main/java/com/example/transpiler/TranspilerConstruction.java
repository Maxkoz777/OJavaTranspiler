package com.example.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

public class TranspilerConstruction {

    public static void main(String[] args) {

        CompilationUnit cu = new CompilationUnit();

        cu.setPackageDeclaration("jpexample.model");

        ClassOrInterfaceDeclaration book = cu.addClass("Book");
        book.addField("String", "title");
        book.addField("Person", "author");

        book.addConstructor(Modifier.PUBLIC)
            .addParameter("String", "title")
            .addParameter("Person", "author")
            .setBody(new BlockStmt()
                         .addStatement(new ExpressionStmt(new AssignExpr(
                             new FieldAccessExpr(new ThisExpr(), "title"),
                             new NameExpr("title"),
                             AssignExpr.Operator.ASSIGN)))
                         .addStatement(new ExpressionStmt(new AssignExpr(
                             new FieldAccessExpr(new ThisExpr(), "author"),
                             new NameExpr("author"),
                             AssignExpr.Operator.ASSIGN))));

        book.addMethod("getTitle", Modifier.PUBLIC).setBody(
            new BlockStmt().addStatement(new ReturnStmt(new NameExpr("title"))));

        book.addMethod("getAuthor", Modifier.PUBLIC).setBody(
            new BlockStmt().addStatement(new ReturnStmt(new NameExpr("author"))));

        System.out.println(cu.toString());

    }

}
