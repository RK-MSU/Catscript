package edu.montana.csci.csci468.tokenizer;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptTokenizer {

    TokenList tokenList;
    String src;
    int postion = 0;
    int line = 1;
    int lineOffset = 0;

    public CatScriptTokenizer(String source) {
        src = source;
        tokenList = new TokenList(this);
        tokenize();
    }

    private void tokenize() {
        consumeWhitespace();
        while (!tokenizationEnd()) {
            scanToken();
            consumeWhitespace();
        }
        tokenList.addToken(EOF, "<EOF>", postion, postion, line, lineOffset);
    }

    private void scanToken() {
        if(scanNumber()) {
            return;
        }
        if(scanString()) {
            return;
        }
        if(scanIdentifier()) {
            return;
        }
        scanSyntax();
    }

    private boolean scanString() {
        // TODO: string may also deliminate with a single quote (')
        if (peek() != '\"') { // let's assume that strings begin with (")
            return false; // not a string to scan
        }

        // capture the string delimiter
        char str_delimiter = takeChar();

        // keep track of where the string starts
        int start = postion;

        // time to capture the value of the string
        while(!tokenizationEnd() && peek() != '\"') {
            matchAndConsume('\\');
            if(tokenizationEnd()) break;
            takeChar();
        }

        // let's get the value of the string we just iterated through
        String value = src.substring(start, postion);

        if(!tokenizationEnd()) {
            takeChar();
            tokenList.addToken(STRING, value, start, postion, line, lineOffset);
        } else {
            tokenList.addToken(ERROR, value, start, postion, line, lineOffset);
        }

        return true;
    }

    private boolean scanIdentifier() {
        if( isAlpha(peek())) {
            int start = postion;
            while (isAlphaNumeric(peek())) {
                takeChar();
            }
            String value = src.substring(start, postion);
            if (KEYWORDS.containsKey(value)) {
                tokenList.addToken(KEYWORDS.get(value), value, start, postion, line, lineOffset);
            } else {
                tokenList.addToken(IDENTIFIER, value, start, postion, line, lineOffset);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean scanNumber() {
        if(isDigit(peek())) {
            int start = postion;
            while (isDigit(peek())) {
                takeChar();
            }
            tokenList.addToken(INTEGER, src.substring(start, postion), start, postion, line, lineOffset);
            return true;
        } else {
            return false;
        }
    }

    private void scanSyntax() {
        int start = postion;
        // parenthesis - "(" or ")"
        if(matchAndConsume('(')) tokenList.addToken(LEFT_PAREN, "(", start, postion, line, lineOffset);
        else if(matchAndConsume(')')) tokenList.addToken(RIGHT_PAREN, ")", start, postion, line, lineOffset);
        // brace - "{" or "}"
        else if(matchAndConsume('{')) tokenList.addToken(LEFT_BRACE, "{", start, postion, line, lineOffset);
        else if(matchAndConsume('}')) tokenList.addToken(RIGHT_BRACE, "}", start, postion, line, lineOffset);
        // bracket - "[" or "]"
        else if(matchAndConsume('[')) tokenList.addToken(LEFT_BRACKET, "[", start, postion, line, lineOffset);
        else if(matchAndConsume(']')) tokenList.addToken(RIGHT_BRACKET, "]", start, postion, line, lineOffset);
        // colon
        else if(matchAndConsume(':')) tokenList.addToken(COLON, ":", start, postion, line, lineOffset);
        // comma
        else if(matchAndConsume(',')) tokenList.addToken(COMMA, ",", start, postion, line, lineOffset);
        // period (dot)
        else if(matchAndConsume('.')) tokenList.addToken(DOT, ".", start, postion, line, lineOffset);
        // minus
        else if(matchAndConsume('-')) tokenList.addToken(MINUS, "-", start, postion, line, lineOffset);
        // plus
        else if(matchAndConsume('+')) tokenList.addToken(PLUS, "+", start, postion, line, lineOffset);
        // forward slash
        else if(matchAndConsume('/')) {
            // are we looking at an inline-comment? (i.e. "// some comment")
            if (matchAndConsume('/')) {
                // this is a comment
                while (peek() != '\n' && !tokenizationEnd()) {
                    takeChar();
                }
            } else {
                tokenList.addToken(SLASH, "/", start, postion, line, lineOffset);
            }
        }
        // asterisk (STAR)
        else if(matchAndConsume('*')) tokenList.addToken(STAR, "*", start, postion, line, lineOffset);
        // !=
        else if (matchAndConsume('!')) {
            if (matchAndConsume('=')) {
                tokenList.addToken(BANG_EQUAL, "!=", start, postion, line, lineOffset);
            }
        }
        // equal
        else if(matchAndConsume('=')) {
            // equal_equal
            if (matchAndConsume('=')) {
                tokenList.addToken(EQUAL_EQUAL, "==", start, postion, line, lineOffset);
            } else { // just one equal (=) sign
                tokenList.addToken(EQUAL, "=", start, postion, line, lineOffset);
            }
        }
        // greater(than)
        else if(matchAndConsume('>')) {
            // greater_equal?
            if (matchAndConsume('=')) {
                tokenList.addToken(GREATER_EQUAL, ">=", start, postion, line, lineOffset);
            } else { // just a greater(than) sign
                tokenList.addToken(GREATER, ">", start, postion, line, lineOffset);
            }
        }
        // less(than)
        else if(matchAndConsume('<')) {
            // less_equal?
            if (matchAndConsume('=')) {
                tokenList.addToken(LESS_EQUAL, "<=", start, postion, line, lineOffset);
            } else { // just a less(than) sign
                tokenList.addToken(LESS, "<", start, postion, line, lineOffset);
            }
        }
        // Unexpected Token
        else tokenList.addToken(ERROR, "<Unexpected Token: [" + takeChar() + "]>", start, postion, line, lineOffset);
    }

    private void consumeWhitespace() {
        while (!tokenizationEnd()) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t') {
                postion++;
                lineOffset++;
                continue;
            } else if (c == '\n') {
                postion++;
                line++;
                lineOffset = 0;
                continue;
            }
            break;
        }
    }

    //===============================================================
    // Utility functions
    //===============================================================

    private char peek() {
        if (tokenizationEnd()) return '\0';
        return src.charAt(postion);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char takeChar() {
        char c = src.charAt(postion);
        postion++;
        lineOffset++;
        return c;
    }

    private boolean tokenizationEnd() {
        return postion >= src.length();
    }

    public boolean matchAndConsume(char c) {
        if (peek() == c) {
            takeChar();
            return true;
        }
        return false;
    }

    public TokenList getTokens() {
        return tokenList;
    }

    @Override
    public String toString() {
        if (tokenizationEnd()) {
            return src + "-->[]<--";
        } else {
            return src.substring(0, postion) + "-->[" + peek() + "]<--" +
                    ((postion == src.length() - 1) ? "" :
                            src.substring(postion + 1, src.length() - 1));
        }
    }
}