package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression lhsExpression = parseComparisonExpression();
        if (!tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            return lhsExpression;
        }
        Token token = tokens.consumeToken();
        Expression rhsExpression = parseComparisonExpression();
        return new EqualityExpression(token, lhsExpression, rhsExpression);
    }

    private Expression parseComparisonExpression() {
        Expression lhsExpression = parseAdditiveExpression();
        if (!tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            return lhsExpression;
        }
        Token token = tokens.consumeToken();
        Expression rhsExpression = parseAdditiveExpression();
        return new ComparisonExpression(token, lhsExpression, rhsExpression);
    }

    private Expression parseAdditiveExpression() {
        Expression lhsExpression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rhsExpression = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, lhsExpression, rhsExpression);
            additiveExpression.setStart(lhsExpression.getStart());
            additiveExpression.setEnd(rhsExpression.getEnd());
            lhsExpression = additiveExpression;
        }
        return lhsExpression;
    }

    private Expression parseFactorExpression() {
        Expression lhsExpression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator_token = tokens.consumeToken();
            final Expression rhsExpression = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator_token, lhsExpression, rhsExpression);
            factorExpression.setStart(lhsExpression.getStart());
            factorExpression.setEnd(rhsExpression.getEnd());
            lhsExpression = factorExpression;
        }
        return lhsExpression;
    }

    private Expression parseUnaryExpression() {
        if (!tokens.match(MINUS, NOT)) {
            return parsePrimaryExpression();
        }
        Token token = tokens.consumeToken();
        Expression rhs = parseUnaryExpression();
        UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
        unaryExpression.setStart(token);
        unaryExpression.setEnd(rhs.getEnd());
        return unaryExpression;
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(IDENTIFIER)) {
            Token token = tokens.consumeToken();
            // function call
            if (tokens.matchAndConsume(LEFT_PAREN)) {
                return parseFunctionCallStatement(token);
            }
            String token_value = token.getStringValue();
            IdentifierExpression identifierExpression = new IdentifierExpression(token_value);
            return identifierExpression;
        } else if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token token = tokens.consumeToken();
            String token_value = token.getStringValue();
            StringLiteralExpression stringExpression = new StringLiteralExpression(token_value);
            return stringExpression;
        } else if (tokens.match(TRUE)) {
            Token token = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(true);
            return booleanLiteralExpression;
        } else if (tokens.match(FALSE)) {
            Token token = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(false);
            return booleanLiteralExpression;
        } else if (tokens.match(NULL)) {
            Token token = tokens.consumeToken();
            NullLiteralExpression nullLiteralExpression = new NullLiteralExpression();
            return nullLiteralExpression;
        } else if (tokens.matchAndConsume(LEFT_BRACKET)) { // list literal
            List<Expression> listLiteralExpressionArguments = new ArrayList<>(0);
            if(!tokens.match(RIGHT_BRACKET)) {
                do {
                    listLiteralExpressionArguments.add(parseExpression());
                } while(tokens.matchAndConsume(COMMA));
            }
            ListLiteralExpression listLiteralExpression = new ListLiteralExpression(listLiteralExpressionArguments);
            if(!tokens.matchAndConsume(RIGHT_BRACKET)) {
                require(RIGHT_BRACKET, listLiteralExpression, ErrorType.UNTERMINATED_LIST);
            }
            return listLiteralExpression;
        } else if (tokens.matchAndConsume(LEFT_PAREN)) {
            Expression expression = parseExpression();
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(expression);
            require(RIGHT_PAREN, parenthesizedExpression);
            return parenthesizedExpression;
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private FunctionCallExpression parseFunctionCallStatement(Token beginningToken) {
        List<Expression> functionCallArguments = new ArrayList<>(0);
        if(!tokens.match(RIGHT_PAREN)) {
            do {
                functionCallArguments.add(parseExpression());
            } while (tokens.matchAndConsume(COMMA));
        }
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(beginningToken.getStringValue(), functionCallArguments);
        if(!tokens.matchAndConsume(RIGHT_PAREN)) {
            require(RIGHT_BRACKET, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
        }
        return functionCallExpression;
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
