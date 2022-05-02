package com.example.transpiler.typeChecker;

import com.example.transpiler.syntaxer.Node;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.util.Pair;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DebtVariable {

    private String name;
    private Tree tree;
    private List<VariableExpression> expressionsWithTypes;
    private Node declarationNode;

}
