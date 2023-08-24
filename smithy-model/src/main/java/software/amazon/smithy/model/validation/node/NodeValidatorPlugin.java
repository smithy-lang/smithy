/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies pluggable validation when validating {@link Node} values against
 * the schema of a {@link Shape} (e.g., when validating that the values
 * provided for a trait in the model are valid for the shape of the trait).
 */
@SmithyInternalApi
public interface NodeValidatorPlugin {
     String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Applies the plugin to the given shape, node value, and model.
     *
     * @param shape Shape being checked.
     * @param value Value being evaluated.
     * @param context Evaluation context.
     * @param emitter Consumer to notify of validation event locations and messages.
     */
    void apply(Shape shape, Node value, Context context, Emitter emitter);

    /**
     * @return Gets the built-in Node validation plugins.
     */
    static List<NodeValidatorPlugin> getBuiltins() {
        return ListUtils.of(
                new NonNumericFloatValuesPlugin(),
                new BlobLengthPlugin(),
                new CollectionLengthPlugin(),
                new IdRefPlugin(),
                new MapLengthPlugin(),
                new PatternTraitPlugin(),
                new RangeTraitPlugin(),
                new StringEnumPlugin(),
                new StringLengthPlugin());
    }

    /**
     * Validation context to pass to each NodeValidatorPlugin.
     */
    @SmithyInternalApi
    final class Context {
        private final Model model;
        private final Set<NodeValidationVisitor.Feature> features;
        private MemberShape referringMember;

        // Use an LRU cache to ensure the Selector cache doesn't grow too large
        // when given bad inputs.
        private final Map<Selector, Set<Shape>> selectorResults = new LinkedHashMap<Selector, Set<Shape>>(
                50 + 1, .75F, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry<Selector, Set<Shape>> eldest) {
                return size() > 50;
            }
        };

        /**
         * @param model Model being evaluated.
         */
        public Context(Model model) {
            this(model, Collections.emptySet());
        }

        public Context(Model model, Set<NodeValidationVisitor.Feature> features) {
            this.model = model;
            this.features = features;
        }

        /**
         * Get the model being evaluated.
         *
         * @return Returns the model.
         */
        public Model model() {
            return model;
        }

        /**
         * Select and memoize shapes from the model using a Selector.
         *
         * <p>The cache used by this method is not thread-safe, though that's
         * fine because NodeValidatorPlugins using the same Context all run
         * within the same thread.
         *
         * @param selector Selector to evaluate.
         * @return Returns the matching shapes.
         */
        public Set<Shape> select(Selector selector) {
            return selectorResults.computeIfAbsent(selector, s -> s.select(model));
        }

        public boolean hasFeature(NodeValidationVisitor.Feature feature) {
            return features.contains(feature);
        }

        public void setReferringMember(MemberShape referringMember) {
            this.referringMember = referringMember;
        }

        public Optional<MemberShape> getReferringMember() {
            return Optional.ofNullable(referringMember);
        }
    }

    @SmithyInternalApi
    @FunctionalInterface
    interface Emitter {
        void accept(FromSourceLocation sourceLocation,
                    Severity severity,
                    String message,
                    String... additionalEventIdParts);

        default void accept(FromSourceLocation sourceLocation, String message) {
            accept(sourceLocation, Severity.ERROR, message, EMPTY_STRING_ARRAY);
        }

        default void accept(FromSourceLocation sourceLocation, Severity severity, String message) {
            accept(sourceLocation, severity, message, EMPTY_STRING_ARRAY);
        }
    }
}
