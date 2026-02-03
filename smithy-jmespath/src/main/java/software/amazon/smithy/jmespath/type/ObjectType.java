package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.EnumSet;
import java.util.Map;

// TODO: RecordType? StructureType?
public class ObjectType implements Type {

    private final Map<String, Type> properties;

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.OBJECT);

    public ObjectType(Map<String, Type> properties) {
        this.properties = properties;
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type valueType(String key) {
        return properties == null ? Type.nullType() : properties.getOrDefault(key, Type.nullType());
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
