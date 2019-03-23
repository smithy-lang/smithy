package software.amazon.smithy.openapi;

public class OpenApiException extends RuntimeException {
    public OpenApiException(RuntimeException e) {
        super(e);
    }

    public OpenApiException(String message) {
        super(message);
    }

    public OpenApiException(String message, Throwable previous) {
        super(message, previous);
    }
}
