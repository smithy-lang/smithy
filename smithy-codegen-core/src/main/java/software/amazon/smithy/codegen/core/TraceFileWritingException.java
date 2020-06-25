package software.amazon.smithy.codegen.core;

/**
 * Exception encountered during trace file writing.
 */
public class TraceFileWritingException extends RuntimeException{
    public TraceFileWritingException(String className, String nullVar) {
        super("Trace file or "+ className +" writing failed." + className + "'s " + nullVar + " is null or empty. " +
                nullVar + " is a required component of " + className + ".");
    }
}
