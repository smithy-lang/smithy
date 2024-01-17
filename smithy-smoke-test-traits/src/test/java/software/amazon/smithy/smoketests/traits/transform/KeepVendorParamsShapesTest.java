package software.amazon.smithy.smoketests.traits.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait;
import software.amazon.smithy.utils.ListUtils;

public class KeepVendorParamsShapesTest {
    @Test
    public void keepsOnlyVendorParams() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params-and-other-unused-shapes.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model transformed = transformer.removeUnreferencedShapes(model);
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams")), not(equalTo(Optional.empty())));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams$foo")), not(equalTo(Optional.empty())));

        assertThat(transformed.getShape(ShapeId.from("smithy.example#Unused")), equalTo(Optional.empty()));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#Unused$unusedMember")), equalTo(Optional.empty()));
    }

    @Test
    public void doesntKeepVendorParamsOnUnconnectedOperations() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params-with-unconnected-operation.smithy"))
                .assemble()
                .unwrap();

        Model transformed = ModelTransformer.create().removeUnreferencedShapes(model);
        assertThat(transformed.getShape(ShapeId.from("smithy.example#GetFoo")), equalTo(Optional.empty()));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams")), equalTo(Optional.empty()));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams$foo")), equalTo(Optional.empty()));
    }

    @Test
    public void doesntKeepIfSmokeTestsAreRemoved() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params-and-other-unused-shapes.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model transformed = transformer.removeUnreferencedShapes(transformer
                .removeTraitsIf(model, (shape, trait) -> trait.toShapeId().equals(SmokeTestsTrait.ID)));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams")), equalTo(Optional.empty()));
    }

    @Test
    public void keepsShapesReferencedByVendorParamsShape() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params-with-nested-shapes.smithy"))
                .assemble()
                .unwrap();

        Model transformed = ModelTransformer.create().removeUnreferencedShapes(model);
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams")), not(equalTo(Optional.empty())));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams$nestedStruct")), not(equalTo(Optional.empty())));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#NestedStruct")), not(equalTo(Optional.empty())));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#NestedStruct$nestedString")), not(equalTo(Optional.empty())));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#NestedString")), not(equalTo(Optional.empty())));
    }

    @Test
    public void doesntKeepShapesThatTargetVendorParams() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params-referenced-by-unconnected-shape.smithy"))
                .assemble()
                .unwrap();

        Model transformed = ModelTransformer.create().removeUnreferencedShapes(model);
        assertThat(transformed.getShape(ShapeId.from("smithy.example#VendorParams")), not(equalTo(Optional.empty())));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#Unconnected")), equalTo(Optional.empty()));
        assertThat(transformed.getShape(ShapeId.from("smithy.example#Unconnected$vendorParams")), equalTo(Optional.empty()));
    }

    @Test
    public void shapesConnectedToVendorParamsCanStillBeRemoved() {
        // NOTE: Removing `NestedStruct` also removes members that target it, mutating `VendorParams`.
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params-with-nested-shapes.smithy"))
                .assemble()
                .unwrap();

        ShapeId connected = ShapeId.from("smithy.example#NestedStruct");
        Model removeConnected = ModelTransformer.create()
                .removeShapes(model, ListUtils.of(model.expectShape(connected)));
        assertThat(removeConnected.getShape(connected).isPresent(), is(false));
    }
}
