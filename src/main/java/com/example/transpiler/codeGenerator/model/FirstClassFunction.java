package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FirstClassFunction {
    private String name;
    private String inputType;
    private String outputType;
    private Node declaration;
    private String variable;
    private String expression;
}
