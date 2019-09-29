package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ShapeIdShaderTest {
    @Test
    public void removesRootFromTargetNamesapce() {
        ShapeIdShader shader = ShapeIdShader.builder()
                .targetNamespace("EC2")
                .rootNamespace("com.amazon.ec2")
                .build();

        assertThat(shader.shade(ShapeId.from("com.amazon.ec2#Thing")),
                   equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.shade(ShapeId.from("com.amazon.ec2.nested#Thing")),
                   equalTo(ShapeId.from("EC2.nested#Thing")));
        assertThat(shader.shade(ShapeId.from("not.same#Thing")),
                   equalTo(ShapeId.from("EC2.not.same#Thing")));
    }

    @Test
    public void squishesNamespacesIntoNames() {
        ShapeIdShader shader = ShapeIdShader.builder()
                .targetNamespace("EC2")
                .rootNamespace("com.amazon.ec2")
                .build();

        assertThat(shader.shade(ShapeId.from("com.amazon.ec2#Thing"),
                                ShapeIdShader.ShadeOption.MERGE_NAME),
                   equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.shade(ShapeId.from("com.amazon.ec2.nested#Thing"),
                                ShapeIdShader.ShadeOption.MERGE_NAME),
                   equalTo(ShapeId.from("EC2#NestedThing")));
        assertThat(shader.shade(ShapeId.from("not.same#Thing"), ShapeIdShader.ShadeOption.MERGE_NAME),
                   equalTo(ShapeId.from("EC2#NotSameThing")));
    }

    @Test
    public void squishesNamespacesIntoNamespace() {
        ShapeIdShader shader = ShapeIdShader.builder()
                .targetNamespace("EC2")
                .rootNamespace("com.amazon.ec2")
                .build();

        assertThat(shader.shade(ShapeId.from("com.amazon.ec2#Thing"),
                                ShapeIdShader.ShadeOption.MERGE_NAMESPACE),
                   equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.shade(ShapeId.from("com.amazon.ec2.nested#Thing"),
                                ShapeIdShader.ShadeOption.MERGE_NAMESPACE),
                   equalTo(ShapeId.from("EC2.Nested#Thing")));
        assertThat(shader.shade(ShapeId.from("not.same#Thing"), ShapeIdShader.ShadeOption.MERGE_NAMESPACE),
                   equalTo(ShapeId.from("EC2.NotSame#Thing")));
    }

    @Test
    public void returnsIdsAsIs() {
        ShapeIdShader shader = ShapeIdShader.builder()
                .targetNamespace("EC2")
                .rootNamespace("EC2")
                .build();

        assertThat(shader.shade(ShapeId.from("EC2#Thing"),
                                ShapeIdShader.ShadeOption.MERGE_NAME),
                   equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.shade(ShapeId.from("EC2#Thing"),
                                ShapeIdShader.ShadeOption.MERGE_NAMESPACE),
                   equalTo(ShapeId.from("EC2#Thing")));
        assertThat(shader.shade(ShapeId.from("EC2#Thing")), equalTo(ShapeId.from("EC2#Thing")));
    }

    @Test
    public void sanitizesShapeIdTarget() {
        ShapeIdShader shader = ShapeIdShader.builder()
                .targetNamespace("Hey...you... guys!")
                .rootNamespace("com.amazon.example")
                .build();

        assertThat(shader.shade(ShapeId.from("com.amazon.example#BabyRuth")),
                   equalTo(ShapeId.from("Hey.you._guys_#BabyRuth")));
    }

    @Test
    public void canLoadFromConfig() {
        ShapeIdShader shader = ShapeIdShader.builder()
                .fromNode(Node.parse("{\"rootNamespace\": \"com.foo\", \"targetNamespace\": \"Foo\"}"))
                .build();

        assertThat(shader.shade(ShapeId.from("com.foo.bar#Hi")), equalTo(ShapeId.from("Foo.bar#Hi")));
    }
}
