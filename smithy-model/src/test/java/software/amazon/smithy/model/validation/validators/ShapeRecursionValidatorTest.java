/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ShapeRecursionValidatorTest {

    private static final String HEADER = "$version: \"2.0\"\nnamespace smithy.example\n\n";

    private static List<ValidationEvent> validate(String idl) {
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy", idl)
                .disableValidation()
                .assemble()
                .unwrap();
        return new ShapeRecursionValidator().validate(model);
    }

    private static void assertOneEventPerShape(List<ValidationEvent> events, int expectedShapes) {
        Set<ShapeId> ids = new HashSet<>();
        for (ValidationEvent event : events) {
            assertEquals(Severity.ERROR, event.getSeverity());
            assertEquals("ShapeRecursion", event.getId());
            assertTrue(event.getMessage().startsWith("Found invalid shape recursion: "));
            ids.add(event.getShapeId().get());
        }
        assertEquals(expectedShapes, events.size());
        assertEquals(expectedShapes, ids.size());
    }

    @Test
    public void reportsOneEventPerShapeForDiamondModels() {
        int n = 40;
        StringBuilder sb = new StringBuilder(HEADER);
        for (int i = 0; i < n; i++) {
            sb.append("structure S")
                    .append(i)
                    .append(" {\n")
                    .append("    @required\n    a: S")
                    .append(i + 1)
                    .append("\n")
                    .append("    @required\n    b: S")
                    .append(i + 1)
                    .append("\n")
                    .append("}\n");
        }
        sb.append("structure S").append(n).append(" {\n    @required\n    back: S0\n}\n");

        assertOneEventPerShape(validate(sb.toString()), n + 1);
    }

    @Test
    public void handlesDeepValidRequiredChains() {
        int n = 20000;
        StringBuilder sb = new StringBuilder(HEADER);
        for (int i = 0; i < n; i++) {
            sb.append("structure S")
                    .append(i)
                    .append(" {\n")
                    .append("    @required\n    a: S")
                    .append(i + 1)
                    .append("\n")
                    .append("}\n");
        }
        sb.append("structure S").append(n).append(" {\n    @required\n    tail: String\n}\n");

        assertEquals(0, validate(sb.toString()).size());
    }

    @Test
    public void reportsOneEventPerShapeForStarModels() {
        int n = 50;
        StringBuilder sb = new StringBuilder(HEADER);
        sb.append("structure H {\n");
        for (int i = 0; i < n; i++) {
            sb.append("    @required\n    m").append(i).append(": S").append(i).append("\n");
        }
        sb.append("}\n");
        for (int i = 0; i < n; i++) {
            sb.append("structure S").append(i).append(" {\n    @required\n    h: H\n}\n");
        }

        assertOneEventPerShape(validate(sb.toString()), n + 1);
    }

    @Test
    public void pathsEndAtTheSubjectShape() {
        String idl = HEADER
                + "structure A {\n    @required\n    b: B\n}\n"
                + "structure B {\n    @required\n    c: C\n    @required\n    a: A\n}\n"
                + "structure C {\n    @required\n    a: A\n}\n"
                + "list L {\n    member: M\n}\n"
                + "map M {\n    key: String\n    value: L\n}\n";
        List<ValidationEvent> events = validate(idl);

        assertEquals(5, events.size());
        for (ValidationEvent event : events) {
            String message = event.getMessage();
            String path = message.substring("Found invalid shape recursion: ".length(), message.indexOf(". "));
            assertTrue(path.startsWith(event.getShapeId().get() + "$"), message);
            assertTrue(path.endsWith(" > " + event.getShapeId().get()), message);
        }
        // B's shortest recursion path is B$a > A > A$b > B, not the longer one through C.
        ValidationEvent b = events.stream()
                .filter(e -> e.getShapeId().get().equals(ShapeId.from("smithy.example#B")))
                .findFirst()
                .get();
        assertTrue(b.getMessage()
                .startsWith("Found invalid shape recursion: smithy.example#B$a > smithy.example#A > "
                        + "smithy.example#A$b > smithy.example#B. "),
                b.getMessage());
    }
}
