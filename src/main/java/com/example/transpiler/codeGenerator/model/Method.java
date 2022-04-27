package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Method {

    private String name;
    private Node parameterNode;
    private String type;
    private Node body;

}
