package com.example.transpiler.codeGenerator;

import com.example.transpiler.lexer.Lexer;
import com.example.transpiler.lexer.Token;
import com.example.transpiler.lexer.TokenType;
import com.example.transpiler.syntaxer.*;
import com.example.transpiler.typeChecker.TypeChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JavaCodeGenerator {

    // public contract

    public void generateJavaSourceFile(File file) {
//        generateJavaCodeForClass(file, ClassType.SOURCE);
    }

    public void generateJavaLibFile(File file) {
        generateJavaCodeForClass(file, ClassType.LIBRARY);
    }

    // inner logic

    private void generateJavaCodeForClass(File file, ClassType type) {

        try {
            String stringWithSourceCode = getStringForFile(file);
            List<Token> tokens = Lexer.getTokensFromCode(stringWithSourceCode);
            String className = tokens.stream()
                .filter(token -> token.getType().equals(TokenType.IDENTIFIER))
                .map(Token::getLexeme)
                .findFirst().orElseThrow(() -> new CompilationException("No name for class"));
            Tree tree = GrammarChecker.checkGrammar(tokens);
            ObjectMapper mapper = new ObjectMapper();
            File treeFile = new File(className +  "Tree.json");
            mapper.writeValue(treeFile, tree);
            //TypeChecker.check(tree);
            System.out.println(TreeUtil.getClassMethods(tree.getRoot().getChildNodes().get(0)));
            ClassGenerator.generateClass(tree, type);
        }
        catch (IOException e) {
            log.error("No such file with name {}", file.getPath(), e);
        }

    }

    private String getStringForFile(File file) throws IOException {
        Path path = Paths.get(file.getPath());
        return Files.readString(path);
    }

}
