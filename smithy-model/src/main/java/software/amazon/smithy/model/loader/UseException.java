package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;

/**
 * Throw when an error occurs with the IDL's {@code use} statements.
 */
public class UseException extends SourceException {
    public UseException(String message, FromSourceLocation sourceLocation) {
        super(message, sourceLocation.getSourceLocation());
    }
}
