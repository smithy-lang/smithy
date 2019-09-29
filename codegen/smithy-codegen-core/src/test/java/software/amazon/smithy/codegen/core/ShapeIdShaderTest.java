package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;

public class ShapeIdShaderTest {
    @Test
    public void removesRootFromTargetNamesapce() {
        Function<ShapeId, ShapeId> shader = ShapeIdShader.createShader(
                "com.amazon.ec2", "EC2", ShapeIdShader.APPEND);

        assertThat(shader.apply(ShapeId.from("com.amazon.ec2#Thing")),
                   equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.apply(ShapeId.from("com.amazon.ec2.Nested#Thing")),
                   equalTo(ShapeId.from("EC2.Nested#Thing")));
        assertThat(shader.apply(ShapeId.from("not.same#Thing")),
                   equalTo(ShapeId.from("EC2.not.same#Thing")));
    }

    @Test
    public void mergesNamespacesIntoNames() {
        Function<ShapeId, ShapeId> shader = ShapeIdShader.createShader(
                "com.amazon.ec2", "EC2", ShapeIdShader.MERGE_NAME);

        assertThat(shader.apply(ShapeId.from("com.amazon.ec2#Thing")), equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.apply(ShapeId.from("com.amazon.ec2.nested#Thing")),
                   equalTo(ShapeId.from("EC2#NestedThing")));
        assertThat(shader.apply(ShapeId.from("not.same#Thing")), equalTo(ShapeId.from("EC2#NotSameThing")));
    }

    @Test
    public void mergesNamespacesIntoNamespace() {
        Function<ShapeId, ShapeId> shader = ShapeIdShader.createShader(
                "com.amazon.ec2", "EC2", ShapeIdShader.MERGE_NAMESPACE);

        assertThat(shader.apply(ShapeId.from("com.amazon.ec2#Thing")), equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.apply(ShapeId.from("com.amazon.ec2.Nested#Thing")),
                   equalTo(ShapeId.from("EC2.Nested#Thing")));
        assertThat(shader.apply(ShapeId.from("not.same#Thing")), equalTo(ShapeId.from("EC2.NotSame#Thing")));
    }

    @Test
    public void flattensNamespaces() {
        Function<ShapeId, ShapeId> shader = ShapeIdShader.createShader(
                "com.amazon.ec2", "EC2", ShapeIdShader.FLATTEN);

        assertThat(shader.apply(ShapeId.from("com.amazon.ec2#Thing")), equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.apply(ShapeId.from("com.amazon.ec2.Nested#Thing")), equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.apply(ShapeId.from("not.same#Thing")), equalTo(ShapeId.from("EC2#Thing")));
    }

    @Test
    public void returnsIdsAsIs() {
        Function<ShapeId, ShapeId> shader = ShapeIdShader.createShader("EC2", "EC2", ShapeIdShader.MERGE_NAME);

        assertThat(shader.apply(ShapeId.from("EC2#Thing")), equalTo(ShapeId.from("EC2#Thing")));
    }

    @Test
    public void sanitizesShapeIdTarget() {
        Function<ShapeId, ShapeId> shader = ShapeIdShader.createShader(
                "com.amazon.example", "Hey...you... guys!", ShapeIdShader.APPEND);

        assertThat(shader.apply(ShapeId.from("com.amazon.example#BabyRuth")),
                   equalTo(ShapeId.from("Hey.you._guys_#BabyRuth")));
    }
}
