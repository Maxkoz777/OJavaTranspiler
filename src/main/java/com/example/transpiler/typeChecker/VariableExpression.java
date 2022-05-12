package com.example.transpiler.typeChecker;

import com.example.transpiler.syntaxer.Node;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VariableExpression {
    private String term;
    private ExpressionResult type;
    private String wholeExpression;
//    private Node assignmentNode;
}
