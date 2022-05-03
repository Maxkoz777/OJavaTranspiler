package com.example.transpiler.typeChecker;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VariableExpression {
    private String term;
    private ExpressionResult type;
    private String wholeExpression;
}
