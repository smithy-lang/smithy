package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public interface ShapeValue extends ToNode, ToShapeId {

    // TODO: Refactor/rename to something like "ShapeValueMetadata"
    default boolean hasFeature(NodeValidationVisitor.Feature feature) {
        return false;
    }

    // TODO: Probably doesn't belong here?
    default ValidationEvent constraintsEvent(String constraint) {
        Severity severity = hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
        return ValidationEvent.builder()
                .shapeId(toShapeId())
                .severity(severity)
                .sourceLocation(toNode().getSourceLocation())
                .message(constraint)
                .build();
    }
}
