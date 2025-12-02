package software.amazon.smithy.jmespath.evaluation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InheritingClassMap<T> {

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private final Map<Class<?>, T> map;

    private InheritingClassMap(Map<Class<?>, T> map) {
        this.map = map;
    }

    public T get(Class<?> clazz) {
        // Fast path
        T result = map.get(clazz);
        if (result != null) {
            return result;
        }

        // Slow path (cache miss)
        // Recursively check supertypes, throwing if there is any conflict
        Class<?> superclass = clazz.getSuperclass();
        Class<?> matchingClass = superclass;
        if (superclass != null) {
            result = get(superclass);
        }
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            T interfaceResult = get(interfaceClass);
            if (interfaceResult != null) {
                if (result != null) {
                    throw new RuntimeException("Duplicate match for " + clazz + ": "
                            + matchingClass + " and " + interfaceClass);
                }
                matchingClass = interfaceClass;
                result = interfaceResult;
            }
        }

        // Cache the value directly even if it's a null.
        map.put(clazz, result);

        return result;
    }

    public static class Builder<T> {

        private final Map<Class<?>, T> map = new ConcurrentHashMap<>();

        public Builder<T> put(Class<?> clazz, T value) {
            map.put(clazz, Objects.requireNonNull(value));
            return this;
        }

        public InheritingClassMap<T> build() {
            return new InheritingClassMap<>(map);
        }
    }
}
