package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.LiteralExpressionJmespathRuntime;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

import java.util.Iterator;
import java.util.function.Function;

public class WrappingIterable<T, R> implements Iterable<R> {

    private final Iterable<T> inner;
    private final Function<? super T, ? extends R> mapping;

    public WrappingIterable(Function<T, R> mapping, Iterable<T> inner) {
        this.inner = inner;
        this.mapping = mapping;
    }

    @Override
    public Iterator<R> iterator() {
        return new WrappingIterator(inner.iterator());
    }

    private class WrappingIterator implements Iterator<R> {

        private final Iterator<? extends T> inner;

        private WrappingIterator(Iterator<? extends T> inner) {
            this.inner = inner;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public R next() {
            return mapping.apply(inner.next());
        }
    }
}
