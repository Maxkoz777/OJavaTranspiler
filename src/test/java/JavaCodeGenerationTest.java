import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.transpiler.codeGenerator.ClassGenerator;
import com.example.transpiler.codeGenerator.ClassType;
import com.example.transpiler.lexer.Lexer;
import com.example.transpiler.syntaxer.GrammarChecker;
import com.example.transpiler.syntaxer.Tree;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class JavaCodeGenerationTest {

    private String getProgram(int testNumber) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("code_gen_tests/data/test" + testNumber + ".txt");
        assert inputStream != null;
        return new String(inputStream.readAllBytes());
    }

    @Test
    void test1() throws IOException {
        int index= 2;
        String program = getProgram(index);
        var tokens = Lexer.getTokensFromCode(program);
        Tree tree;
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        tree = GrammarChecker.checkGrammar(tokens);
        String result = ClassGenerator.generateClassWithoutFileCreation(tree, ClassType.SOURCE);
        String expected = new String(
            getClass()
                .getResourceAsStream("code_gen_tests/expected/test" + index + ".txt")
                .readAllBytes()
        );
        assertEquals(expected, result);
    }

}
