package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;
import java.util.Map;

public class MapType implements Type {

    private final Type valueType;

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.OBJECT);

    public MapType(Type valueType) {
        this.valueType = valueType;
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type valueType(String key) {
        return Type.unionType(valueType, Type.nullType());
    }

    @Override
    public String toString() {
        return "object[" + valueType + "]";
    }
}
