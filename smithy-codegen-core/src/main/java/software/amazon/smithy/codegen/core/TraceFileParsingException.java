package software.amazon.smithy.codegen.core;

/**
 * Exception encountered during trace file parsing.
 */
public class TraceFileParsingException extends RuntimeException {
    public TraceFileParsingException(String className, String varText) {
        super("Trace file or "+ className + " parsing failed." + className + "'s " + varText + " node is missing or incorrectly" +
                "formatted, make sure " + className + " has a child called\"" + varText + "\"");
    }
}
