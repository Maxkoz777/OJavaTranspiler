package com.example.transpiler.codeGenerator;


import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.syntaxer.TreeUtil;
import com.example.transpiler.util.Pair;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClassGenerator {

    private final String PATH = "src/main/java/com/example/transpiler/generated/";
    private final String PACKAGE = "com.example.transpiler.generated";

    public void generateClass(Tree tree, ClassType classType) {

        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration(getPackage(classType));

        Node classNode = TreeUtil.getMainClassNode(tree);

        Pair<String, String> signature = TreeUtil.getClassSignature(classNode);

        ClassOrInterfaceDeclaration mainClass = cu.addClass(signature.getFirst());
        if (!Objects.isNull(signature.getSecond()) && !signature.getSecond().isEmpty()) {
            mainClass.addExtends(signature.getSecond());
        }

        TreeUtil.getConstructors(classNode).forEach(ConstructorGenerator::generateConstructor);

//        generateCode(cu, classType, signature.getFirst());

    }

    private void generateCode(CompilationUnit cu, ClassType type, String name) {
        saveFile(
            cu.toString().getBytes(StandardCharsets.UTF_8),
            getFilePath(name, type)
        );
    }

    private String getFilePath(String name, ClassType classType) {
        StringBuilder builder = new StringBuilder(PATH);
        if (classType.equals(ClassType.LIBRARY)) {
            builder.append("lib/");
        }
        builder.append(name);
        builder.append(".java");
        return builder.toString();
    }

    private void saveFile(byte[] content, String fullPath) {
        try(OutputStream out = new FileOutputStream(fullPath)) {
            out.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPackage(ClassType type) {
        return switch (type) {
            case LIBRARY -> PACKAGE + ".lib";
            case SOURCE -> PACKAGE;
        };
    }

}
