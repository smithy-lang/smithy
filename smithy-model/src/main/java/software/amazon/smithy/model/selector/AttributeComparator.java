/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.selector;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Compares two selector attribute values.
 */
@FunctionalInterface
interface AttributeComparator {

    AttributeComparator EQUALS = stringComparator(String::equals);
    AttributeComparator NOT_EQUALS = stringComparator((a, b) -> !a.equals(b));
    AttributeComparator STARTS_WITH = stringComparator(String::startsWith);
    AttributeComparator ENDS_WITH = stringComparator(String::endsWith);
    AttributeComparator CONTAINS = stringComparator(String::contains);
    AttributeComparator GT = stringComparator((a, b) -> numericComparison(a, b, result -> result == 1));
    AttributeComparator GTE = stringComparator((a, b) -> numericComparison(a, b, result -> result >= 0));
    AttributeComparator LT = stringComparator((a, b) -> numericComparison(a, b, result -> result <= -1));
    AttributeComparator LTE = stringComparator((a, b) -> numericComparison(a, b, result -> result <= 0));
    AttributeComparator EXISTS = AttributeComparator::existsCheck;

    /**
     * Compares the left hand side value against the right using a comparator.
     *
     * @param lhs Left value of the comparison.
     * @param rhs Right value of the comparison.
     * @param caseInsensitive Whether or not the comparison is case-insensitive.
     * @return Returns true if the values match the comparator.
     */
    boolean compare(AttributeValue lhs, AttributeValue rhs, boolean caseInsensitive);

    /**
     * Compares the given attribute values by flattening each side of the
     * comparison, and comparing each value.
     *
     * <p>This method is necessary in order to support matching on projections.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param insensitive Whether or not to use a case-insensitive comparison.
     * @return Returns true if the attributes match the comparator.
     */
    default boolean flattenedCompare(AttributeValue lhs, AttributeValue rhs, boolean insensitive) {
        for (AttributeValue l : lhs.getFlattenedValues()) {
            for (AttributeValue r : rhs.getFlattenedValues()) {
                if (compare(l, r, insensitive)) {
                    return true;
                }
            }
        }

        return false;
    }

    // String comparators simplify how comparisons are made on attribute
    // values that MUST resolve to strings.
    static AttributeComparator stringComparator(BiFunction<String, String, Boolean> compare) {
        return (lhs, rhs, caseInsensitive) -> {
            // Both values MUST be present to compare.
            if (!lhs.isPresent() || !rhs.isPresent()) {
                return false;
            }

            String lhsString = lhs.toString();
            String rhsString = rhs.toString();

            // Convert both sides of the comparison to lowercase when case insensitive.
            if (caseInsensitive) {
                lhsString = lhsString.toLowerCase(Locale.ENGLISH);
                rhsString = rhsString.toLowerCase(Locale.ENGLISH);
            }

            return compare.apply(lhsString, rhsString);
        };
    }

    // Try to parse both numbers, ignore numeric failures since that's acceptable,
    // then pass the result of calling compareTo on the numbers to the given
    // evaluator. The evaluator then determines if the comparison is what was expected.
    static boolean numericComparison(String lhs, String rhs, Function<Integer, Boolean> evaluator) {
        BigDecimal lhsNumber = parseNumber(lhs);
        if (lhsNumber == null) {
            return false;
        }

        BigDecimal rhsNumber = parseNumber(rhs);
        if (rhsNumber == null) {
            return false;
        }

        return evaluator.apply(lhsNumber.compareTo(rhsNumber));
    }

    // Invalid numbers do not fail the parser or evaluation of a selector.
    static BigDecimal parseNumber(String token) {
        try {
            return new BigDecimal(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Checks if a value "exists" and if the expected boolean string matches
    // the resolved existence boolean.
    static boolean existsCheck(AttributeValue a, AttributeValue b, boolean caseInsensitive) {
        String bString = b.toString();
        return (a.isPresent() && bString.equals("true"))
               || (!a.isPresent() && b.toString().equals("false"));
    }
}
