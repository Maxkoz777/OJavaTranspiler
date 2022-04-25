package com.example.transpiler.typeChecker;

import com.example.transpiler.util.Pair;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DebtVariable {

    private String name;
    private List<Pair<String, ExpressionResult>> expressionsWithTypes;

}
