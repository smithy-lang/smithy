package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;

public class StringType implements Type {

    public static final StringType INSTANCE = new StringType();

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.STRING);

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public String toString() {
        return "string";
    }
}
