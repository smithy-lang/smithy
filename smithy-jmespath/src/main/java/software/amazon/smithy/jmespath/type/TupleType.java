package software.amazon.smithy.jmespath.type;

import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class TupleType implements Type {

    private static final EnumSet<RuntimeType> TYPES = EnumSet.of(RuntimeType.ARRAY);

    // Never null - array is equivalent to array<any>
    private final List<Type> members;

    public TupleType(List<Type> members) {
        this.members = members;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TupleType)) {
            return false;
        }
        TupleType other = (TupleType) obj;
        return members.equals(other.members);
    }

    @Override
    public int hashCode() {
        return members.hashCode();
    }

    @Override
    public <T> boolean isInstance(T array, JmespathRuntime<T> runtime) {
        if (!runtime.is(array, RuntimeType.ARRAY)) {
            return false;
        }

        Iterator<Type> memberIter = members.iterator();
        Iterator<? extends T> valueIter = runtime.asIterable(array).iterator();
        while (valueIter.hasNext()) {
            if (!memberIter.hasNext()) {
                return false;
            }
            Type member = memberIter.next();
            T value = valueIter.next();
            if (!member.isInstance(value, runtime)) {
                return false;
            }
        }
        return !memberIter.hasNext();
    }

    @Override
    public EnumSet<RuntimeType> runtimeTypes() {
        return TYPES;
    }

    @Override
    public Type elementType(int index) {
        if (index < 0 || index > members.size()) {
            return Type.nullType();
        }
        return members.get(index);
    }

    @Override
    public Type elementType() {
        // TODO: precalculate
        // TODO: helper method
        Type result = BottomType.INSTANCE;
        for (Type member : members) {
            result = Type.unionType(result, member);
        }
        return result;
    }

    @Override
    public String toString() {
        return "tuple[" + members.stream().map(Type::toString).collect(Collectors.joining(", ")) + "]";
    }
}
