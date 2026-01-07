package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public interface ShapeValue extends ToNode, ToShapeId {

    default ShapeId eventShapeId() {
        return toShapeId();
    }

    default String context() {
        return "";
    }

    // TODO: Refactor/rename to something like "ShapeValueMetadata"
    default boolean hasFeature(NodeValidationVisitor.Feature feature) {
        return false;
    }

    default ValidationEvent constraintsEvent(String eventId, String message) {
        Severity severity = hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
        return event(eventId, severity, message);
    }

    default ValidationEvent event(String eventId, Severity severity, String message) {
        String context = context();
        return ValidationEvent.builder()
                .id(eventId)
                .shapeId(eventShapeId())
                .severity(severity)
                .sourceLocation(toNode().getSourceLocation())
                .message(context.isEmpty() ? message : context + ": " + message)
                .build();
    }
}
