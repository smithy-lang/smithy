/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.linters;

import static java.lang.String.format;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Checks if an operation should be paginated but is not.
 *
 * <ol>
 *     <li>Skip operations that are paginated</li>
 *     <li>Emit if an operation verb matches one of the
 *      {@code verbsRequirePagination} values</li>
 *     <li>Emit if an operation input member name matches one of the
 *       {@code inputMembersRequirePagination} values</li>
 *     <li>Emit if an operation output member name matches one of the
 *      {@code outputMembersRequirePagination} values</li>
 *     <li>Skip if the operation has no output</li>
 *     <li>Emit if the operation verb matches one of the
 *      {@code verbsSuggestPagination} and there's a top-level output
 *      member that targets a list.</li>
 * </ol>
 */
public final class MissingPaginatedTraitValidator extends AbstractValidator {

    private static final Set<String> DEFAULT_VERBS_REQUIRE = SetUtils.of("list", "search");
    private static final Set<String> DEFAULT_VERBS_SUGGEST = SetUtils.of("describe", "get");
    private static final Set<String> DEFAULT_INPUT_MEMBERS = SetUtils.of(
            "maxresults", "maxitems", "pagesize", "limit",
            "nexttoken", "pagetoken", "token", "marker");
    private static final Set<String> DEFAULT_OUTPUT_MEMBERS = SetUtils.of(
            "nexttoken", "pagetoken", "token", "marker", "nextpage", "nextpagetoken", "position", "nextmarker",
            "paginationtoken", "nextpagemarker");

    public static final class Config {
        private Set<String> verbsRequirePagination = DEFAULT_VERBS_REQUIRE;
        private Set<String> verbsSuggestPagination = DEFAULT_VERBS_SUGGEST;
        private Set<String> inputMembersRequirePagination = DEFAULT_INPUT_MEMBERS;
        private Set<String> outputMembersRequirePagination = DEFAULT_OUTPUT_MEMBERS;

        public Set<String> getVerbsRequirePagination() {
            return verbsRequirePagination;
        }

        /**
         * Sets the list of verbs that require pagination.
         *
         * @param verbsRequirePagination Operation verbs that require pagination.
         */
        public void setVerbsRequirePagination(Set<String> verbsRequirePagination) {
            this.verbsRequirePagination = lowercaseSet(verbsRequirePagination);
        }

        public Set<String> getVerbsSuggestPagination() {
            return verbsSuggestPagination;
        }

        /**
         * Sets the verbs that suggest the operation should be paginated.
         *
         * @param verbsSuggestPagination Operation verbs that suggest pagination.
         */
        public void setVerbsSuggestPagination(Set<String> verbsSuggestPagination) {
            this.verbsSuggestPagination = lowercaseSet(verbsSuggestPagination);
        }

        public Set<String> getInputMembersRequirePagination() {
            return inputMembersRequirePagination;
        }

        /**
         * Sets the input member names that indicate an operation requires pagination.
         *
         * @param inputMembersRequirePagination Input member names.
         */
        public void setInputMembersRequirePagination(Set<String> inputMembersRequirePagination) {
            this.inputMembersRequirePagination = lowercaseSet(inputMembersRequirePagination);
        }

        public Set<String> getOutputMembersRequirePagination() {
            return outputMembersRequirePagination;
        }

        /**
         * Sets the output member names that indicate the operation requires pagination.
         *
         * @param outputMembersRequirePagination Output member names.
         */
        public void setOutputMembersRequirePagination(Set<String> outputMembersRequirePagination) {
            this.outputMembersRequirePagination = lowercaseSet(outputMembersRequirePagination);
        }

        // Convert the entries in the set to lowercase to normalize set lookups and comparisons.
        private Set<String> lowercaseSet(Set<String> set) {
            Set<String> result = new HashSet<>(set.size());
            for (String entry : set) {
                result.add(entry.toLowerCase(Locale.ENGLISH));
            }
            return result;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(MissingPaginatedTraitValidator.class, node -> {
                Config config = new NodeMapper().deserialize(node, Config.class);
                return new MissingPaginatedTraitValidator(config);
            });
        }
    }

    private static final String DISCLAIMER = "Paginating operations that can return potentially unbounded lists "
                                             + "of data helps to maintain a predictable SLA and helps to prevent "
                                             + "operational issues in the future.";

    private final Config config;

    private MissingPaginatedTraitValidator(Config config) {
        this.config = config;
    }

    private static Optional<String> findMember(Collection<String> haystack, Collection<String> needles) {
        return haystack.stream()
                .filter(member -> needles.contains(member.toLowerCase(Locale.US)))
                .findFirst();
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);
        return model.shapes(OperationShape.class)
                .filter(shape -> !shape.getTrait(PaginatedTrait.class).isPresent())
                .flatMap(shape -> validateShape(model, operationIndex, shape))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateShape(
            Model model,
            OperationIndex operationIndex,
            OperationShape operation
    ) {
        List<String> words = ValidationUtils.splitCamelCaseWord(operation.getId().getName());
        String verb = words.get(0).toLowerCase(Locale.US);

        // The presence of "verbsRequirePagination" immediately qualifies the operation as needing `paginated`.
        if (config.getVerbsRequirePagination().contains(verb)) {
            return Stream.of(danger(operation, format(
                    "The verb of this operation, `%s`, requires that the operation is marked with the "
                    + "`paginated` trait. %s", verb, DISCLAIMER)));
        }

        StructureShape input = operationIndex.expectInputShape(operation.getId());
        Optional<String> member = findMember(
                input.getAllMembers().keySet(), config.getInputMembersRequirePagination());
        if (member.isPresent()) {
            return Stream.of(danger(operation, format(
                    "This operation contains an input member, `%s`, that requires that the operation is "
                    + "marked with the `paginated` trait. %s", member.get(), DISCLAIMER)));
        }

        StructureShape output = operationIndex.expectOutputShape(operation.getId());
        return findMember(output.getAllMembers().keySet(), config.getOutputMembersRequirePagination())
                .map(outputMember -> Stream.of(danger(operation, format(
                        "This operation contains an output member, `%s`, that requires that the "
                        + "operation is marked with the `paginated` trait. %s", outputMember, DISCLAIMER))))
                .orElseGet(() -> suggestPagination(verb, operation, output, model));
    }

    private Stream<ValidationEvent> suggestPagination(
            String verb,
            OperationShape operation,
            StructureShape output,
            Model model
    ) {
        if (!config.getVerbsSuggestPagination().contains(verb)) {
            return Stream.empty();
        }

        // We matched a verb, but only suggest pagination if there's a top-level output member that's a list.
        boolean hasListMember = output.getAllMembers().values().stream()
                .map(MemberShape::getTarget)
                .flatMap(id -> OptionalUtils.stream(model.getShape(id)))
                .anyMatch(Shape::isListShape);

        if (!hasListMember) {
            return Stream.empty();
        }

        return Stream.of(warning(operation, format(
                "The verb of this operation, `%s`, and the presence of a top-level list member in its "
                + "output, suggests that the operation should have the `paginated` trait. %s",
                verb, DISCLAIMER)));
    }
}
