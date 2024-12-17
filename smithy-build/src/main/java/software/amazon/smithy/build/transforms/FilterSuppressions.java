/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.suppressions.Suppression;

/**
 * Filters suppressions found in metadata and {@link SuppressTrait} traits.
 */
public final class FilterSuppressions extends ConfigurableProjectionTransformer<FilterSuppressions.Config> {

    private static final Logger LOGGER = Logger.getLogger(FilterSuppressions.class.getName());

    /**
     * {@code filterSuppressions} configuration settings.
     */
    public static final class Config {

        private boolean removeUnused = false;
        private boolean removeReasons = false;
        private Set<String> eventIdAllowList = Collections.emptySet();
        private Set<String> eventIdDenyList = Collections.emptySet();
        private Set<String> namespaceAllowList = Collections.emptySet();
        private Set<String> namespaceDenyList = Collections.emptySet();

        /**
         * Gets whether unused suppressions are removed.
         *
         * @return Returns true if unused suppressions are removed.
         */
        public boolean getRemoveUnused() {
            return removeUnused;
        }

        /**
         * Set to true to remove suppressions that have no effect.
         *
         * <p>If a validation event ID is never emitted, then {@code suppress} traits
         * will be updated to no longer refer to the ID and removed if they no longer
         * refer to any event. Metadata suppressions are also removed if they have no
         * effect.
         *
         * @param removeUnused Set to true to remove unused suppressions.
         */
        public void setRemoveUnused(boolean removeUnused) {
            this.removeUnused = removeUnused;
        }

        /**
         * Gets whether suppression reasons are removed.
         *
         * @return Returns true if suppression reasons are removed.
         */
        public boolean getRemoveReasons() {
            return removeReasons;
        }

        /**
         * Set to true to remove the {@code reason} property from metadata suppressions.
         *
         * <p>The reason for a suppression could reveal internal or sensitive information.
         * Removing the "reason" from metadata suppressions is an extra step teams can
         * take to ensure they do not leak internal information when publishing models
         * outside of their organization.
         *
         * @param removeReasons Set to true to remove reasons.
         */
        public void setRemoveReasons(boolean removeReasons) {
            this.removeReasons = removeReasons;
        }

        /**
         * Gets a list of allowed event IDs.
         *
         * @return Returns the allow list of event IDs.
         */
        public Set<String> getEventIdAllowList() {
            return eventIdAllowList;
        }

        /**
         * Sets a list of event IDs that can be referred to in suppressions.
         *
         * <p>Suppressions that refer to any other event ID will be updated to
         * no longer refer to them, or removed if they no longer refer to any events.
         *
         * <p>This setting cannot be used in tandem with {@code eventIdDenyList}.
         *
         * @param eventIdAllowList IDs to allow.
         */
        public void setEventIdAllowList(Set<String> eventIdAllowList) {
            this.eventIdAllowList = eventIdAllowList;
        }

        /**
         * Gets a list of denied event IDs.
         *
         * @return Gets the event ID deny list.
         */
        public Set<String> getEventIdDenyList() {
            return eventIdDenyList;
        }

        /**
         * Sets a list of event IDs that cannot be referred to in suppressions.
         *
         * <p>Suppressions that refer to any of these event IDs will be updated to
         * no longer refer to them, or removed if they no longer refer to any events.
         *
         * <p>This setting cannot be used in tandem with {@code eventIdAllowList}.
         *
         * @param eventIdDenyList IDs to deny.
         */
        public void setEventIdDenyList(Set<String> eventIdDenyList) {
            this.eventIdDenyList = eventIdDenyList;
        }

        /**
         * Gets the metadata suppression namespace allow list.
         *
         * @return The metadata suppression namespace allow list.
         */
        public Set<String> getNamespaceAllowList() {
            return namespaceAllowList;
        }

        /**
         * Sets a list of namespaces that can be referred to in metadata suppressions.
         *
         * <p>Metadata suppressions that refer to namespaces outside of this list,
         * including "*", will be removed.
         *
         * <p>This setting cannot be used in tandem with {@code namespaceDenyList}.
         *
         * @param namespaceAllowList Namespaces to allow.
         */
        public void setNamespaceAllowList(Set<String> namespaceAllowList) {
            this.namespaceAllowList = namespaceAllowList;
        }

        /**
         * Gets the metadata suppression namespace deny list.
         *
         * @return The metadata suppression namespace deny list.
         */
        public Set<String> getNamespaceDenyList() {
            return namespaceDenyList;
        }

        /**
         * Sets a list of namespaces that cannot be referred to in metadata suppressions.
         *
         * <p>Metadata suppressions that refer to namespaces in this list,
         * including "*", will be removed.
         *
         * <p>This setting cannot be used in tandem with {@code namespaceAllowList}.
         *
         * @param namespaceDenyList Namespaces to deny.
         */
        public void setNamespaceDenyList(Set<String> namespaceDenyList) {
            this.namespaceDenyList = namespaceDenyList;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "filterSuppressions";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        if (!config.getEventIdAllowList().isEmpty() && !config.getEventIdDenyList().isEmpty()) {
            throw new SmithyBuildException(getName() + ": cannot set both eventIdAllowList values and "
                    + "eventIdDenyList values at the same time");
        }

        if (!config.getNamespaceAllowList().isEmpty() && !config.getNamespaceDenyList().isEmpty()) {
            throw new SmithyBuildException(getName() + ": cannot set both namespaceAllowList values and "
                    + "namespaceDenyList values at the same time");
        }

        Model model = context.getModel();
        Set<String> removedValidators = getRemovedValidators(context, config);
        List<ValidationEvent> suppressedEvents = context.getOriginalModelValidationEvents()
                .stream()
                .filter(event -> event.getSeverity() == Severity.SUPPRESSED)
                .filter(event -> !removedValidators.contains(event.getId()))
                .collect(Collectors.toList());
        model = filterSuppressionTraits(model, config, suppressedEvents, context.getTransformer());
        model = filterMetadata(model, config, suppressedEvents, removedValidators);
        return model;
    }

    private Set<String> getRemovedValidators(TransformContext context, Config config) {
        // Validators could have been removed by other transforms in this projection,
        // and if they were removed, then validation events referring to them are
        // no longer relevant. We don't want to keep suppressions around for validators
        // that were removed.
        if (!context.getOriginalModel().isPresent()) {
            return Collections.emptySet();
        }

        Set<String> originalValidators = getValidatorNames(context.getOriginalModel().get());
        Set<String> updatedValidators = getValidatorNames(context.getModel());

        if (originalValidators.equals(updatedValidators)) {
            return Collections.emptySet();
        }

        originalValidators.removeAll(updatedValidators);

        if (config.getRemoveUnused()) {
            LOGGER.info(() -> "Detected the removal of the following validators: "
                    + originalValidators
                    + ". Suppressions that refer to these validators will be removed.");
        }

        return originalValidators;
    }

    private Set<String> getValidatorNames(Model model) {
        ArrayNode validators = model.getMetadata()
                .getOrDefault("validators", Node.arrayNode())
                .expectArrayNode();
        Set<String> metadataSuppressions = new HashSet<>();
        for (Node validator : validators) {
            ObjectNode validatorObject = validator.expectObjectNode();
            String id = validatorObject
                    .getStringMember("id")
                    .orElseGet(() -> validatorObject.expectStringMember("name"))
                    .getValue();
            metadataSuppressions.add(id);
        }
        return metadataSuppressions;
    }

    private Model filterSuppressionTraits(
            Model model,
            Config config,
            List<ValidationEvent> suppressedEvents,
            ModelTransformer transformer
    ) {

        List<Shape> replacementShapes = new ArrayList<>();
        // First filter and '@suppress' traits that didn't suppress anything.
        for (Shape shape : model.getShapesWithTrait(SuppressTrait.class)) {
            SuppressTrait trait = shape.expectTrait(SuppressTrait.class);
            List<String> allowed = new ArrayList<>(trait.getValues());
            allowed.removeIf(value -> !isAllowed(value, config.getEventIdAllowList(), config.getEventIdDenyList()));

            // Only keep IDs that actually acted to suppress an event.
            if (config.getRemoveUnused()) {
                Set<ValidationEvent> matchedEvents = suppressedEvents.stream()
                        .filter(event -> Objects.equals(shape.getId(), event.getShapeId().orElse(null)))
                        .collect(Collectors.toSet());
                allowed.removeIf(value -> matchedEvents.stream().noneMatch(event -> event.containsId(value)));
            }

            if (allowed.isEmpty()) {
                replacementShapes.add(Shape.shapeToBuilder(shape).removeTrait(SuppressTrait.ID).build());
            } else if (!allowed.equals(trait.getValues())) {
                trait = trait.toBuilder().values(allowed).build();
                replacementShapes.add(Shape.shapeToBuilder(shape).addTrait(trait).build());
            }
        }
        return transformer.replaceShapes(model, replacementShapes);
    }

    private Model filterMetadata(
            Model model,
            Config config,
            List<ValidationEvent> suppressedEvents,
            Set<String> removedValidators
    ) {
        // Next remove metadata suppressions that didn't suppress anything.
        ArrayNode suppressionsNode = model.getMetadata()
                .getOrDefault("suppressions", Node.arrayNode())
                .expectArrayNode();
        List<ObjectNode> updatedMetadataSuppressions = new ArrayList<>();

        for (Node suppressionNode : suppressionsNode) {
            ObjectNode object = suppressionNode.expectObjectNode();
            String id = object.getStringMemberOrDefault("id", "");
            String namespace = object.getStringMemberOrDefault("namespace", "");
            if (config.getRemoveReasons()) {
                object = object.withoutMember("reason");
            }

            // Only keep the suppression if it passes each filter.
            if (isAllowed(id, config.getEventIdAllowList(), config.getEventIdDenyList())
                    && isAllowed(namespace, config.getNamespaceAllowList(), config.getNamespaceDenyList())) {
                if (!config.getRemoveUnused()) {
                    updatedMetadataSuppressions.add(object);
                } else {
                    Suppression suppression = Suppression.fromMetadata(object);
                    for (ValidationEvent event : suppressedEvents) {
                        if (!removedValidators.contains(event.getId()) && suppression.test(event)) {
                            updatedMetadataSuppressions.add(object);
                            break;
                        }
                    }
                }
            }
        }

        ArrayNode updatedMetadataSuppressionsNode = Node.fromNodes(updatedMetadataSuppressions);
        if (suppressionsNode.equals(updatedMetadataSuppressionsNode)) {
            return model;
        }
        Model.Builder builder = model.toBuilder();
        builder.removeMetadataProperty("suppressions");
        if (!updatedMetadataSuppressions.isEmpty()) {
            builder.putMetadataProperty("suppressions", updatedMetadataSuppressionsNode);
        }
        return builder.build();
    }

    private boolean isAllowed(String value, Collection<String> allowList, Collection<String> denyList) {
        return (allowList.isEmpty() || allowList.contains(value))
                && (denyList.isEmpty() || !denyList.contains(value));
    }
}
