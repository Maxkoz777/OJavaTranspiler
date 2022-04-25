package com.example.transpiler.typeChecker;

import com.example.transpiler.syntaxer.CompilationException;

public class TypeCheckerException extends CompilationException {

    public TypeCheckerException(String message) {
        super(message);
    }

}
