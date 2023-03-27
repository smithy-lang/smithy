package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ContextualValidationEventFormatterTest {
    @Test
    public void loadsContext() {
        Model model = Model.assembler()
                // Use the shared context file.
                .addImport(SourceContextLoader.class.getResource("context.smithy"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("example.smithy#Foo"));
        ValidationEvent event = ValidationEvent.builder()
                .id("foo")
                .severity(Severity.ERROR)
                .message("This is the message")
                .shape(shape)
                .build();

        String format = new ContextualValidationEventFormatter().format(event);
        // Normalize line endings for Windows.
        format = format.replace("\r\n", "\n");

        assertThat(format, startsWith("ERROR: example.smithy#Foo (foo)"));
        assertThat(format, containsString("\n     @ "));
        assertThat(format, endsWith(
                "\n     |"
                + "\n   3 | structure Foo {"
                + "\n     | ^"
                + "\n     = This is the message"
                + "\n"));
    }

    @Test
    public void doesNotLoadSourceLocationNone() {
        ValidationEvent event = ValidationEvent.builder()
                .id("foo")
                .severity(Severity.ERROR)
                .message("This is the message")
                .sourceLocation(SourceLocation.NONE)
                .build();

        String format = new ContextualValidationEventFormatter().format(event);
        // Normalize line endings for Windows.
        format = format.replace("\r\n", "\n");

        assertThat(format, equalTo(
                "ERROR: - (foo)"
                + "\n     = This is the message"
                + "\n"));
    }
}
