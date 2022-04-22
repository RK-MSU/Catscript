package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.statements.FunctionDefinitionStatement;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class FunctionCallExpression extends Expression {
    private final String name;
    List<Expression> arguments;
    private CatscriptType type;

    private String funcDefinition;

    public FunctionCallExpression(String functionName, List<Expression> arguments) {
        this.arguments = new LinkedList<>();
        for (Expression value : arguments) {
            this.arguments.add(addChild(value));
        }
        this.name = functionName;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    public String getName() {
        return name;
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        FunctionDefinitionStatement function = symbolTable.getFunction(getName());
        if (function == null) {
            addError(ErrorType.UNKNOWN_NAME);
            type = CatscriptType.OBJECT;
        } else {
            type = function.getType();
            if (arguments.size() != function.getParameterCount()) {
                addError(ErrorType.ARG_MISMATCH);
            } else {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression argument = arguments.get(i);
                    argument.validate(symbolTable);
                    CatscriptType parameterType = function.getParameterType(i);
                    if (!parameterType.isAssignableFrom(argument.getType())) {
                        argument.addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
            }
        }

        funcDefinition = function.getDescriptor();

    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        List<Object> fncArgs = new ArrayList<>();
        for (Expression expr : arguments) {
            fncArgs.add(expr.evaluate(runtime));
        }
        FunctionDefinitionStatement fnc = getProgram().getFunction(name);
        return fnc.invoke(runtime, fncArgs);

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    public void compile(ByteCodeGenerator code) {
        // build the descriptor
        String descriptor = "(";
        // iterate over arguments
        for (Expression argumentExpression : getArguments()) {
            if (argumentExpression.getType().equals(CatscriptType.BOOLEAN) || argumentExpression.getType().equals(CatscriptType.INT)) {
                descriptor = descriptor + "I";
            } else {
                descriptor = descriptor + "L" + internalNameFor(getType().getJavaType()) + ";";
            }
        }
        descriptor = descriptor + ")";
        if (getType().equals(CatscriptType.VOID)) {
            descriptor = descriptor + "V";
        } else if (type.equals(CatscriptType.BOOLEAN) || type.equals(CatscriptType.INT)) {
            descriptor = descriptor + "I";
        } else {
            descriptor = descriptor + "L" + internalNameFor(getType().getJavaType()) + ";";
        }


        code.addVarInstruction(Opcodes.ALOAD, 0);

        for (Expression arg : getArguments()) {
            arg.compile(code);
            if(!descriptor.equals(funcDefinition)) {
                box(code, arg.getType());
            }
        }

        code.addMethodInstruction(Opcodes.INVOKEVIRTUAL,
                code.getProgramInternalName(),
                getName(),
                funcDefinition);
    }

}
