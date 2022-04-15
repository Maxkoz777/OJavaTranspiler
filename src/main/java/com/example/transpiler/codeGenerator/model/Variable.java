package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.Data;

@Data
public class Variable {

    private String name;
    private String typeName;
    private Node expression;
    private JavaType type;
    private String typeChar;

    private void defineType() {
        switch (typeName) {
            case "Integer":
                type = JavaType.INTEGER;
                typeChar = "I";
                break;
            case "Real":
                type = JavaType.REAL;
                typeChar = "D";
                break;
            case "Boolean":
                type = JavaType.BOOLEAN;
                typeChar = "B";
                break;
            default:
                type = JavaType.REFERENCE;
                typeChar = "A";
        }
    }

    public Variable(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
        defineType();
    }

    public Variable(String name, Node expression) {
        this.name = name;
        this.expression = expression;
    }
}
