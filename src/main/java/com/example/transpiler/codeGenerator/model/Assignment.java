package com.example.transpiler.codeGenerator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Assignment {

    private String varName;
    private String expression;

}
