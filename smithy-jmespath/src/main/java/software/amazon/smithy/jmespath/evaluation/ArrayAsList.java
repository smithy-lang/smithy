package software.amazon.smithy.jmespath.evaluation;

import java.util.AbstractList;
import java.util.List;

public class ArrayAsList<T> extends AbstractList<T> {

    private final JmespathRuntime<T> runtime;
    private final T array;

    public ArrayAsList(JmespathRuntime<T> runtime, T array) {
        this.runtime = runtime;
        this.array = array;
    }

    @Override
    public T get(int index) {
        return runtime.element(array, runtime.createNumber(index));
    }

    @Override
    public int size() {
        return runtime.length(array).intValue();
    }

    // TODO: iterator
}
