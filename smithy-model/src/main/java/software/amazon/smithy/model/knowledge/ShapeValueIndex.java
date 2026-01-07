package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ShapeValueIndex implements KnowledgeIndex {

    public static ShapeValueIndex of(Model model) {
        return model.getKnowledge(ShapeValueIndex.class, ShapeValueIndex::new);
    }

    private final Model model;
    private final Map<ShapeId, Set<ShapeValue>> shapeValues;

    public ShapeValueIndex(Model model) {
        this.model = model;
        this.shapeValues = new HashMap<>();

        model.shapes().forEach(shape -> {
            for (Trait trait : shape.getAllTraits().values()) {
                for (ShapeValue shapeValue : trait.shapeValues(model, shape)) {
                    addShapeValue(shapeValue);
                }
            }
        });
    }

    private void addShapeValue(ShapeValue shapeValue) {
        shapeValues.computeIfAbsent(shapeValue.toShapeId(), id -> new HashSet<>()).add(shapeValue);

        Node value = shapeValue.toNode();
        model.getShape(shapeValue.toShapeId()).ifPresent(shape -> {
            if (shape.isStructureShape() && value.isObjectNode()) {
                Map<String, MemberShape> members = shape.getAllMembers();

                for (Map.Entry<String, Node> entry : value.expectObjectNode().getStringMap().entrySet()) {
                    String entryKey = entry.getKey();
                    if (members.containsKey(entryKey)) {
                        ShapeValue childShapeValue = new SimpleShapeValue(shapeValue.eventShapeId(), members.get(entryKey).getTarget(), entry.getValue());
                        addShapeValue(childShapeValue);
                    }
                }
            } else if (shape.isListShape() && value.isArrayNode()) {
                ShapeId memberShapeId = shape.asListShape().get().getMember().getTarget();

                for (Node element : value.expectArrayNode().getElements()) {
                    ShapeValue childShapeValue = new SimpleShapeValue(memberShapeId, element);
                    addShapeValue(childShapeValue);
                }
            }
        });
    }

    public Set<ShapeValue> getShapeValues(ToShapeId toShapeId) {
        return shapeValues.getOrDefault(toShapeId.toShapeId(), Collections.emptySet());
    }
}
