package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Method {

    private String name;
    private List<Parameter> parameters;
    private String type;
    private Node body;

}
