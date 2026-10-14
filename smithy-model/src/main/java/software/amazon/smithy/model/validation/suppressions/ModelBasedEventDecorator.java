/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Creates a {@link ValidationEventDecorator} that applies custom suppressions, custom severity overrides,
 * suppressions parsed from model metadata, and severity overrides parsed from model metadata.
 *
 * <p>The created decorator implements {@link SuppressionUsage} and tracks each suppression it applies
 * so that warnings can be created for suppressions that matched no validation events.
 */
@SmithyUnstableApi
public final class ModelBasedEventDecorator {

    /**
     * The event ID used for warnings about suppressions that matched no validation events.
     */
    static final String UNUSED_SUPPRESSION_EVENT_ID = "UnusedSuppression";

    private static final String SUPPRESSIONS = "suppressions";
    private static final String SEVERITY_OVERRIDES = "severityOverrides";

    private final List<Suppression> suppressions = new ArrayList<>();
    private final List<SeverityOverride> severityOverrides = new ArrayList<>();

    /**
     * Sets custom suppressions not found in the model.
     *
     * @param suppressions Suppressions to set.
     * @return Returns the ModelBasedEventDecorator.
     */
    public ModelBasedEventDecorator suppressions(Collection<? extends Suppression> suppressions) {
        this.suppressions.clear();
        this.suppressions.addAll(suppressions);
        return this;
    }

    /**
     * Sets custom severity overrides not found in the model.
     *
     * @param severityOverrides Severity overrides to set.
     * @return Returns the ModelBasedEventDecorator.
     */
    public ModelBasedEventDecorator severityOverrides(Collection<? extends SeverityOverride> severityOverrides) {
        this.severityOverrides.clear();
        this.severityOverrides.addAll(severityOverrides);
        return this;
    }

    /**
     * Creates a ValidationEventDecorator for the given Model.
     *
     * <p>Validators, suppressions, and severity overrides found in the model are used each time
     * {@link ValidationEventDecorator#decorate(ValidationEvent)} is called. The
     * {@link ValidationEventDecorator#canDecorate(ValidationEvent)} always returns true.
     *
     * @param model Model to load validation events from.
     * @return Returns a decorator that can be used to modify the severity and suppression reason of each given event.
     */
    public ValidatedResult<ValidationEventDecorator> createDecorator(Model model) {
        // Create dedicated arrays to separate the state of the created decorator from the builder.
        List<ValidationEvent> events = new ArrayList<>();
        List<TrackedSuppression> loadedSuppressions = new ArrayList<>();
        for (Suppression suppression : suppressions) {
            loadedSuppressions.add(new TrackedSuppression(suppression));
        }
        loadMetadataSuppressions(model, loadedSuppressions, events);
        List<SeverityOverride> loadedSeverityOverrides = new ArrayList<>(severityOverrides);
        loadMetadataSeverityOverrides(model, loadedSeverityOverrides, events);

        // Load the suppress traits of the model up front so that each trait value can be tracked
        // across the entire validation run.
        Map<ShapeId, TraitSuppression> traitSuppressions = loadTraitSuppressions(model);

        // Tracks the suppress trait values that matched at least one event, by shape.
        Map<ShapeId, Set<String>> matchedTraitValues = new ConcurrentHashMap<>();

        // Modify severities and overrides of each encountered event.
        for (int i = 0; i < events.size(); i++) {
            events.set(i,
                    modifyEventSeverity(events.get(i),
                            loadedSuppressions,
                            traitSuppressions,
                            matchedTraitValues,
                            loadedSeverityOverrides));
        }

        return new ValidatedResult<>(new SuppressionUsage() {
            @Override
            public boolean canDecorate(ValidationEvent ev) {
                return true;
            }

            @Override
            public ValidationEvent decorate(ValidationEvent ev) {
                return modifyEventSeverity(ev,
                        loadedSuppressions,
                        traitSuppressions,
                        matchedTraitValues,
                        loadedSeverityOverrides);
            }

            @Override
            public List<ValidationEvent> createNoOpSuppressionWarnings() {
                return createUnusedSuppressionWarnings(traitSuppressions, matchedTraitValues, loadedSuppressions);
            }
        }, events);
    }

    private static void loadMetadataSeverityOverrides(
            Model model,
            List<SeverityOverride> severityOverrides,
            List<ValidationEvent> events
    ) {
        model.getMetadataProperty(SEVERITY_OVERRIDES).ifPresent(value -> {
            try {
                List<ObjectNode> values = value.expectArrayNode().getElementsAs(ObjectNode.class);
                for (ObjectNode rule : values) {
                    try {
                        severityOverrides.add(SeverityOverride.fromMetadata(rule));
                    } catch (SourceException e) {
                        events.add(ValidationEvent.fromSourceException(e));
                    }
                }
            } catch (SourceException e) {
                events.add(ValidationEvent.fromSourceException(e));
            }
        });
    }

    private static void loadMetadataSuppressions(
            Model model,
            List<TrackedSuppression> suppressions,
            List<ValidationEvent> events
    ) {
        model.getMetadataProperty(SUPPRESSIONS).ifPresent(value -> {
            try {
                List<ObjectNode> values = value.expectArrayNode().getElementsAs(ObjectNode.class);
                for (ObjectNode rule : values) {
                    try {
                        suppressions.add(new TrackedSuppression(Suppression.fromMetadata(rule)));
                    } catch (SourceException e) {
                        events.add(ValidationEvent.fromSourceException(e));
                    }
                }
            } catch (SourceException e) {
                events.add(ValidationEvent.fromSourceException(e));
            }
        });
    }

    private static Map<ShapeId, TraitSuppression> loadTraitSuppressions(Model model) {
        // A TreeMap is used to provide a deterministic order for no-op suppression warnings.
        Map<ShapeId, TraitSuppression> result = new TreeMap<>();
        for (Shape shape : model.getShapesWithTrait(SuppressTrait.ID)) {
            result.put(shape.getId(), new TraitSuppression(shape.getId(), shape.expectTrait(SuppressTrait.class)));
        }
        return result;
    }

    private static ValidationEvent modifyEventSeverity(
            ValidationEvent event,
            List<TrackedSuppression> suppressions,
            Map<ShapeId, TraitSuppression> traitSuppressions,
            Map<ShapeId, Set<String>> matchedTraitValues,
            List<SeverityOverride> severityOverrides
    ) {
        // ERROR and SUPPRESSED events cannot be suppressed.
        if (!event.getSeverity().canSuppress()) {
            return event;
        }

        // Use a suppress trait if present.
        if (event.getShapeId().isPresent()) {
            ShapeId target = event.getShapeId().get();
            TraitSuppression suppression = traitSuppressions.get(target);
            if (suppression != null) {
                Optional<String> matchingValue = suppression.matchingValue(event);
                if (matchingValue.isPresent()) {
                    matchedTraitValues
                            .computeIfAbsent(target, ignored -> ConcurrentHashMap.newKeySet())
                            .add(matchingValue.get());
                    return changeSeverity(event, Severity.SUPPRESSED, suppression.getReason().orElse(null));
                }
            }
        }

        // Check metadata and manual suppressions.
        for (TrackedSuppression suppression : suppressions) {
            if (suppression.test(event)) {
                return changeSeverity(event, Severity.SUPPRESSED, suppression.getReason().orElse(null));
            }
        }

        Severity appliedSeverity = event.getSeverity();
        for (SeverityOverride override : severityOverrides) {
            Severity overrideResult = override.apply(event);
            if (overrideResult.ordinal() > appliedSeverity.ordinal()) {
                appliedSeverity = overrideResult;
            }
        }

        return changeSeverity(event, appliedSeverity, null);
    }

    private static List<ValidationEvent> createUnusedSuppressionWarnings(
            Map<ShapeId, TraitSuppression> traitSuppressions,
            Map<ShapeId, Set<String>> matchedTraitValues,
            List<TrackedSuppression> suppressions
    ) {
        List<ValidationEvent> warnings = new ArrayList<>();

        // Create a warning for each suppress trait value that matched no validation events.
        for (Map.Entry<ShapeId, TraitSuppression> entry : traitSuppressions.entrySet()) {
            // Prelude shapes are skipped to mirror how non-error events for prelude shapes are
            // filtered out of validation events.
            if (Prelude.isPreludeShape(entry.getKey())) {
                continue;
            }

            ShapeId shapeId = entry.getKey();
            SuppressTrait trait = entry.getValue().getTrait();
            Set<String> matchedValues = matchedTraitValues.getOrDefault(shapeId, Collections.emptySet());
            for (String value : trait.getValues()) {
                if (!matchedValues.contains(value)) {
                    warnings.add(ValidationEvent.builder()
                            .id(UNUSED_SUPPRESSION_EVENT_ID)
                            .severity(Severity.WARNING)
                            .shapeId(shapeId)
                            .sourceLocation(trait)
                            .message("The `@suppress` trait value `" + value
                                    + "` did not match any validation events.")
                            .build());
                }
            }
        }

        // Create a warning for each metadata suppression that matched no validation events. Custom
        // suppressions are not tracked because they have no model-defined identity to name in a
        // warning.
        for (TrackedSuppression tracked : suppressions) {
            if (tracked.hasMatched() || !(tracked.getSuppression() instanceof MetadataSuppression)) {
                continue;
            }

            MetadataSuppression suppression = (MetadataSuppression) tracked.getSuppression();
            warnings.add(ValidationEvent.builder()
                    .id(UNUSED_SUPPRESSION_EVENT_ID)
                    .severity(Severity.WARNING)
                    .sourceLocation(suppression)
                    .message("The suppression with ID `" + suppression.getId() + "` in namespace `"
                            + suppression.getNamespace() + "` did not match any validation events.")
                    .build());
        }

        return warnings;
    }

    private static ValidationEvent changeSeverity(ValidationEvent event, Severity severity, String reason) {
        if (event.getSeverity() == severity) {
            return event;
        } else {
            // The event was suppressed so change the severity and reason.
            ValidationEvent.Builder builder = event.toBuilder();
            builder.severity(severity);
            if (reason != null) {
                builder.suppressionReason(reason);
            }
            return builder.build();
        }
    }
}
