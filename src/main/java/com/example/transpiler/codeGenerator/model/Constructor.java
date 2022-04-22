package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import java.util.List;
import lombok.Data;

@Data
public class Constructor {

    private List<Variable> parameters;
    private List<Assignment> assignments;
    private String className = "";
    private Node body;


    public Constructor(List<Variable> parameters, List<Assignment> assignments, Node body, String className) {
        this.parameters = parameters;
        this.assignments = assignments;
        this.body = body;
        this.className = className;
    }

}
