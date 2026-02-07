package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Map;

// TODO: RecordType? StructureType?
public class ObjectType implements Type {

    // TODO: Optional keys as well (may not be present, but if so has type X)
    // Not the same thing as always present but mapped to null
    private final Map<String, Type> properties;

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.OBJECT);

    public ObjectType(Map<String, Type> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectType)) {
            return false;
        }

        ObjectType other = (ObjectType) obj;
        return properties.equals(other.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public <T> boolean isInstance(T value, JmespathRuntime<T> runtime) {
        if (!runtime.is(value, RuntimeType.OBJECT)){
            return false;
        }

        if (properties != null) {
            // TODO
            return false;
        } else {
            return true;
        }
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type valueType(Type key) {
        return properties == null ? Type.anyType() : properties.getOrDefault(key, Type.nullType());
    }

    @Override
    public String toString() {
        if (properties == null) {
            return "object";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("object<");
        for (Map.Entry<String, Type> entry : properties.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }
        builder.append('>');
        return builder.toString();
    }
}
