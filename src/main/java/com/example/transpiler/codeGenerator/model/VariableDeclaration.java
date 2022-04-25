package com.example.transpiler.codeGenerator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VariableDeclaration {

    private String name;
    private JavaType type;
    private String expression;

}
