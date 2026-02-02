package software.amazon.smithy.jmespath.type;

import java.util.Arrays;
import java.util.List;

public class UnionType implements Type {

    private final List<Type> types;

    public UnionType(Type ... types) {
        this.types = Arrays.asList(types);
    }

    public UnionType(List<Type> types) {
        this.types = types;
    }
}
