package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class AdditiveExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public AdditiveExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }
    public Expression getRightHandSide() {
        return rightHandSide;
    }
    public boolean isAdd() {
        return operator.getType() == TokenType.PLUS;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
        if (getType().equals(CatscriptType.INT)) {
            if (!leftHandSide.getType().equals(CatscriptType.INT)) {
                leftHandSide.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
            if (!rightHandSide.getType().equals(CatscriptType.INT)) {
                rightHandSide.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
        }
        // TODO handle strings
    }

    @Override
    public CatscriptType getType() {
        if (leftHandSide.getType().equals(CatscriptType.STRING) || rightHandSide.getType().equals(CatscriptType.STRING)) {
            return CatscriptType.STRING;
        } else {
            return CatscriptType.INT;
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
//        Object lhsValue = leftHandSide.evaluate(runtime);
//        Object rhsValue = rightHandSide.evaluate(runtime);
        if (getType().equals(CatscriptType.STRING)) {
            String lhsStrVal = String.valueOf(leftHandSide.evaluate(runtime));
            String rhsStrVal = String.valueOf(rightHandSide.evaluate(runtime));
            return lhsStrVal + rhsStrVal;
        } else {
            Integer lhsIntVal = (Integer) leftHandSide.evaluate(runtime);
            Integer rhsIntVal = (Integer) rightHandSide.evaluate(runtime);
            if (isAdd()) {
                return lhsIntVal + rhsIntVal;
            } else {
                return lhsIntVal - rhsIntVal;
            }
        }
    }

    @Override
    public void transpile(StringBuilder javascript) {
        getLeftHandSide().transpile(javascript);
        javascript.append(operator.getStringValue());
        getRightHandSide().transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        // the AdditiveExpression is used for integer types and also for strings
        // we need to handle these two possibilities
        // Situation examples
        // 1.) var x = 1 + 1 (or 1 - 1)
        // 2.) var x = 'a' + 'b'

        // Are we dealing with integers?
        if (getType().equals(CatscriptType.INT)) { // yes, we are dealing with ints
            // Example: var x = 1 - 1
            // compile left-hand side
            getLeftHandSide().compile(code);
            // compile right-hand side
            getRightHandSide().compile(code);
            // is this an additive or subtraction expression
            if (isAdd()) {
                // additive
                code.addInstruction(Opcodes.IADD);
            } else {
                // subtraction
                code.addInstruction(Opcodes.ISUB);
            }
        } else { // we are dealing with strings, potentially
            // compile left-hand side
            getLeftHandSide().compile(code);
            // is left-hand side an int?
            if (getLeftHandSide().getType().equals(CatscriptType.INT)) {
                code.addMethodInstruction(Opcodes.INVOKESTATIC, internalNameFor(String.class), "valueOf", "(I)Ljava/lang/String;");
            } else {
                code.addMethodInstruction(Opcodes.INVOKESTATIC, internalNameFor(String.class), "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            }

            // compile right-hand side
            getRightHandSide().compile(code);

            if (getRightHandSide().getType().equals(CatscriptType.INT)) {
                code.addMethodInstruction(Opcodes.INVOKESTATIC, internalNameFor(String.class), "valueOf", "(I)Ljava/lang/String;");
            } else {
                code.addMethodInstruction(Opcodes.INVOKESTATIC, internalNameFor(String.class), "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            }

            code.addMethodInstruction(Opcodes.INVOKEVIRTUAL, internalNameFor(String.class), "concat", "(Ljava/lang/String;)Ljava/lang/String;");

        }

    }

}
