package software.amazon.smithy.jmespath.type;

import java.util.Map;

public class ObjectType {

    private final Map<String, Type> properties;

    public ObjectType(Map<String, Type> properties) {
        this.properties = properties;
    }
}
