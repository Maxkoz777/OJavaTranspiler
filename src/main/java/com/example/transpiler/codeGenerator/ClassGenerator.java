package com.example.transpiler.codeGenerator;


import com.example.transpiler.codeGenerator.model.FirstClassFunction;
import com.example.transpiler.syntaxer.CompilationException;
import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.syntaxer.TreeUtil;
import com.example.transpiler.util.Pair;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
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

    /**
     *
     * @param tree
     * @param classType
     * @return String for java-class
     */
    public String generateClassWithoutFileCreation(Tree tree, ClassType classType) {
        CompilationUnit cu = getCompilationUnit(tree, classType);
        return cu.toString();
    }

    /**
     *
     * @param tree
     * @param classType
     * @return CompilationUnit that can be transformed into real java-class
     */
    private CompilationUnit getCompilationUnit(Tree tree, ClassType classType) {
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration(getPackage(classType));

        Node classNode = TreeUtil.getMainClassNode(tree);

        Pair<String, String> signature = TreeUtil.getClassSignature(classNode);

        ClassOrInterfaceDeclaration mainClass = cu.addClass(signature.getFirst());
        if (!Objects.isNull(signature.getSecond()) && !signature.getSecond().isEmpty()) {
            mainClass.addExtends(signature.getSecond());
        }

        TreeUtil.getClassVariables(classNode)
            .forEach(variable -> mainClass.addField(variable.getTypeName(), variable.getName()));

        TreeUtil.getFunctions(classNode)
            .forEach(function -> addFirstClassFunctionToClass(function, cu, signature.getFirst()));

        TreeUtil.getConstructors(classNode)
            .forEach(constructor -> ConstructorGenerator.generateConstructor(cu, constructor));

        TreeUtil.getClassMethods(classNode)
            .forEach(method -> MethodGenerator.generateMethod(cu, method, signature.getFirst()));

        TreeUtil.getNestedClasses(classNode)
            .forEach(nestedClass -> generateNestedClass(cu, nestedClass, mainClass));

        return cu;
    }

    /**
     * Generates java-class from CompilationUnit
     * @param tree
     * @param classType
     */
    public void generateClass(Tree tree, ClassType classType) {
        CompilationUnit cu = getCompilationUnit(tree, classType);
        generateCode(cu, classType, tree.getClassName());
    }

    /**
     * Self-descriptive
     * @param function
     * @param cu - CompilationUnit
     * @param className
     */
    private void addFirstClassFunctionToClass(FirstClassFunction function,
                                                     CompilationUnit cu,
                                                     String className) {
        ClassOrInterfaceDeclaration clazz = cu.getClassByName(className)
            .orElseThrow(() -> new CompilationException("class name wasn't specified for provided method"));
        String type = getTypeForFunction(function);
        Expression lambda = new NameExpr(function.getVariable() + " -> " + function.getExpression());
        Keyword[] keywords = new Keyword[0];
        clazz.addFieldWithInitializer(type, function.getName(), lambda, keywords);
    }

    /**
     *
     * @param function
     * @return Java signature for first-class functions from O-language
     */
    private String getTypeForFunction(FirstClassFunction function) {
        return "Function<" + function.getInputType() + ", " + function.getOutputType() + ">";
    }

    /**
     * Generate nested classes for CompilationUnit
     * @param cu
     * @param nestedClass
     * @param mainClass
     */
    private void generateNestedClass(CompilationUnit cu, Node nestedClass, ClassOrInterfaceDeclaration mainClass) {
        String className = TreeUtil.getClassSignature(nestedClass).getFirst();
        ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration(
            new NodeList<>(),
            false,
            className
        );

        TreeUtil.getClassVariables(nestedClass)
            .forEach(variable -> clazz.addField(variable.getTypeName(), variable.getName()));

        TreeUtil.getConstructors(nestedClass)
            .forEach(constructor -> ConstructorGenerator.generateConstructor(clazz, constructor));

        TreeUtil.getClassMethods(nestedClass)
            .forEach(method -> MethodGenerator.generateMethod(clazz, method, className));

        TreeUtil.getNestedClasses(nestedClass)
            .forEach(nested -> generateNestedClass(cu, nested, clazz));


        mainClass.addMember(clazz);
    }

    /**
     * Self-descriptive
     * @param cu
     * @param type
     * @param name
     */
    private void generateCode(CompilationUnit cu, ClassType type, String name) {
        saveFile(
            cu.toString().getBytes(StandardCharsets.UTF_8),
            getFilePath(name, type)
        );
    }

    /**
     *
     * @param name
     * @param classType
     * @return path to generated file
     */
    private String getFilePath(String name, ClassType classType) {
        StringBuilder builder = new StringBuilder(PATH);
        if (classType.equals(ClassType.LIBRARY)) {
            builder.append("lib/");
        }
        builder.append(name);
        builder.append(".java");
        return builder.toString();
    }

    /**
     * Self-descriptive
     * @param content
     * @param fullPath
     */
    private void saveFile(byte[] content, String fullPath) {
        try(OutputStream out = new FileOutputStream(fullPath)) {
            out.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param type
     * @return package name based on initial purpose of file(lib or source)
     */
    private String getPackage(ClassType type) {
        return switch (type) {
            case LIBRARY -> PACKAGE + ".lib";
            case SOURCE -> PACKAGE;
        };
    }

}
