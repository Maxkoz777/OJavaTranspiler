package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VariableDeclaration {

    private String name;
    private String typeName;
    private JavaType type;
    private String expression;
    private Node node;

    public VariableDeclaration(String name, JavaType type, String expression, Node node) {
        this.name = name;
        this.type = type;
        this.expression = expression;
        this.node = node;
    }

}
