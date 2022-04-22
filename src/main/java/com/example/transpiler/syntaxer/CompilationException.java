package com.example.transpiler.syntaxer;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CompilationException extends RuntimeException{

    public CompilationException(String message) {
        super(message);
    }

}
