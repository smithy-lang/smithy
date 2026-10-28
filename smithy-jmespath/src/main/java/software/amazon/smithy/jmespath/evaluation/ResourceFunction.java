/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;

/**
 * {@code resource(world, handle)} looks up a single resource instance in a world
 * snapshot (a {@code before} or {@code after} state) and returns it, or null when
 * no instance matches.
 *
 * <p>The handle is a reference structure:
 * <pre>
 * { service: String, resource: String, ids: { idName: idValue, ... } }
 * </pre>
 * where {@code service} and {@code resource} are the service and resource shape IDs.
 *
 * <p>Resolution follows the handle's coordinates into the snapshot. The world is keyed
 * by service shape ID, and each entry is the array of that service's instances. A world
 * snapshot may hold at most one instance of each service shape, so the function returns
 * null when the service has no instance, and it is an error when the service has more
 * than one. Within that instance's {@code resources} member, the instance of the
 * handle's {@code resource} shape whose {@code identifiers} all equal {@code handle.ids}
 * is returned, or null when none matches.
 *
 * <p>The returned resource instance is an object with {@code identifiers} and
 * {@code properties} members, so a property is read through {@code properties} (for
 * example {@code resource(before, handle).properties.status}).
 */
class ResourceFunction implements Function {
    @Override
    public String name() {
        return "resource";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> arguments) {
        checkArgumentCount(2, arguments);
        T world = arguments.get(0).expectObject();
        T handle = arguments.get(1).expectObject();

        T service = runtime.value(handle, runtime.createString("service"));
        T resourceType = runtime.value(handle, runtime.createString("resource"));
        if (!runtime.is(service, RuntimeType.STRING) || !runtime.is(resourceType, RuntimeType.STRING)) {
            return runtime.createNull();
        }
        T ids = runtime.value(handle, runtime.createString("ids"));

        // The world is keyed by service shape ID; each entry is the array of that
        // service's instances. A world snapshot may hold at most one instance of each
        // service shape: none resolves to null, more than one is an error.
        T instances = runtime.value(world, service);
        if (!runtime.is(instances, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }
        int count = runtime.length(instances);
        if (count == 0) {
            return runtime.createNull();
        }
        if (count > 1) {
            throw new JmespathException(JmespathExceptionType.INVALID_VALUE,
                    String.format(
                            "resource() cannot resolve service `%s`: the world snapshot holds %d instances of it, "
                                    + "but a world snapshot may hold at most one instance of each service shape",
                            runtime.asString(service),
                            count));
        }
        T serviceInstance = runtime.element(instances, 0);

        // Within the single service instance, find the resource whose identifiers match.
        T resources = runtime.value(serviceInstance, runtime.createString("resources"));
        T collection = runtime.value(resources, resourceType);
        return firstMatch(runtime, collection, ids);
    }

    private <T> T firstMatch(JmespathRuntime<T> runtime, T collection, T ids) {
        if (!runtime.is(collection, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }
        for (T instance : runtime.asIterable(collection)) {
            if (matchesIds(runtime, instance, ids)) {
                return instance;
            }
        }
        return runtime.createNull();
    }

    private <T> boolean matchesIds(JmespathRuntime<T> runtime, T instance, T ids) {
        if (!runtime.is(instance, RuntimeType.OBJECT) || !runtime.is(ids, RuntimeType.OBJECT)) {
            return false;
        }
        T identifiers = runtime.value(instance, runtime.createString("identifiers"));
        if (!runtime.is(identifiers, RuntimeType.OBJECT)) {
            return false;
        }
        for (T idName : runtime.asIterable(ids)) {
            T expected = runtime.value(ids, idName);
            T actual = runtime.value(identifiers, idName);
            if (!runtime.equal(expected, actual)) {
                return false;
            }
        }
        return true;
    }
}
