/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.utils.BuilderRef;

class TypedPropertiesBag {

    private final Map<String, Object> properties;
    private final Map<Property<?>, Object> typedProperties;

    TypedPropertiesBag(Builder<?> bagBuilder) {
        this.properties = bagBuilder.properties.copy();
        this.typedProperties = bagBuilder.typedProperties.copy();
    }

    /**
     * Gets the additional properties of the object.
     *
     * @return Returns a map of additional property strings.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Gets the additional typed properties of the object.
     *
     * @return Returns a map of additional typed properties.
     */
    public Map<Property<?>, Object> getTypedProperties() {
        return typedProperties;
    }

    /**
     * Gets a specific property if present.
     *
     * @param name Property to retrieve.
     * @return Returns the optionally found property.
     */
    public Optional<Object> getProperty(String name) {
        return Optional.ofNullable(properties.get(name));
    }

    /**
     * Get a typed property if present.
     *
     * @param property   property key to get by exact reference identity.
     * @param <T> value type of the property
     * @return Returns the optionally found property.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(Property<T> property) {
        return Optional.ofNullable((T) typedProperties.get(property));
    }

    /**
     * Gets an additional property of a specific type.
     *
     * @param name Name of the property to get.
     * @param type Type of value to expect.
     * @param <T> Type of value to expect.
     * @return Returns a map of additional property strings.
     * @throws IllegalArgumentException if the value is not of the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String name, Class<T> type) {
        return getProperty(name)
                .map(value -> {
                    if (!type.isInstance(value)) {
                        throw new IllegalArgumentException(String.format(
                                "%s property `%s` of `%s` is not an instance of `%s`. Found `%s`",
                                getClass().getSimpleName(),
                                name,
                                this,
                                type.getName(),
                                value.getClass().getName()));
                    }
                    return (T) value;
                });
    }

    /**
     * Gets a specific additional property or throws if missing.
     *
     * @param name Property to retrieve.
     * @return Returns the found property.
     * @throws IllegalArgumentException if the property is not present.
     */
    public Object expectProperty(String name) {
        return getProperty(name).orElseThrow(() -> new IllegalArgumentException(String.format(
                "Property `%s` is not part of %s, `%s`",
                name,
                getClass().getSimpleName(),
                this)));
    }

    /**
     * Gets a specific additional property or throws if missing or if the
     * property is not an instance of the given type.
     *
     * @param name Property to retrieve.
     * @param type Type of value to expect.
     * @param <T> Type of value to expect.
     * @return Returns the found property.
     * @throws IllegalArgumentException if the property is not present.
     * @throws IllegalArgumentException if the value is not of the given type.
     */
    public <T> T expectProperty(String name, Class<T> type) {
        return getProperty(name, type).orElseThrow(() -> new IllegalArgumentException(String.format(
                "Property `%s` is not part of %s, `%s`",
                name,
                getClass().getSimpleName(),
                this)));
    }

    /**
     * Get a property and throw if it isn't present.
     *
     * @param property property key to get by exact reference identity.
     * @param <T> value type of the property.
     * @throws IllegalArgumentException if the property isn't found.
     */
    public <T> T expectProperty(Property<T> property) {
        return getProperty(property).orElseThrow(() -> new IllegalArgumentException(String.format(
                "Property `%s` expected but not found on %s",
                property,
                this)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof TypedPropertiesBag)) {
            return false;
        } else {
            return properties.equals(((TypedPropertiesBag) o).properties);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    /**
     * Builds a SymbolReference.
     */
    abstract static class Builder<T extends Builder<T>> {
        BuilderRef<Map<String, Object>> properties = BuilderRef.forOrderedMap();
        BuilderRef<Map<Property<?>, Object>> typedProperties = BuilderRef.forOrderedMap();

        /**
         * Sets a specific custom property.
         *
         * @param key Key to set.
         * @param value Value to set.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public T putProperty(String key, Object value) {
            properties.get().put(key, value);
            return (T) this;
        }

        /**
         * Sets a specific, typed custom property.
         *
         * @param property Key to set.
         * @param value Value to set.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public <K> T putProperty(Property<K> property, K value) {
            typedProperties.get().put(property, value);
            return (T) this;
        }

        /**
         * Removes a specific custom property.
         *
         * @param key Key to remove.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public T removeProperty(String key) {
            properties.get().remove(key);
            return (T) this;
        }

        /**
         * Removes a specific, typed custom property.
         *
         * @param property Property to remove.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public T removeProperty(Property<?> property) {
            typedProperties.get().remove(property);
            return (T) this;
        }

        /**
         * Replaces all the custom properties.
         *
         * @param properties Custom properties to replace with.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public T properties(Map<String, Object> properties) {
            this.properties.clear();
            this.properties.get().putAll(properties);
            return (T) this;
        }

        /**
         * Replaces all the custom typed properties.
         *
         * @param properties Custom typed properties to replace with.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public T typedProperties(Map<Property<?>, Object> properties) {
            this.typedProperties.clear();
            this.typedProperties.get().putAll(properties);
            return (T) this;
        }
    }
}
