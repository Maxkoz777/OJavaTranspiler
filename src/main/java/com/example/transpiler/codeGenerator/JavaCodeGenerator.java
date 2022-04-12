package com.example.transpiler.codeGenerator;

import com.example.transpiler.lexer.Lexer;
import com.example.transpiler.lexer.Token;
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
            
        }
        catch (IOException e) {
            log.error("No such file with name {}", file.getPath(), e);
        }

    }

    private String getStringForFile(File file) throws IOException {
        Path path = Paths.get(file.getPath());
        return Files.readString(path);
    }

    private enum ClassType {
        LIBRARY,
        SOURCE
    }

}
