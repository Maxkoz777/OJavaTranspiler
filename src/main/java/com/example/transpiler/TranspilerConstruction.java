package com.example.transpiler;

import com.example.transpiler.codeGenerator.JavaCodeGenerator;
import com.example.transpiler.util.TranspilerUtil;

public class TranspilerConstruction {

    public static void main(String[] args) {

        // main program
        TranspilerUtil.retrieveSourceLanguageLibraryFiles()
                .forEach(JavaCodeGenerator::generateJavaLibFile);

        TranspilerUtil.retrieveSourceLanguageFiles()
            .forEach(JavaCodeGenerator::generateJavaSourceFile);

    }

}
