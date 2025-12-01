package software.amazon.smithy.jmespath.evaluation;

import java.util.HashMap;
import java.util.Map;

public class InheritingClassMap<T> {

    private final Map<Class<?>, T> map = new HashMap<>();

    public T get(Class<?> clazz) {
        Class<?> c = clazz;
        while (c != null) {
            T value = map.get(c);
            if (value != null) {
                return value;
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
