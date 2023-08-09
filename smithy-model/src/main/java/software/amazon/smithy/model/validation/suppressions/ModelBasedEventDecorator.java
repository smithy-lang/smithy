/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.validation.suppressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
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
 */
@SmithyUnstableApi
public final class ModelBasedEventDecorator {

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
        List<Suppression> loadedSuppressions = new ArrayList<>(suppressions);
        loadMetadataSuppressions(model, loadedSuppressions, events);
        List<SeverityOverride> loadedSeverityOverrides = new ArrayList<>(severityOverrides);
        loadMetadataSeverityOverrides(model, loadedSeverityOverrides, events);

        // Modify severities and overrides of each encountered event.
        for (int i = 0; i < events.size(); i++) {
            events.set(i, modifyEventSeverity(model, events.get(i), loadedSuppressions, loadedSeverityOverrides));
        }

        return new ValidatedResult<>(new ValidationEventDecorator() {
            @Override
            public boolean canDecorate(ValidationEvent ev) {
                return true;
            }

            @Override
            public ValidationEvent decorate(ValidationEvent ev) {
                return modifyEventSeverity(model, ev, loadedSuppressions, loadedSeverityOverrides);
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
            List<Suppression> suppressions,
            List<ValidationEvent> events
    ) {
        model.getMetadataProperty(SUPPRESSIONS).ifPresent(value -> {
            try {
                List<ObjectNode> values = value.expectArrayNode().getElementsAs(ObjectNode.class);
                for (ObjectNode rule : values) {
                    try {
                        suppressions.add(Suppression.fromMetadata(rule));
                    } catch (SourceException e) {
                        events.add(ValidationEvent.fromSourceException(e));
                    }
                }
            } catch (SourceException e) {
                events.add(ValidationEvent.fromSourceException(e));
            }
        });
    }

    private static ValidationEvent modifyEventSeverity(
            Model model,
            ValidationEvent event,
            List<Suppression> suppressions,
            List<SeverityOverride> severityOverrides
    ) {
        // Use a suppress trait if present.
        if (event.getShapeId().isPresent()) {
            ShapeId target = event.getShapeId().get();
            Shape shape = model.getShape(target).orElse(null);
            if (shape != null) {
                if (shape.hasTrait(SuppressTrait.class)) {
                    Suppression suppression = Suppression.fromSuppressTrait(shape);
                    if (suppression.test(event)) {
                        return changeSeverity(event, Severity.SUPPRESSED, suppression.getReason().orElse(null));
                    }
                }
            }
        }

        // Check metadata and manual suppressions.
        for (Suppression suppression : suppressions) {
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
