/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.math.BigDecimal;
import java.util.Collection;
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
    AttributeComparator GT = numericComparator(result -> result == 1);
    AttributeComparator GTE = numericComparator(result -> result >= 0);
    AttributeComparator LT = numericComparator(result -> result <= -1);
    AttributeComparator LTE = numericComparator(result -> result <= 0);
    AttributeComparator EXISTS = AttributeComparator::existsCheck;
    AttributeComparator SUBSET = AttributeComparator::subset;
    AttributeComparator PROPER_SUBSET = AttributeComparator::properSubset;
    AttributeComparator PROJECTION_EQUALS = AttributeComparator::setEquals;
    AttributeComparator PROJECTION_NOT_EQUALS = AttributeComparator::setNotEquals;

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
     * <p>This method is necessary in order to support matching on projections
     * using projection semantics.
     *
     * @param singleComparison Comparison to apply to each element.
     * @return Returns the created comparator.
     */
    static AttributeComparator flattenedCompare(AttributeComparator singleComparison) {
        return (lhs, rhs, caseInsensitive) -> {
            for (AttributeValue l : lhs.getFlattenedValues()) {
                for (AttributeValue r : rhs.getFlattenedValues()) {
                    if (singleComparison.compare(l, r, caseInsensitive)) {
                        return true;
                    }
                }
            }

            return false;
        };
    }

    // String comparators simplify how comparisons are made on attribute
    // values that MUST resolve to strings.
    static AttributeComparator stringComparator(BiFunction<String, String, Boolean> compare) {
        return flattenedCompare((lhs, rhs, caseInsensitive) -> {
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
        });
    }

    // Try to parse both numbers, ignore numeric failures since that's acceptable,
    // then pass the result of calling compareTo on the numbers to the given
    // evaluator. The evaluator then determines if the comparison is what was expected.
    static AttributeComparator numericComparator(Function<Integer, Boolean> evaluator) {
        return stringComparator((lhs, rhs) -> {
            BigDecimal lhsNumber = parseNumber(lhs);
            if (lhsNumber == null) {
                return false;
            }

            BigDecimal rhsNumber = parseNumber(rhs);
            if (rhsNumber == null) {
                return false;
            }

            return evaluator.apply(lhsNumber.compareTo(rhsNumber));
        });
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

    static boolean areBothProjections(AttributeValue a, AttributeValue b) {
        return a instanceof AttributeValueImpl.Projection && b instanceof AttributeValueImpl.Projection;
    }

    static boolean subset(AttributeValue a, AttributeValue b, boolean caseInsensitive) {
        return areBothProjections(a, b)
                && isSubset(a.getFlattenedValues(), b.getFlattenedValues(), caseInsensitive);
    }

    // Note that projections with different sizes can still be subsets since
    // they operate like Sets<> where multiple instances of the same value
    // are treated as a single value.
    static boolean isSubset(
            Collection<? extends AttributeValue> aValues,
            Collection<? extends AttributeValue> bValues,
            boolean caseInsensitive
    ) {
        for (AttributeValue aValue : aValues) {
            boolean foundMatch = false;
            for (AttributeValue bValue : bValues) {
                if (EQUALS.compare(aValue, bValue, caseInsensitive)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }

        return true;
    }

    // {A} is a proper subset of {B} as long as {A} is a subset of {B},
    // and {B} is not a subset of {A}.
    static boolean properSubset(AttributeValue a, AttributeValue b, boolean caseInsensitive) {
        if (!areBothProjections(a, b)) {
            return false;
        }

        Collection<? extends AttributeValue> aValues = a.getFlattenedValues();
        Collection<? extends AttributeValue> bValues = b.getFlattenedValues();
        return isSubset(aValues, bValues, caseInsensitive)
                && !isSubset(bValues, aValues, caseInsensitive);
    }

    // {A} is equal to {B} if they are both subsets of one another.
    static boolean setEquals(AttributeValue a, AttributeValue b, boolean caseInsensitive) {
        if (!areBothProjections(a, b)) {
            return false;
        }

        Collection<? extends AttributeValue> aValues = a.getFlattenedValues();
        Collection<? extends AttributeValue> bValues = b.getFlattenedValues();
        return isSubset(aValues, bValues, caseInsensitive)
                && isSubset(bValues, aValues, caseInsensitive);
    }

    static boolean setNotEquals(AttributeValue a, AttributeValue b, boolean caseInsensitive) {
        return !setEquals(a, b, caseInsensitive);
    }
}
