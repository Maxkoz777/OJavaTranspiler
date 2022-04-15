package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.Data;

@Data
public class Variable {

    private String name;
    private String typeName;
    private Node expression;
    private JvmType type;
    private String typeChar;

    private void defineType() {
        switch (typeName) {
            case "Integer":
                type = JvmType.INTEGER;
                typeChar = "I";
                break;
            case "Real":
                type = JvmType.REAL;
                typeChar = "D";
                break;
            case "Boolean":
                type = JvmType.BOOLEAN;
                typeChar = "B";
                break;
            default:
                type = JvmType.REFERENCE;
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
