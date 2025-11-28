package software.amazon.smithy.jmespath.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ListArrayBuilder<T> implements JmespathRuntime.ArrayBuilder<T> {

    private final JmespathRuntime<T> runtime;
    private final List<T> result = new ArrayList<>();
    private final Function<List<T>, T> wrapping;

    public ListArrayBuilder(JmespathRuntime<T> runtime, Function<List<T>, T> wrapping) {
        this.runtime = runtime;
        this.wrapping = wrapping;
    }

    @Override
    public void add(T value) {
        result.add(value);
    }

    @Override
    public void addAll(T array) {
        Iterable<? extends T> iterable = runtime.toIterable(array);
        if (iterable instanceof Collection<?>) {
            result.addAll((Collection<? extends T>) iterable);
        } else {
            for (T value : iterable) {
                result.add(value);
            }
        }
    }

    @Override
    public T build() {
        return wrapping.apply(result);
    }
}
