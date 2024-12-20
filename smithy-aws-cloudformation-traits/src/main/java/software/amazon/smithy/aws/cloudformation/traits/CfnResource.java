/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains extracted resource information.
 */
public final class CfnResource implements ToSmithyBuilder<CfnResource> {
    private final Map<String, CfnResourceProperty> propertyDefinitions;
    private final Map<String, CfnResourceProperty> availableProperties;
    private final Set<ShapeId> excludedProperties;
    private final Set<String> primaryIdentifiers;
    private final List<Set<String>> additionalIdentifiers;

    private CfnResource(Builder builder) {
        Map<String, CfnResourceProperty> propertyDefinitions = new HashMap<>(builder.propertyDefinitions);
        Map<String, CfnResourceProperty> availableProperties = new HashMap<>();
        this.excludedProperties = Collections.unmodifiableSet(builder.excludedProperties);
        this.primaryIdentifiers = Collections.unmodifiableSet(builder.primaryIdentifiers);
        this.additionalIdentifiers = Collections.unmodifiableList(builder.additionalIdentifiers);

        // Pre-compute the properties available, cleaning up any exclusions.
        for (Map.Entry<String, CfnResourceProperty> propertyDefinition : propertyDefinitions.entrySet()) {
            for (ShapeId shapeId : propertyDefinition.getValue().getShapeIds()) {
                if (excludedProperties.contains(shapeId)) {
                    // Remove an excluded ShapeId for validation.
                    CfnResourceProperty updatedDefinition = propertyDefinition.getValue()
                            .toBuilder()
                            .removeShapeId(shapeId)
                            .build();
                    propertyDefinitions.put(propertyDefinition.getKey(), updatedDefinition);

                    // Update any definition in available properties to also
                    // remove the excluded ShapeId.
                    if (availableProperties.containsKey(propertyDefinition.getKey())) {
                        availableProperties.put(propertyDefinition.getKey(), updatedDefinition);
                    }
                } else {
                    availableProperties.put(propertyDefinition.getKey(), propertyDefinition.getValue());
                }
            }
        }

        this.propertyDefinitions = Collections.unmodifiableMap(propertyDefinitions);
        this.availableProperties = Collections.unmodifiableMap(availableProperties);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get all property definitions of the CloudFormation resource.
     *
     * <p>Properties excluded by the {@code cfnExcludedProperty} trait are not
     * returned.
     *
     * @see CfnResource#getExcludedProperties()
     *
     * @return Returns all members that map to CloudFormation resource
     *   properties.
     */
    public Map<String, CfnResourceProperty> getProperties() {
        return availableProperties;
    }

    /**
     * Gets the definition of the specified property of the CloudFormation resource.
     *
     * <p>An empty {@code Optional} will be returned if the requested property
     * has been excluded by the {@code cfnExcludedProperty} trait.
     *
     * @see CfnResource#getExcludedProperties()
     *
     * @param propertyName Name of the property to retrieve
     * @return The property definition.
     */
    public Optional<CfnResourceProperty> getProperty(String propertyName) {
        return Optional.ofNullable(getProperties().get(propertyName));
    }

    /**
     * Get create-specifiable-only property definitions of the CloudFormation resource.
     *
     * These properties can be specified only during resource creation and
     * can be returned in a {@code read} or {@code list} request.
     *
     * @return Returns create-only member names that map to CloudFormation resource
     *   properties.
     */
    public Set<String> getCreateOnlyProperties() {
        return getConstrainedProperties(definition -> {
            Set<CfnResourceIndex.Mutability> mutabilities = definition.getMutabilities();
            return mutabilities.contains(CfnResourceIndex.Mutability.CREATE)
                    && !mutabilities.contains(CfnResourceIndex.Mutability.WRITE);
        });
    }

    /**
     * Get read-only property definitions of the CloudFormation resource.
     *
     * These properties can be returned by a {@code read} or {@code list} request,
     * but cannot be set by the user.
     *
     * @return Returns read-only member names that map to CloudFormation resource
     *   properties.
     */
    public Set<String> getReadOnlyProperties() {
        return getConstrainedProperties(definition -> {
            Set<CfnResourceIndex.Mutability> mutabilities = definition.getMutabilities();
            return mutabilities.size() == 1 && mutabilities.contains(CfnResourceIndex.Mutability.READ);
        });
    }

    /**
     * Get write-only property definitions of the CloudFormation resource.
     *
     * These properties can be specified by the user, but cannot be
     * returned by a {@code read} or {@code list} request.
     *
     * @return Returns write-only member names that map to CloudFormation resource
     *   properties.
     */
    public Set<String> getWriteOnlyProperties() {
        return getConstrainedProperties(definition -> {
            Set<CfnResourceIndex.Mutability> mutabilities = definition.getMutabilities();
            // Create and non-read properties need to be set as createOnly and writeOnly.
            if (mutabilities.size() == 1 && mutabilities.contains(CfnResourceIndex.Mutability.CREATE)) {
                return true;
            }

            // Otherwise, create and update, or update only become writeOnly.
            return mutabilities.contains(CfnResourceIndex.Mutability.WRITE)
                    && !mutabilities.contains(CfnResourceIndex.Mutability.READ);
        });
    }

    private Set<String> getConstrainedProperties(Predicate<CfnResourceProperty> constraint) {
        return getProperties().entrySet()
                .stream()
                .filter(property -> constraint.test(property.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Get members that have been explicitly excluded from the CloudFormation
     * resource.
     *
     * @return Returns members that have been excluded from a CloudFormation
     *   resource.
     */
    public Set<ShapeId> getExcludedProperties() {
        return excludedProperties;
    }

    /**
     * Gets a set of identifier names that represent the primary way to identify
     * a CloudFormation resource. This uniquely identifies an individual instance
     * of the resource type, and can be one or more properties to represent
     * composite-key identifiers.
     *
     * @return Returns the identifier set primarily used to access a
     *   CloudFormation resource.
     */
    public Set<String> getPrimaryIdentifiers() {
        return primaryIdentifiers;
    }

    /**
     * Get a list of sets of member shape ids, each set can be used to identify
     * the CloudFormation resource in addition to its primary identifier(s).
     *
     * @return Returns identifier sets used to access a CloudFormation resource.
     */
    public List<Set<String>> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .propertyDefinitions(propertyDefinitions)
                .excludedProperties(excludedProperties)
                .primaryIdentifiers(primaryIdentifiers)
                .additionalIdentifiers(additionalIdentifiers);
    }

    public static final class Builder implements SmithyBuilder<CfnResource> {
        private final Map<String, CfnResourceProperty> propertyDefinitions = new HashMap<>();
        private final Set<ShapeId> excludedProperties = new HashSet<>();
        private final Set<String> primaryIdentifiers = new HashSet<>();
        private final List<Set<String>> additionalIdentifiers = new ArrayList<>();

        private Builder() {}

        public boolean hasPropertyDefinition(String propertyName) {
            return propertyDefinitions.containsKey(propertyName);
        }

        public Builder putPropertyDefinition(String propertyName, CfnResourceProperty definition) {
            propertyDefinitions.put(propertyName, definition);
            return this;
        }

        public Builder updatePropertyDefinition(
                String propertyName,
                Function<CfnResourceProperty, CfnResourceProperty> updater
        ) {
            CfnResourceProperty definition = propertyDefinitions.get(propertyName);

            // Don't update if we don't have a property or it's already locked.
            if (definition == null || definition.hasExplicitMutability()) {
                return this;
            }

            return putPropertyDefinition(propertyName, updater.apply(definition));
        }

        public Builder propertyDefinitions(Map<String, CfnResourceProperty> propertyDefinitions) {
            this.propertyDefinitions.clear();
            this.propertyDefinitions.putAll(propertyDefinitions);
            return this;
        }

        public Builder addExcludedProperty(ShapeId excludedProperty) {
            this.excludedProperties.add(excludedProperty);
            return this;
        }

        public Builder excludedProperties(Set<ShapeId> excludedProperties) {
            this.excludedProperties.clear();
            this.excludedProperties.addAll(excludedProperties);
            return this;
        }

        public Builder addPrimaryIdentifier(String primaryIdentifier) {
            this.primaryIdentifiers.add(primaryIdentifier);
            return this;
        }

        public Builder primaryIdentifiers(Set<String> primaryIdentifiers) {
            this.primaryIdentifiers.clear();
            this.primaryIdentifiers.addAll(primaryIdentifiers);
            return this;
        }

        public Builder addAdditionalIdentifier(Set<String> additionalIdentifier) {
            this.additionalIdentifiers.add(additionalIdentifier);
            return this;
        }

        public Builder additionalIdentifiers(List<Set<String>> additionalIdentifiers) {
            this.additionalIdentifiers.clear();
            this.additionalIdentifiers.addAll(additionalIdentifiers);
            return this;
        }

        @Override
        public CfnResource build() {
            return new CfnResource(this);
        }
    }
}
