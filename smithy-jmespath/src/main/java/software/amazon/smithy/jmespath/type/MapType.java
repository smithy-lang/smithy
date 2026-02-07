package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Map;

public class MapType implements Type {

    private final Type keyType;
    private final Type valueType;

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.OBJECT);

    public MapType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MapType) {
            MapType other = (MapType) obj;
            return keyType.equals(other.keyType) && valueType.equals(other.valueType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return keyType.hashCode() * 31 + valueType.hashCode();
    }

    @Override
    public <T> boolean isInstance(T object, JmespathRuntime<T> runtime) {
        if (!runtime.is(object, RuntimeType.OBJECT)) {
            return false;
        }
        for (T key : runtime.asIterable(object)) {
            if (!keyType.isInstance(key, runtime)) {
                return false;
            }
            T value = runtime.value(object, key);
            if (!valueType.isInstance(value, runtime)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type valueType(Type key) {
        return Type.unionType(valueType, Type.nullType());
    }

    @Override
    public String toString() {
        return "map[" + keyType + ", " + valueType + "]";
    }
}
