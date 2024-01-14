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

package software.amazon.smithy.model.validation;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.validators.DefaultTraitValidator;
import software.amazon.smithy.model.validation.validators.EnumShapeValidator;
import software.amazon.smithy.model.validation.validators.ExclusiveStructureMemberTraitValidator;
import software.amazon.smithy.model.validation.validators.ResourceCycleValidator;
import software.amazon.smithy.model.validation.validators.ShapeIdConflictValidator;
import software.amazon.smithy.model.validation.validators.SingleOperationBindingValidator;
import software.amazon.smithy.model.validation.validators.SingleResourceBindingValidator;
import software.amazon.smithy.model.validation.validators.TargetValidator;
import software.amazon.smithy.model.validation.validators.TraitConflictValidator;
import software.amazon.smithy.model.validation.validators.TraitTargetValidator;
import software.amazon.smithy.model.validation.validators.TraitValueValidator;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Utility methods used when validating.
 */
public final class ValidationUtils {

    // Critical validators must emit validation events with names that match their class name (Name in NameValidator).
    private static final Set<Class<? extends Validator>> CRITICAL_VALIDATORS = SetUtils.of(
            // These validators should run before even any other validators. These validators are considered
            // correctness validators that ensure relationships are valid and to shapes that can be found and that
            // the model has no cycles.
            TargetValidator.class,
            ResourceCycleValidator.class,

            // This validator is considered critical because it ensures an enum shape itself is valid.
            EnumShapeValidator.class,

            // Ensure that traits are valid, according to the model, before other validators are run.
            ExclusiveStructureMemberTraitValidator.class,
            TraitTargetValidator.class,
            TraitValueValidator.class,
            TraitConflictValidator.class,

            // Defaults need to be valid.
            DefaultTraitValidator.class,

            // Ensure the semantic model adheres to the requirements of shape IDs and service shape uniqueness.
            ShapeIdConflictValidator.class,
            SingleOperationBindingValidator.class,
            SingleResourceBindingValidator.class
    );

    private static final Pattern CAMEL_WORD_SPLITTER = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

    private ValidationUtils() {}

    /**
     * Checks if the given validator class is a critical validator that should be applied before other downstream
     * validators are applied.
     *
     * @param validator Validator class to check.
     * @return Returns true if the given validator is considered a critical validator.
     */
    @SmithyInternalApi
    public static boolean isCriticalValidator(Class<? extends Validator> validator) {
        return CRITICAL_VALIDATORS.contains(validator);
    }

    /**
     * Checks if the given validation event was emitted by a critical validator.
     *
     * @param eventId Event ID to check if it's a critical validation event.
     * @return Returns true if the given event was emitted from a critical validator.
     */
    @SmithyInternalApi
    public static boolean isCriticalEvent(String eventId) {
        int slice = eventId.indexOf(".");
        if (slice > 0) {
            eventId = eventId.substring(0, slice);
        }

        for (Class<?> c : CRITICAL_VALIDATORS) {
            if (c.getSimpleName().replace("Validator", "").equals(eventId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Splits a camelCase word into a list of words.
     *
     * @param word Word to split.
     * @return Returns the split words.
     */
    public static List<String> splitCamelCaseWord(String word) {
        return Arrays.asList(CAMEL_WORD_SPLITTER.split(word));
    }

    /**
     * Creates a comma separated string made up of the given collection.
     * Each value is surrounded by "`", and the values are sorted to make it
     * easier to write tests against the messages.
     *
     * @param values Values to turn into a comma separated list.
     * @return Returns the string.
     */
    public static String orderedTickedList(Collection<?> values) {
        if (values.size() == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner("`, `", "`", "`");
        for (Object value : values) {
            joiner.add(value.toString());
        }
        return joiner.toString();
    }

    /**
     * Creates a comma separated string made up of the given collection.
     * Each value is surrounded by "`", and the values are sorted to make it
     * easier to write tests against the messages.
     *
     * @param values Values to turn into a comma separated list.
     * @return Returns the string.
     */
    public static String tickedList(Collection<?> values) {
        List<?> valueList = new ArrayList<>(values);
        valueList.sort(Comparator.comparing(Object::toString));
        return orderedTickedList(valueList);
    }

    public static String tickedList(Stream<?> values) {
        return values.map(Object::toString).sorted().collect(Collectors.joining("`, `", "`", "`"));
    }

    @Deprecated
    public static <T extends ToShapeId> Map<String, List<ShapeId>> findDuplicateShapeNames(Collection<T> shapes) {
        return shapes.stream()
                .map(ToShapeId::toShapeId)
                // Exclude IDs with members since these need to be validated separately.
                .filter(id -> !id.hasMember())
                // Group by the lowercase name of each shape, and collect the shape IDs as strings.
                .collect(groupingBy(id -> id.getName().toLowerCase(Locale.US)))
                .entrySet().stream()
                // Only keep entries that have duplicates.
                .filter(entry -> entry.getValue().size() > 1)
                // Sort by the member name and collect into an ordered map to preserve sort order.
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(toMap(Map.Entry::getKey, entry -> {
                    // Sort the shape IDs.
                    entry.getValue().sort(Comparator.comparing(ShapeId::toString));
                    return entry.getValue();
                }, (a, b) -> b, LinkedHashMap::new));
    }
}
