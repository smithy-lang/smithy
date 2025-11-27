package software.amazon.smithy.jmespath.evaluation;

import java.math.BigDecimal;
import java.math.BigInteger;

public class EvaluationUtils {
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
            return (BigDecimal)number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger)number);
        } else if (number instanceof Integer || number instanceof Long
                || number instanceof Byte
                || number instanceof Short) {
            return BigDecimal.valueOf(number.longValue());
        } else {
            return BigDecimal.valueOf(number.doubleValue());
        }
    }
}
