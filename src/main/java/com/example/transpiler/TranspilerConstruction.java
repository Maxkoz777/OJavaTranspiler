package com.example.transpiler;

import com.example.transpiler.codeGenerator.JavaCodeGenerator;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TranspilerConstruction {

    public static void main(String[] args) throws IOException {

        // main program
        TranspilerUtil.retrieveSourceLanguageLibraryFiles()
                .forEach(JavaCodeGenerator::generateJavaLibFile);

        TranspilerUtil.retrieveSourceLanguageFiles()
            .forEach(JavaCodeGenerator::generateJavaFile);

        // example parser
//        example();

    }

    private static void example() throws IOException {

        final String PATH = "src/main/java/com/example/transpiler/generated/";

        CompilationUnit cu = new CompilationUnit();

        cu.setPackageDeclaration("com.example.transpiler.generated");

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

        String FILE_NAME = PATH + "Book.java";
        Path path = Paths.get(FILE_NAME);
        Files.createFile(path);
        OutputStream out = new FileOutputStream(FILE_NAME);
        out.write(cu.toString().getBytes());
        out.close();
    }

}
