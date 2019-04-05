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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.ValidatorService;

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
    private static final String DISCLAIMER = "Paginating operations that can return potentially unbounded lists "
                                             + "of data helps to maintain a predictable SLA and helps to prevent "
                                             + "operational issues in the future.";
    private static final Set<String> DEFAULT_VERBS_REQUIRE = Set.of("list", "search");
    private static final Set<String> DEFAULT_VERBS_SUGGEST = Set.of("describe", "get");
    private static final Set<String> DEFAULT_INPUT_MEMBERS = Set.of(
            "maxresults", "pagesize", "limit", "nexttoken", "pagetoken", "token");
    private static final Set<String> DEFAULT_OUTPUT_MEMBERS = Set.of(
            "nexttoken", "pagetoken", "token", "marker", "nextpage");

    private final Set<String> verbsRequirePagination;
    private final Set<String> verbsSuggestPagination;
    private final Set<String> inputMembersRequirePagination;
    private final Set<String> outputMembersRequirePagination;

    private MissingPaginatedTraitValidator(
            Set<String> verbsRequirePagination,
            Set<String> verbsSuggestPagination,
            Set<String> inputMembersRequirePagination,
            Set<String> outputMembersRequirePagination
    ) {
        this.verbsRequirePagination = verbsRequirePagination;
        this.verbsSuggestPagination = verbsSuggestPagination;
        this.inputMembersRequirePagination = inputMembersRequirePagination;
        this.outputMembersRequirePagination = outputMembersRequirePagination;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(MissingPaginatedTraitValidator.class, node -> {
                var verbsRequirePagination = parseSetOfString(node, "verbsRequirePagination", DEFAULT_VERBS_REQUIRE);
                var verbsSuggestPagination = parseSetOfString(node, "verbsSuggestPagination", DEFAULT_VERBS_SUGGEST);
                var inputMembersRequirePagination = parseSetOfString(
                        node, "inputMembersRequirePagination", DEFAULT_INPUT_MEMBERS);
                var outputMembersRequirePagination = parseSetOfString(
                        node, "outputMembersRequirePagination", DEFAULT_OUTPUT_MEMBERS);
                return new MissingPaginatedTraitValidator(
                        verbsRequirePagination, verbsSuggestPagination,
                        inputMembersRequirePagination, outputMembersRequirePagination);
            });
        }
    }

    private static Set<String> parseSetOfString(ObjectNode node, String member, Set<String> defaults) {
        return node.getArrayMember(member)
                .map(array -> array.getElements().stream()
                        .map(Node::expectStringNode)
                        .map(StringNode::getValue)
                        .map(string -> string.toLowerCase(Locale.US))
                        .collect(Collectors.toSet()))
                .orElse(defaults);
    }

    private static Optional<String> findMember(Collection<String> haystack, Collection<String> needles) {
        return haystack.stream()
                .filter(member -> needles.contains(member.toLowerCase(Locale.US)))
                .findFirst();
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        var operationIndex = model.getKnowledge(OperationIndex.class);
        return model.getShapeIndex().shapes(OperationShape.class)
                .filter(shape -> shape.getTrait(PaginatedTrait.class).isEmpty())
                .flatMap(shape -> validateShape(model.getShapeIndex(), operationIndex, shape))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateShape(
            ShapeIndex index,
            OperationIndex operationIndex,
            OperationShape operation
    ) {
        var words = ValidationUtils.splitCamelCaseWord(operation.getId().getName());
        var verb = words.get(0).toLowerCase(Locale.US);

        // The presence of "verbsRequirePagination" immediately qualifies the operation as needing `paginated`.
        if (verbsRequirePagination.contains(verb)) {
            return Stream.of(danger(operation, format(
                    "The verb of this operation, `%s`, requires that the operation is marked with the "
                    + "`paginated` trait. %s", verb, DISCLAIMER)));
        }

        if (operationIndex.getInput(operation.getId()).isPresent()) {
            StructureShape input = operationIndex.getInput(operation.getId()).get();
            Optional<String> member = findMember(
                    input.getAllMembers().keySet(), inputMembersRequirePagination);
            if (member.isPresent()) {
                return Stream.of(danger(operation, format(
                        "This operation contains an input member, `%s`, that requires that the operation is "
                        + "marked with the `paginated` trait. %s", member.get(), DISCLAIMER)));
            }
        }

        if (operationIndex.getOutput(operation.getId()).isPresent()) {
            StructureShape output = operationIndex.getOutput(operation.getId()).get();
            return findMember(output.getAllMembers().keySet(), outputMembersRequirePagination)
                    .map(member -> Stream.of(danger(operation, format(
                            "This operation contains an output member, `%s`, that requires that the "
                            + "operation is marked with the `paginated` trait. %s", member, DISCLAIMER))))
                    .orElseGet(() -> suggestPagination(verb, operation, output, index));
        }

        return Stream.empty();
    }

    private Stream<ValidationEvent> suggestPagination(
            String verb,
            OperationShape operation,
            StructureShape output,
            ShapeIndex index
    ) {
        if (!verbsSuggestPagination.contains(verb)) {
            return Stream.empty();
        }

        // We matched a verb, but only suggest pagination if there's a top-level output member that's a list.
        var hasListMember = output.getAllMembers().values().stream()
                .map(MemberShape::getTarget)
                .flatMap(id -> index.getShape(id).stream())
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
