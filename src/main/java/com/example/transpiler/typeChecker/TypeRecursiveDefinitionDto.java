package com.example.transpiler.typeChecker;

import com.example.transpiler.syntaxer.Tree;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TypeRecursiveDefinitionDto {
    private String term;
    private String expression;
    private ExpressionResult type;
    private Tree tree;
}
