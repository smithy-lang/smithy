package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;

public class NumberType implements Type {

    public static final NumberType INSTANCE = new NumberType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.NULL);

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "number";
    }
}
