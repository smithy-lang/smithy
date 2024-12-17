/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.Locale;
import software.amazon.smithy.jmespath.ast.ComparatorType;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

public enum RuntimeType {

    STRING {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            }
            switch (comparator) {
                case EQUAL:
                    return new LiteralExpression(left.expectStringValue().equals(right.expectStringValue()));
                case NOT_EQUAL:
                    return new LiteralExpression(!left.expectStringValue().equals(right.expectStringValue()));
                default:
                    return LiteralExpression.NULL;
            }
        }
    },

    NUMBER {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            }
            double comparison = left.expectNumberValue().doubleValue() - right.expectNumberValue().doubleValue();
            switch (comparator) {
                case EQUAL:
                    return new LiteralExpression(comparison == 0);
                case NOT_EQUAL:
                    return new LiteralExpression(comparison != 0);
                case GREATER_THAN:
                    return new LiteralExpression(comparison > 0);
                case GREATER_THAN_EQUAL:
                    return new LiteralExpression(comparison >= 0);
                case LESS_THAN:
                    return new LiteralExpression(comparison < 0);
                case LESS_THAN_EQUAL:
                    return new LiteralExpression(comparison <= 0);
                default:
                    throw new IllegalArgumentException("Unreachable comparator " + comparator);
            }
        }
    },

    BOOLEAN {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            }
            switch (comparator) {
                case EQUAL:
                    return new LiteralExpression(left.expectBooleanValue() == right.expectBooleanValue());
                case NOT_EQUAL:
                    return new LiteralExpression(left.expectBooleanValue() != right.expectBooleanValue());
                default:
                    return LiteralExpression.NULL;
            }
        }
    },

    NULL {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            }
            switch (comparator) {
                case EQUAL:
                    return new LiteralExpression(true);
                case NOT_EQUAL:
                    return new LiteralExpression(false);
                default:
                    return LiteralExpression.NULL;
            }
        }
    },

    ARRAY {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            }
            switch (comparator) {
                case EQUAL:
                    return new LiteralExpression(left.expectArrayValue().equals(right.expectArrayValue()));
                case NOT_EQUAL:
                    return new LiteralExpression(!left.expectArrayValue().equals(right.expectArrayValue()));
                default:
                    return LiteralExpression.NULL;
            }
        }
    },

    OBJECT {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            }
            switch (comparator) {
                case EQUAL:
                    return new LiteralExpression(left.expectObjectValue().equals(right.expectObjectValue()));
                case NOT_EQUAL:
                    return new LiteralExpression(!left.expectObjectValue().equals(right.expectObjectValue()));
                default:
                    return LiteralExpression.NULL;
            }
        }
    },

    EXPRESSION {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            if (left.getType() != right.getType()) {
                return LiteralExpression.BOOLEAN;
            } else {
                return LiteralExpression.NULL;
            }
        }
    },

    ANY {
        @Override
        public LiteralExpression compare(LiteralExpression left, LiteralExpression right, ComparatorType comparator) {
            // Just assume any kind of ANY comparison is satisfied.
            return new LiteralExpression(true);
        }
    };

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    public abstract LiteralExpression compare(
            LiteralExpression left,
            LiteralExpression right,
            ComparatorType comparator
    );
}
