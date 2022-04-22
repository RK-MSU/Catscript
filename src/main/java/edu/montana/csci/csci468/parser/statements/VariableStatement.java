package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class VariableStatement extends Statement {
    private Expression expression;
    private String variableName;
    private CatscriptType explicitType;
    private CatscriptType type;

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public void setExplicitType(CatscriptType type) {
        this.explicitType = type;
    }

    public CatscriptType getExplicitType() {
        return explicitType;
    }

    public boolean isGlobal() {
        return getParent() instanceof CatScriptProgram;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            if (explicitType != null) {
                type = explicitType;
            } else {
                type = expression.getType();
            }
            if (!type.isAssignableFrom(expression.getType())) {
                addError(ErrorType.INCOMPATIBLE_TYPES);
            }
            symbolTable.registerSymbol(variableName, type);
        }
    }

    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        runtime.setValue(variableName, expression.evaluate(runtime));
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        // global variables
        if (isGlobal()) { // store in a field
            // There is one more condition to check
            // - if the type is an INT
            // - if the type is a BOOLEAN
            // there is a distinction between the integer type and the reference type

            code.addVarInstruction(Opcodes.ALOAD, 0);
            expression.compile(code);
            String descName = "I";

            if (!getType().equals(CatscriptType.INT) && !getType().equals(CatscriptType.BOOLEAN)) {
                descName = "L" + internalNameFor(getType().getJavaType()) + ";";
            }

            code.addField(variableName, descName);
            code.addFieldInstruction(Opcodes.PUTFIELD, variableName, descName, code.getProgramInternalName());

        } else { // store in a slot
            // create a local storage slot
            Integer localSlot = code.createLocalStorageSlotFor(variableName);

            // compile code
            expression.compile(code);

            // there is a distinction between the integer type and the reference type
            if (getType().equals(CatscriptType.INT) || getType().equals(CatscriptType.BOOLEAN)) {
                code.addVarInstruction(Opcodes.ISTORE, localSlot);
            } else {
                code.addVarInstruction(Opcodes.ASTORE, localSlot);
            }
        }
    }
}
