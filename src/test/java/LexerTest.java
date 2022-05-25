import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.transpiler.lexer.InvalidTokenException;
import com.example.transpiler.lexer.Lexer;
import com.example.transpiler.lexer.TokenType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class LexerTest {

    private String getProgram(int testNumber) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        Path path = Path.of("test_data_lexer", "test" + testNumber + ".txt");
        InputStream inputStream = classLoader.getResourceAsStream(String.valueOf(path));
        assert inputStream != null;
        return new String(inputStream.readAllBytes());
    }

    @Test
    void lexerTest01() throws InvalidTokenException, IOException {
        String program = getProgram(1);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals(TokenType.KEYWORD, tokens.get(0).getType());
        assertEquals("var", tokens.get(0).getLexeme());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("a", tokens.get(1).getLexeme());
        assertEquals(TokenType.OPERATOR, tokens.get(2).getType());
        assertEquals(":", tokens.get(2).getLexeme());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType());
        assertEquals("Array", tokens.get(3).getLexeme());
        assertEquals(TokenType.OPERATOR, tokens.get(4).getType());
        assertEquals("[", tokens.get(4).getLexeme());
    }

    @Test
    void lexerTest02() throws InvalidTokenException, IOException {
        String program = getProgram(2);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals(TokenType.KEYWORD, tokens.get(0).getType());
        assertEquals("var", tokens.get(0).getLexeme());
        assertEquals("i", tokens.get(1).getLexeme());
        assertEquals("is", tokens.get(2).getLexeme());
        assertEquals("1", tokens.get(3).getLexeme());
        assertEquals("while", tokens.get(4).getLexeme());
        assertEquals("loop", tokens.get(10).getLexeme());
    }

    @Test
    void lexerTest03() throws InvalidTokenException, IOException {
        String program = getProgram(3);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals(TokenType.KEYWORD, tokens.get(0).getType());
        assertEquals("i", tokens.get(1).getLexeme());
        assertEquals("while", tokens.get(8).getLexeme());
    }

    @Test
    void lexerTest04() throws InvalidTokenException, IOException {
        String program = getProgram(4);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
    }

    @Test
    void lexerTest05() throws InvalidTokenException, IOException {
        String program = getProgram(5);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals("T", tokens.get(18).getLexeme());

    }

    @Test
    void lexerTest06() throws InvalidTokenException, IOException {
        String program = getProgram(6);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals(TokenType.IDENTIFIER, tokens.get(5).getType());

    }

    @Test
    void lexerTest07() throws InvalidTokenException, IOException {
        String program = getProgram(7);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals(TokenType.IDENTIFIER, tokens.get(8).getType());

    }
    @Test
    void lexerTest08() throws InvalidTokenException, IOException {
        String program = getProgram(8);
        var tokens = Lexer.getTokensFromCode(program);
        String stringWithTokens = tokens.toString();
        log.info("Array with tokens: {}", stringWithTokens);
        assertEquals(TokenType.KEYWORD, tokens.get(5).getType());

    }
}
