import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.transpiler.lexer.InvalidTokenException;
import com.example.transpiler.lexer.Lexer;
import com.example.transpiler.syntaxer.GrammarChecker;
import com.example.transpiler.syntaxer.Tree;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class SyntaxerTest {


    private String getProgram(int testNumber) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        Path path = Path.of("test_data", "test" + testNumber + ".txt");
        InputStream inputStream = classLoader.getResourceAsStream(String.valueOf(path));
        assert inputStream != null;
        return new String(inputStream.readAllBytes());
    }

    @Test
    void syntaxerTest01() throws InvalidTokenException, IOException {
        String program = getProgram(2);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        try {

            String stringWithTokens = tokens.toString();
            log.info("Array with tokens: {}", stringWithTokens);

            tree = GrammarChecker.checkGrammar(tokens);

            assertEquals("PROGRAM", tree.getRoot().getType().toString());
            assertEquals("CLASS_DECLARATION", tree.getRoot().getChildNodes().get(0).getType().toString());
            assertEquals("ClassName", tree.getRoot().getChildNodes().get(0).getChildNodes().get(0).getChildNodes().get(0).getValue().toString());
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }

    @Test
    void syntaxerTest02() throws InvalidTokenException, IOException {
        String program = getProgram(3);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        try {

            String stringWithTokens = tokens.toString();
            log.info("Array with tokens: {}", stringWithTokens);

            tree = GrammarChecker.checkGrammar(tokens);

            log.info(tree.toString());

            log.info(tree.getRoot().getChildNodes().get(0).getChildNodes().get(0).getChildNodes().get(0).getValue().toString());
            assertEquals("RealValues", tree.getRoot().getChildNodes().get(0).getChildNodes().get(0).getChildNodes().get(0).getValue().toString());
            assertEquals("IDENTIFIER", tree.getRoot().getChildNodes().get(0).getChildNodes().get(1).getChildNodes().get(0).getType().toString());
            assertEquals("AnyValue", tree.getRoot().getChildNodes().get(0).getChildNodes().get(1).getChildNodes().get(0).getValue().toString());
            assertEquals("CONSTRUCTOR_DECLARATION", tree
                    .getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(2)
                    .getChildNodes()
                    .get(0)
                    .getType()
                    .toString());
            assertEquals("PARAMETER_DECLARATION", tree
                    .getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(2)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getType()
                    .toString());

        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }

    @Test
    void syntaxerTest03() throws InvalidTokenException, IOException {
        String program = getProgram(4);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        try {

            String stringWithTokens = tokens.toString();
            log.info("Array with tokens: {}", stringWithTokens);

            tree = GrammarChecker.checkGrammar(tokens);
            assertEquals("C", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getValue()
                    .toString());
            assertEquals("VARIABLE_DECLARATION", tree
                    .getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getType()
                    .toString());

            assertEquals("T", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getValue()
                    .toString());

            log.info(tree.toString());


        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }

    @Test
    void syntaxerTest04() throws InvalidTokenException, IOException {
        String program = getProgram(5);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        try {

            String stringWithTokens = tokens.toString();
            log.info("Array with tokens: {}", stringWithTokens);

            tree = GrammarChecker.checkGrammar(tokens);
            assertEquals("B", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getValue()
                    .toString());

            assertEquals("IDENTIFIER", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getType()
                    .toString());

            assertEquals("EXPRESSION", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getType()
                    .toString());
            assertEquals("Integer", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getValue()
                    .toString());
            log.info(tree.toString());


        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }

    @Test
    void syntaxerTest05() throws InvalidTokenException, IOException {
        String program = getProgram(5);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        try {

            String stringWithTokens = tokens.toString();
            log.info("Array with tokens: {}", stringWithTokens);

            tree = GrammarChecker.checkGrammar(tokens);


            assertEquals("PRIMARY", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getType()
                    .toString());

            assertEquals("IDENTIFIER", tree.getRoot()
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(1)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getChildNodes()
                    .get(0)
                    .getType()
                    .toString());
            log.info(tree.toString());


        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }
}
