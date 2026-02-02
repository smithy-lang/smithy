package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;

public class BooleanType implements Type {

    public static final BooleanType INSTANCE = new BooleanType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.BOOLEAN);

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "boolean";
    }
}
