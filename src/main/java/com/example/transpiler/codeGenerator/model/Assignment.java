package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Assignment {

    private String varName;
    private String expression;
    private Node node;

    public Assignment(String varName, String expression) {
        this.varName = varName;
        this.expression = expression;
    }

}
