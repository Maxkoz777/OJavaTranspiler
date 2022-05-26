import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.transpiler.lexer.Lexer;
import com.example.transpiler.syntaxer.GrammarChecker;
import com.example.transpiler.syntaxer.Tree;
import com.example.transpiler.typeChecker.TypeChecker;
import com.example.transpiler.typeChecker.TypeCheckerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class TypeCheckerTest {

    private String getProgram(int testNumber) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("test_data_type_checker/test" + testNumber + ".txt");
        assert inputStream != null;
        return new String(inputStream.readAllBytes());
    }

    @BeforeEach
    void before() {
        ObjectMapper mapper = new ObjectMapper();
        TypeChecker.knownTypes.addAll(List.of("Boolean", "Integer", "MathUtils", "Real"));
        TypeChecker.knownTypes.forEach(type -> {
            try {
                URL url = getClass().getResource("lib_trees/" + type + "Tree.json");
                File file = new File(url.getPath());
                Tree tree = mapper.readValue(file, Tree.class);
                tree.setClassName(type);
                TypeChecker.trees.add(tree);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        TypeChecker.knownTypes.add("Child");
        TypeChecker.isTreeArrayReady = true;
    }

    @AfterEach
    void after() {
        TypeChecker.knownTypes.clear();
        TypeChecker.trees.clear();
    }

    @Test
    void typeCheckerTest1() throws IOException {

        String program = getProgram(2);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());
        TypeChecker.check(tree);

    }

    @Test
    void typeCheckerTest2() throws IOException {

        String program = getProgram(3);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());
        TypeChecker.check(tree);

    }

    @Test
    void typeCheckerTest3() throws IOException {

        String program = getProgram(4);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());
        TypeChecker.check(tree);

    }

    @Test
    void typeCheckerTest4() throws IOException {

        String program = getProgram(5);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());
        TypeChecker.check(tree);

    }

    @Test
    void typeCheckerTest6() throws IOException {

        String program = getProgram(7);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());
        TypeChecker.check(tree);

    }

    @Test
    void typeCheckerTest5() throws IOException {

        String program = getProgram(6);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());

        Exception exception = assertThrows(
            TypeCheckerException.class,
            () -> TypeChecker.check(tree)
        );
        assertEquals("No definition for variable sdfwlijfnvsdl", exception.getMessage());
    }

    @Test
    void typeCheckerTest7() throws IOException {

        String program = getProgram(8);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());

        Exception exception = assertThrows(
            TypeCheckerException.class,
            () -> TypeChecker.check(tree)
        );
        assertEquals("Multiple types for variable fff provided", exception.getMessage());
    }

    @Test
    void typeCheckerTest8() throws IOException {

        String program = getProgram(9);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        TypeChecker.knownTypes.add(tree.getClassName());

        Exception exception = assertThrows(
            TypeCheckerException.class,
            () -> TypeChecker.check(tree)
        );
        assertEquals("Trying to apply + to entities of non-comparable type: Boolean", exception.getMessage());
    }

}
