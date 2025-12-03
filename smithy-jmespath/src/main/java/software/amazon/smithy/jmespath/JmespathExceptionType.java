package software.amazon.smithy.jmespath;

public enum JmespathExceptionType {
    SYNTAX("syntax"),

    INVALID_TYPE("invalid-type"),

    /**
     * An error occurred while evaluating the expression.
     */
    INVALID_VALUE("invalid-value"),

    /**
     * An error occurred while linting the expression.
     */
    UNKNOWN_FUNCTION("unknown-function"),

    INVALID_ARITY("invalid-arity"),

    OTHER("other");

    JmespathExceptionType(String id) {
    }

    public static JmespathExceptionType fromID(String id) {
        return valueOf(id.toUpperCase().replace('-', '_'));
    }
}
