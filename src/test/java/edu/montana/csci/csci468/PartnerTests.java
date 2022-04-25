package edu.montana.csci.csci468;

import edu.montana.csci.csci468.CatscriptTestBase;
import edu.montana.csci.csci468.tokenizer.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class PartnerTests extends CatscriptTestBase {

    /**
     * Proper Tokenization test ensures the tokens and correctly identified.
     * This test emphasized the start and end locations of a token.
     */
    @Test
    void properTokenization() {
        String  testString = "var x : int = 10";

        // basic tokenizer test
        assertTokensAre(testString, VAR, IDENTIFIER, COLON, IDENTIFIER, EQUAL, INTEGER, EOF);

        // test that the 'start' and 'end' of each token is corrent
        List<Token> tokenList = getTokensAsList(testString);

        // token: 'var' - "-->var<-- x : int = 1"
        assertEquals(0, tokenList.get(0).getLineOffset());
        assertEquals(0, tokenList.get(0).getStart());
        assertEquals(3, tokenList.get(0).getEnd());

        // token: 'x' - "var -->x<-- : int = 1"
        assertEquals(4, tokenList.get(1).getLineOffset());
        assertEquals(4, tokenList.get(1).getStart());
        assertEquals(5, tokenList.get(1).getEnd());

        // token: ':' - "var x -->:<-- int = 1"
        assertEquals(6, tokenList.get(2).getLineOffset());
        assertEquals(6, tokenList.get(2).getStart());
        assertEquals(7, tokenList.get(2).getEnd());

        // token: 'int' - "var x : -->int<-- = 1"
        assertEquals(8, tokenList.get(3).getLineOffset());
        assertEquals(8, tokenList.get(3).getStart());
        assertEquals(11, tokenList.get(3).getEnd());

        // token: '=' - "var x : int -->=<-- 1"
        assertEquals(12, tokenList.get(4).getLineOffset());
        assertEquals(12, tokenList.get(4).getStart());
        assertEquals(13, tokenList.get(4).getEnd());

        // token: '1' - "var x : int = -->1<--"
        assertEquals(14, tokenList.get(5).getLineOffset());
        assertEquals(14, tokenList.get(5).getStart());
        assertEquals(16, tokenList.get(5).getEnd());
    }


    /**
     * Variable Assignment
     * This tests basic variable assignment
     */
    @Test
    void variableAssignment() {
        String  testString = "var x = 0\n" +
                "print(x)\n" +
                "x = 1\n" +
                "print(x)";

        assertEquals("0\n1\n", executeProgram(testString));
    }

    /**
     * If-Else Statement Controls
     * This test determines whether If-Else statements work properly
     */
    @Test
    void ifElseStatementControls() {
        String  testString = "function foo(x : int) {\n" +
                "    if (x < 1) {\n" +
                "        print(\"x is less than 1\")\n" +
                "    } else if (x > 1) {\n" +
                "        print(\"x is greater than 1\")\n" +
                "    } else if (x == 1) {\n" +
                "        print(\"x is equal to 1\")\n" +
                "    }\n" +
                "}\n";

        assertEquals("x is less than 1\n", executeProgram(testString + "foo(0)"));
        assertEquals("x is greater than 1\n", executeProgram(testString + "foo(2)"));
        assertEquals("x is equal to 1\n", executeProgram(testString + "foo(1)"));
    }

}
