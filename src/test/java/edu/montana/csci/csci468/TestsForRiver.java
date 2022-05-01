package edu.montana.csci.csci468;

import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.expressions.FunctionCallExpression;
import edu.montana.csci.csci468.parser.statements.FunctionDefinitionStatement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestsForRiver extends CatscriptTestBase {

    //Checks to make sure that the proper types are being returned
    @Test
    void testType() {
        FunctionDefinitionStatement functionDefinitionStatement = parseStatement(
                "function foo(x : int) : int {\n" +
                        "return x + 1" +
                        "}\n" +
                        "print(foo(9))");
        assertEquals(CatscriptType.INT, functionDefinitionStatement.getType());
        assertEquals(CatscriptType.INT, functionDefinitionStatement.getParameterType(0));
    }

    //Checks that math is parsed before logic for all operators
    @Test
    void mathInLogic() {
        assertEquals("1\n", executeProgram("if((1+2)>(2)){ print(1) }"));
        assertEquals("1\n", executeProgram("if((5-2)<(6)){ print(1) }"));
        assertEquals("1\n", executeProgram("if((1*3)>=(3)){ print(1) }"));
        assertEquals("1\n", executeProgram("if((4/2)<=(2)){ print(1) }"));
    }


    //If statements can be nested without overriding each other
    @Test
    void ifStatementNesting() {
        String testString = "function foo(x : int) {\n" +
                "    if (x < 1) {\n" +
                "        print(\"x is less than 1\")\n" +
                "        if (x == 0) {" +
                "           print(\"x is zero\")\n}" +
                "    } else if (x >= 1) {\n" +
                "        print(\"x is at least 1\")\n" +
                "        if (x == 1) {\n" +
                "           print(\"x is equal to 1\")\n}" +
                "    }" +
                "}\n";
        assertEquals("x is less than 1\nx is zero\n", executeProgram(testString + "foo(0)"));
        assertEquals("x is at least 1\n", executeProgram(testString + "foo(2)"));
        assertEquals("x is at least 1\nx is equal to 1\n", executeProgram(testString + "foo(1)"));
    }
}