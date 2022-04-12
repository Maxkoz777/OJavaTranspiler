package com.example.transpiler.lexer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Token {
    private final TokenType type;
    private final String lexeme;
}