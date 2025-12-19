package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    }

    public Set<ShapeValue> getShapeValues(ToShapeId toShapeId) {
        return shapeValues.getOrDefault(toShapeId.toShapeId(), Collections.emptySet());
    }
}
