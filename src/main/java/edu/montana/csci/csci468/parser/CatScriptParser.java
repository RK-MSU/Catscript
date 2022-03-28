package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import javax.swing.plaf.nimbus.State;
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

        // for
        if (tokens.match(FOR)) {
            return parseForStatement();
        }

        // if
        if (tokens.match(IF)) {
            return parseIfStatement();
        }

        // var
        if (tokens.match(VAR)) {
            return parseVarStatement();
        }

        // function
        if (tokens.match(FUNCTION)) {
            return parseFunctionDefinitionStatement();
        }

        // return statement
        if (tokens.match(RETURN)) {
            return parseReturnStatement();
        }

        // identifier
        if (tokens.match(IDENTIFIER)) {
            Token token = tokens.consumeToken();
            if (tokens.matchAndConsume(EQUAL)) {
                return parseAssignmentStatement(token);
            }
            // function call statement
            else if (tokens.matchAndConsume(LEFT_PAREN)) {
                FunctionCallExpression call_expression = parseFunctionCallStatement(token);
                FunctionCallStatement stmt = new  FunctionCallStatement(call_expression);
                return stmt;
            }
        }

        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    // This is a helper method to help reduce code redundency
    // This method is NOT outlined in the grammar
    private List<Statement> parseProgramBodyStatements() {
        List<Statement> bodyStatements = new ArrayList<>();
        while(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
            bodyStatements.add(parseProgramStatement());
        }
        return bodyStatements;
    }

    private TypeLiteral parseTypeLiteral() {
        TypeLiteral typeLiteral = new TypeLiteral();
        String type_lit_str = tokens.consumeToken().getStringValue();
        if (type_lit_str.equals("int")) {
            typeLiteral.setType(CatscriptType.INT);
        } else if (type_lit_str.equals("string")) {
            typeLiteral.setType(CatscriptType.STRING);
        } else if (type_lit_str.equals("bool")) {
            typeLiteral.setType(CatscriptType.BOOLEAN);
        } else if (type_lit_str.equals("object")) {
            typeLiteral.setType(CatscriptType.OBJECT);
        } else if (type_lit_str.equals("list")) {
            if (tokens.matchAndConsume(LESS)) {
                typeLiteral.setType(CatscriptType.getListType(parseTypeLiteral().getType()));
                tokens.matchAndConsume(GREATER);
            } else {
                typeLiteral.setType(CatscriptType.getListType(CatscriptType.OBJECT));
            }
        }

        return typeLiteral;
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

    private Statement parseForStatement() {
        if (!tokens.match(FOR)) {
            return null;
        }

        ForStatement forStmt = new ForStatement();
        forStmt.setStart(tokens.consumeToken());

        require(LEFT_PAREN, forStmt);

        Token forStmtVariableToken = require(IDENTIFIER, forStmt);
        forStmt.setVariableName(forStmtVariableToken.getStringValue());

        require(IN, forStmt);

        forStmt.setExpression(parseExpression());

        require(RIGHT_PAREN, forStmt);
        require(LEFT_BRACE, forStmt);

        List<Statement> bodyStatements = parseProgramBodyStatements();
        forStmt.setBody(bodyStatements);

        Token forStmtEndBraceToken =  require(RIGHT_BRACE, forStmt);
        if (forStmtEndBraceToken.getType().equals(RIGHT_BRACE)) {
            forStmt.setEnd(forStmtEndBraceToken);
        }

        return forStmt;
    }

    private Statement parseIfStatement() {
        if (!tokens.match(IF)) {
            return null;
        }

        IfStatement ifStmt = new IfStatement();
        ifStmt.setStart(tokens.consumeToken());

        require(LEFT_PAREN, ifStmt);
        ifStmt.setExpression(parseExpression());
        require(RIGHT_PAREN, ifStmt);

        require(LEFT_BRACE, ifStmt);

        List<Statement> bodyStatements = parseProgramBodyStatements();
        ifStmt.setTrueStatements(bodyStatements);

        Token ifStmtEndBraceToken = require(RIGHT_BRACE, ifStmt);

        if (tokens.matchAndConsume(ELSE)) {
            List<Statement> elseStatements = new ArrayList<>();
            if (tokens.match(IF)) {
                Statement elseIfStmt = parseIfStatement();
                ifStmtEndBraceToken = elseIfStmt.getEnd();
                elseStatements.add(elseIfStmt);
            } else {
                require(LEFT_BRACE, ifStmt);
                elseStatements = parseProgramBodyStatements();
                ifStmtEndBraceToken = require(RIGHT_BRACE, ifStmt);
            }

            ifStmt.setElseStatements(elseStatements);
        }

        if (ifStmtEndBraceToken.getType().equals(RIGHT_BRACE)) {
            ifStmt.setEnd(ifStmtEndBraceToken);
        }

        return ifStmt;
    }

    private Statement parseVarStatement() {
        if (!tokens.match(VAR)) {
            return null;
        }

        VariableStatement varStmt = new VariableStatement();
        varStmt.setStart(tokens.consumeToken());
        varStmt.setVariableName(require(IDENTIFIER, varStmt).getStringValue());
        // check for type literal
        if (tokens.matchAndConsume(COLON)) {
            // parse (get) type literal
            TypeLiteral typeLiteral = parseTypeLiteral();
            // set statement type literal
            varStmt.setExplicitType(typeLiteral.getType());
        }
        require(EQUAL, varStmt);
        Expression varExpression = parseExpression();
        varStmt.setEnd(varExpression.getEnd());
        varStmt.setExpression(varExpression);

        return varStmt;
    }

    private Statement parseFunctionDefinitionStatement() {
        if (!tokens.match(FUNCTION)) {
            return null;
        }
        FunctionDefinitionStatement fncDefStmt = new FunctionDefinitionStatement();
        fncDefStmt.setStart(tokens.consumeToken());

        fncDefStmt.setName(require(IDENTIFIER, fncDefStmt).getStringValue());

        require(LEFT_PAREN, fncDefStmt);
        while(tokens.hasMoreTokens() && !tokens.match(RIGHT_PAREN)) {
            Token fncParam = tokens.consumeToken();
            String fncParamString = fncParam.getStringValue();
            TypeLiteral fncParamType = null;
            if (tokens.matchAndConsume(COLON)) {
                fncParamType = parseTypeLiteral();
            }
            fncDefStmt.addParameter(fncParamString, fncParamType);
            tokens.matchAndConsume(COMMA);
        }
        require(RIGHT_PAREN, fncDefStmt);

        if (tokens.matchAndConsume(COLON)) {
            fncDefStmt.setType(parseTypeLiteral());
        } else {
            TypeLiteral type = new TypeLiteral();
            type.setType(CatscriptType.VOID);
            fncDefStmt.setType(type);
        }

        require(LEFT_BRACE, fncDefStmt);
        List<Statement> bodyStatements = parseProgramBodyStatements();
        fncDefStmt.setBody(bodyStatements);
        fncDefStmt.setEnd(require(RIGHT_BRACE, fncDefStmt));

        return fncDefStmt;
    }

    private Statement parseAssignmentStatement(Token name) {
        AssignmentStatement assignmentStmt = new AssignmentStatement();
        assignmentStmt.setStart(name);
        assignmentStmt.setVariableName(name.getStringValue());
        Expression expression = parseExpression();
        assignmentStmt.setExpression(expression);
        assignmentStmt.setEnd(expression.getEnd());
        return assignmentStmt;
    }

    private Statement parseReturnStatement() {
        ReturnStatement returnStmt = new ReturnStatement();
        returnStmt.setStart(require(RETURN, returnStmt));
        FunctionDefinitionStatement fncStmt = new FunctionDefinitionStatement();
        fncStmt.setType(null); // TODO: check this guy
        returnStmt.setFunctionDefinition(fncStmt);
        if (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
            returnStmt.setExpression(parseExpression());
            returnStmt.setEnd(returnStmt.getExpression().getEnd());
        } else {
            returnStmt.setEnd(returnStmt.getStart());
        }

        return returnStmt;
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
            identifierExpression.setToken(token);
            return identifierExpression;
        } else if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token token = tokens.consumeToken();
            String token_value = token.getStringValue();
            StringLiteralExpression stringExpression = new StringLiteralExpression(token_value);
            return stringExpression;
        } else if (tokens.match(TRUE)) {
            Token token = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(true);
            booleanLiteralExpression.setToken(token);
            return booleanLiteralExpression;
        } else if (tokens.match(FALSE)) {
            Token token = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(false);
            booleanLiteralExpression.setToken(token);
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
