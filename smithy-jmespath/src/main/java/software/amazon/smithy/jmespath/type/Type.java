package software.amazon.smithy.jmespath.type;

import java.lang.reflect.Array;

public interface Type {

    boolean isArray();

    ArrayType expectArray();
}
