package com.example.transpiler.typeChecker;

import com.example.transpiler.codeGenerator.model.Assignment;
import com.example.transpiler.codeGenerator.model.VariableDeclaration;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckUnit {
    private List<Assignment> assignments;
    private List<VariableDeclaration> variableDeclarations;
}
