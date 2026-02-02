package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;

public class AnyType implements Type {

    public static final AnyType INSTANCE = new AnyType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.allOf(RuntimeType.class);

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }
}
