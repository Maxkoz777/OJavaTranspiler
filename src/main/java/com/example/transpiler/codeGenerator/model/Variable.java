package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.Data;

@Data
public class Variable {

    private String name;
    private String typeName;
    private String expression;
    private JavaType type;
    private Node declarationNode;

    private void defineType() {
        switch (typeName) {
            case "Integer" -> type = JavaType.INTEGER;
            case "Real"    -> type = JavaType.REAL;
            case "Boolean" -> type = JavaType.BOOLEAN;
            default        -> type = JavaType.REFERENCE;
        }
    }

    public Variable(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
        defineType();
    }

    public Variable(String name, JavaType javaType) {
        this.name = name;
        this.type = javaType;
    }
}
