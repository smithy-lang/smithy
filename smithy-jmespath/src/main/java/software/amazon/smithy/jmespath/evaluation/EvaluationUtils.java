/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Objects;
import software.amazon.smithy.jmespath.RuntimeType;

public final class EvaluationUtils {

    private static final InheritingClassMap<NumberType> numberTypeForClass = InheritingClassMap.<NumberType>builder()
            .put(Byte.class, NumberType.BYTE)
            .put(Short.class, NumberType.SHORT)
            .put(Integer.class, NumberType.INTEGER)
            .put(Long.class, NumberType.LONG)
            .put(Float.class, NumberType.FLOAT)
            .put(Double.class, NumberType.DOUBLE)
            .put(BigInteger.class, NumberType.BIG_INTEGER)
            .put(BigDecimal.class, NumberType.BIG_DECIMAL)
            .build();

    public static NumberType numberType(Number number) {
        return numberTypeForClass.get(number.getClass());
    }

    // Emulate JLS 5.1.2 type promotion.
    static int compareNumbersWithPromotion(Number a, Number b) {
        // Exact matches.
        if (a.equals(b)) {
            return 0;
        } else if (isBig(a, b)) {
            // When the values have a BigDecimal or BigInteger, normalize them both to BigDecimal. This is used even
            // for BigInteger to avoid dropping decimals from doubles or floats (e.g., 10.01 != 10).
            return toBigDecimal(a)
                    .stripTrailingZeros()
                    .compareTo(toBigDecimal(b).stripTrailingZeros());
        } else if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float) {
            // Treat floats as double to allow for comparing larger values from rhs, like longs.
            return Double.compare(a.doubleValue(), b.doubleValue());
        } else {
            return Long.compare(a.longValue(), b.longValue());
        }
    }

    private static boolean isBig(Number a, Number b) {
        return a instanceof BigDecimal || b instanceof BigDecimal
                || a instanceof BigInteger
                || b instanceof BigInteger;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Integer || number instanceof Long
                || number instanceof Byte
                || number instanceof Short) {
            return BigDecimal.valueOf(number.longValue());
        } else {
            return BigDecimal.valueOf(number.doubleValue());
        }
    }

    public static Number addNumbers(Number a, Number b) {
        if (isBig(a, b)) {
            return toBigDecimal(a).add(toBigDecimal(b));
        } else if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float) {
            return a.doubleValue() + b.doubleValue();
        } else {
            return Math.addExact(a.longValue(), b.longValue());
        }
    }

    public static Number divideNumbers(Number a, Number b) {
        if (isBig(a, b)) {
            return toBigDecimal(a).divide(toBigDecimal(b));
        } else {
            return a.doubleValue() / b.doubleValue();
        }
    }

    public static int codePointCount(String string) {
        return string.codePointCount(0, string.length());
    }

    public static <T> boolean equals(JmespathRuntime<T> runtime, T a, T b) {
        switch (runtime.typeOf(a)) {
            case NULL:
            case STRING:
            case BOOLEAN:
                return Objects.equals(a, b);
            case NUMBER:
                if (!runtime.is(b, RuntimeType.NUMBER)) {
                    return false;
                }
                return runtime.compare(a, b) == 0;
            case ARRAY:
                if (!runtime.is(b, RuntimeType.ARRAY)) {
                    return false;
                }
                Iterator<? extends T> aIter = runtime.asIterable(a).iterator();
                Iterator<? extends T> bIter = runtime.asIterable(b).iterator();
                while (aIter.hasNext()) {
                    if (!bIter.hasNext()) {
                        return false;
                    }
                    if (!runtime.equal(aIter.next(), bIter.next())) {
                        return false;
                    }
                }
                return !bIter.hasNext();
            case OBJECT:
                if (!runtime.is(b, RuntimeType.OBJECT)) {
                    return false;
                }
                if (!runtime.length(a).equals(runtime.length(b))) {
                    return false;
                }
                for (T key : runtime.asIterable(a)) {
                    T aValue = runtime.value(a, key);
                    T bValue = runtime.value(b, key);
                    if (!runtime.equal(aValue, bValue)) {
                        return false;
                    }
                }
                return true;
            default:
                throw new IllegalStateException();
        }
    }
}
