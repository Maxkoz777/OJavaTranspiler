package com.example.transpiler.codeGenerator;

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JavaCodeGenerator {

    // public contract

    public void generateJavaFile(File file) {
        generateJavaCodeForClass(file, ClassType.SOURCE);
    }

    public void generateJavaLibFile(File file) {
        generateJavaCodeForClass(file, ClassType.LIBRARY);
    }

    // inner logic

    private void generateJavaCodeForClass(File file, ClassType type) {

        try {
            String stringWithSourceCode = getStringForFile(file);
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
