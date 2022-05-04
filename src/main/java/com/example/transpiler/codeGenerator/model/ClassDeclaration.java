package com.example.transpiler.codeGenerator.model;

import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.TreeUtil;
import lombok.Data;

@Data
public class ClassDeclaration {

    private String name;
    private Node node;

    public ClassDeclaration(String name, Node node) {
        this.name = name;
        this.node = node;
    }

    public ClassDeclaration(Node node) {
        this.node = node;
        this.name = TreeUtil.getClassNameForClassDeclarationNode(node);
    }

}
