package software.amazon.smithy.codegen.core;

/**
 * Exception encountered during trace file deserialization.
 */
public class TraceFileParsingException extends RuntimeException {
    public TraceFileParsingException(String className, String varText) {
        super(className + "'s " + varText + " node is missing or incorrectly" +
                "formatted, make sure ArtifactMetadata has a child called\"" + varText + "\"");
    }
}
