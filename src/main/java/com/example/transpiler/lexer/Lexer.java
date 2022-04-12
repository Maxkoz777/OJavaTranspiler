package com.example.transpiler.lexer;

import com.example.transpiler.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Lexer {

    private ArrayList<String> words;

    private static final char[] declarationSeparators = {';', '\n'};

    private static final List<String> operators = Arrays.asList("and", "or", "xor",
                    "not", "<", "<=", ">", ">=", "=", "/=", "*", "/", "%",
                    "+", "-", "[", "]", "(", ")", ":=", ":", ",", ".");

    private static final List<String> keywords = Arrays.asList(
            "var", "is", "type", "end",
            "array", "if", "then", "else", "size", "true", "false", "for",
            "in", "loop", /* "Integer", "Real", "Boolean", */ "return", "while",
            "class", "method", "extends", "this");

    public List<Token> getTokensFromCode(String program) throws InvalidTokenException {
        words = splitProgram(program);
        separateDeclarations(words);

        for (int order = 0; order < operators.size(); order++) {
            separateSymbolicOperators(words, order);
        }

        return convertToTokens(words);
    }

    private void separateSymbolicOperators(ArrayList<String> words, int order) {
        for (int i = 0; i < words.size(); i++) {
            var word = words.get(i);
            if (isLiteral(word)) continue;

            var position = getPositionOfSymbolicOperator(word, order);
            var operatorIndex = position.getFirst();
            var operatorLength = position.getSecond();
            if (operatorIndex == -1 || operatorLength == word.length()) continue;

            var left = word.substring(0, operatorIndex);
            var operator = word.substring(operatorIndex, operatorIndex + operatorLength);
            var right = word.substring(operatorIndex + operatorLength);

            words.set(i, left);
            words.add(i + 1, operator);
            words.add(i + 2, right);
        }

        removeBlank(words);
    }

    private static void removeBlank(ArrayList<String> words) {
        for (int index = words.size() - 1; index >= 0; index--) {
            if (words.get(index).isEmpty())
                words.remove(index);
        }
    }

    private static Pair<Integer, Integer> getPositionOfSymbolicOperator(String word, int order) {
        int index = -1;
        int length = 0;

        var operator = operators.get(order);
        if (!isSymbolic(operator)) return new Pair<>(index, length);

        var operatorIndex = word.indexOf(operator);
        if (operatorIndex == -1) return new Pair<>(index, length);

        index = operatorIndex;
        length = operator.length();

        return new Pair<>(index, length);
    }

    private static boolean isSymbolic(String string) {
        if (string.length() == 0) return false;

        for (int index = 0; index < string.length(); index++) {
            if (Character.isAlphabetic(string.charAt(index)))
                return false;
        }

        return true;
    }

    private List<Token> convertToTokens(List<String> words) throws InvalidTokenException {
        return words.stream()
            .map(Lexer::resolveToken)
            .collect(Collectors.toList());
    }

    private Token resolveToken(String word) throws InvalidTokenException {
        if (keywords.contains(word)) return new Token(TokenType.KEYWORD, word);
        if (operators.contains(word)) return new Token(TokenType.OPERATOR, word);
        if (isDeclarationSeparator(word)) return new Token(TokenType.DECLARATION_SEPARATOR, word);
        if (isIdentifier(word)) return new Token(TokenType.IDENTIFIER, word);
        if (isLiteral(word)) return new Token(TokenType.LITERAL, word);
        throw new InvalidTokenException(word);
    }

    private boolean isDeclarationSeparator(String word) {
        if (word.length() != 1) return false;

        for (char definedLexeme : declarationSeparators) {
            if (word.charAt(0) == definedLexeme)
                return true;
        }

        return false;
    }

    private boolean isIdentifier(String word){
        return Pattern.matches("^[A-Za-z_][A-Za-z_0-9]*$", word);
    }

    private boolean isLiteral(String word){
        return isIntLiteral(word) || isRealLiteral(word);
    }

    private boolean isIntLiteral(String str)  {
        try {
            int value = Integer.parseInt(str);
            return value >= 0;
        }
        catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isRealLiteral(String str) {
        try {
            var value = Float.parseFloat(str);
            return value >= 0f;
        }
        catch (NumberFormatException ignored) {
            return false;
        }
    }

    private ArrayList<String> splitProgram(String program) {
        program = program.replaceAll("\r\n", "\n");

        ArrayList<String> elements = new ArrayList<>(
            Arrays.asList(program.split("[\\t\\n\\s]"))
        );

        separateDeclarations(elements);

        return elements;
    }

    private void separateDeclarations(List<String> words) {
        for (int i = 0; i < words.size(); i++) {
            var word = words.get(i);
            if (word.length() <= 1) continue;

            var separatorIndex = getSeparatorIndex(word);
            if (separatorIndex == -1) continue;

            var left_word = word.substring(0, separatorIndex);
            var separator = word.substring(separatorIndex, separatorIndex + 1);
            var right_word = word.substring(separatorIndex + 1);

            words.set(i, left_word);
            words.add(i + 1, separator);
            words.add(i + 2, right_word);
        }
    }

    private int getSeparatorIndex(String word){
        var index = -1;

        for (char separator: declarationSeparators) {
            var separatorIndex = word.indexOf(separator);
            if (separatorIndex == -1) continue;
            if (index != -1 && separatorIndex > index) continue;

            index = separatorIndex;
        }

        return index;
    }
}
