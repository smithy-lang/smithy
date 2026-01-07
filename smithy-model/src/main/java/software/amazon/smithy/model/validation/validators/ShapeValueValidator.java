package software.amazon.smithy.model.validation.validators;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.knowledge.ShapeValue;
import software.amazon.smithy.model.knowledge.ShapeValueIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.List;

public class ShapeValueValidator extends AbstractValidator {

    private NullableIndex nullableIndex;

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeValueIndex shapeValueIndex = ShapeValueIndex.of(model);
        nullableIndex = NullableIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();

        for (ShapeId shapeId : model.getShapeIds()) {
            Shape shape = model.expectShape(shapeId);
            for (ShapeValue value : shapeValueIndex.getShapeValues(shapeId)) {
                validateShapeValue(events, shape, value);
            }
        }

        return events;
    }

    private void validateShapeValue(List<ValidationEvent> events, Shape shape, ShapeValue shapeValue) {
        Node node = shapeValue.toNode();

        switch (shape.getType()) {
            case FLOAT:
                validateFloat(events, shapeValue, shape, node);
                break;
            case BOOLEAN:
                validateBoolean(events, shapeValue, shape, node);
                break;
            default:
                break;
        }
    }

    private void validateFloat(List<ValidationEvent> events, ShapeValue shapeValue, Shape shape, Node value) {
        if (value.isNumberNode()) {
            return;
        } else if (value.isStringNode()) {
            // TODO: validate string values
            return;
        } else {
            invalidShape(events, shapeValue, shape, NodeType.NUMBER, value);
        }
    }

    private void validateBoolean(List<ValidationEvent> events, ShapeValue shapeValue, Shape shape, Node value) {
        if (value.isBooleanNode()) {
            return;
        } else {
            invalidShape(events, shapeValue, shape, NodeType.NUMBER, value);
        }
    }

    private void invalidShape(List<ValidationEvent> events, ShapeValue shapeValue, Shape shape, NodeType expectedType, Node value) {
        // Nullable shapes allow null values.
        if (value.isNullNode() && shapeValue.hasFeature(NodeValidationVisitor.Feature.ALLOW_OPTIONAL_NULLS)) {
            // Non-members are nullable. Members are nullable based on context.
            if (!shape.isMemberShape() || shape.asMemberShape().filter(nullableIndex::isMemberNullable).isPresent()) {
                return;
            }
        }

        String message = String.format(
                "Expected %s value for %s shape, `%s`; found %s value",
                expectedType,
                shape.getType(),
                shape.getId(),
                value.getType());
        if (value.isStringNode()) {
            message += ", `" + value.expectStringNode().getValue() + "`";
        } else if (value.isNumberNode()) {
            message += ", `" + value.expectNumberNode().getValue() + "`";
        } else if (value.isBooleanNode()) {
            message += ", `" + value.expectBooleanNode().getValue() + "`";
        }
        events.add(error(shape, message, shapeValue.eventShapeId().toString()));
    }
}
