package software.amazon.smithy.model.transform;


import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

/**
 * Mark Idempotency token members as clientOptional so they can be injected if missing.
 */
final class MarkIdempotencyTokenClientOptional {
    public static Model transform(Model model) {
        return ModelTransformer.create().mapShapes(model, shape -> {
            if (shape.isMemberShape()
                    && shape.hasTrait(RequiredTrait.class)
                    && shape.hasTrait(IdempotencyTokenTrait.class)
                    && !shape.hasTrait(ClientOptionalTrait.class)) {
                return Shape.shapeToBuilder(shape).addTrait(new ClientOptionalTrait()).build();
            }
            return shape;
        });
    }
}
