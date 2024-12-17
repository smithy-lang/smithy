/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.sourcecontext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class DefaultSourceLoaderTest {

    @Test
    public void requiresAtLeastOneLine() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SourceContextLoader.createLineBasedLoader(0);
        });
    }

    @Test
    public void ignoresSourceLocationNone() {
        assertThat(SourceContextLoader.createLineBasedLoader(4).loadContext(SourceLocation.NONE), empty());
    }

    @Test
    public void ignoresInvalidSourceLocations() {
        String file = getClass().getResource("context.smithy").getFile();

        assertThat(SourceContextLoader.createLineBasedLoader(4).loadContext(new SourceLocation(file, -1, -1)),
                empty());
        assertThat(SourceContextLoader.createLineBasedLoader(4).loadContext(new SourceLocation(file, 9999, -1)),
                empty());
    }

    @Test
    public void doesNotLoadHttp() {
        SourceContextLoader loader = SourceContextLoader.createLineBasedLoader(1);
        SourceLocation location = new SourceLocation("http://test.com", 1, 1);

        Assertions.assertThrows(UncheckedIOException.class, () -> loader.loadContext(location));
    }

    @Test
    public void doesNotLoadHttps() {
        SourceContextLoader loader = SourceContextLoader.createLineBasedLoader(1);
        SourceLocation location = new SourceLocation("https://test.com", 1, 1);

        Assertions.assertThrows(UncheckedIOException.class, () -> loader.loadContext(location));
    }

    @Test
    public void loadsSingleLineContextFromFile() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createLineBasedLoader(1);
        SourceLocation location = model.expectShape(ShapeId.from("example.smithy#Foo$bar")).getSourceLocation();
        Collection<SourceContextLoader.Line> context = loader.loadContext(location);

        assertThat(context, hasSize(1));
        assertThat(context.iterator().next().toString(), containsString("  bar: String,"));
    }

    @Test
    public void loadsMultipleLineContextFromFile() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createLineBasedLoader(4);
        SourceLocation location = model.expectShape(ShapeId.from("example.smithy#Foo$bar")).getSourceLocation();
        Collection<SourceContextLoader.Line> context = loader.loadContext(location);
        Iterator<SourceContextLoader.Line> iter = context.iterator();

        assertThat(context, hasSize(4));
        assertThat(iter.next().toString(), containsString("namespace example.smithy"));
        assertThat(iter.next().toString(), endsWith("| "));
        assertThat(iter.next().toString(), containsString("structure Foo {"));
        assertThat(iter.next().toString(), containsString("  bar: String,"));
    }

    @Test
    public void loadsContextFromJar() {
        Model model = Model.assembler().addImport(getClass().getResource("../jar-import.jar")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createLineBasedLoader(1);
        SourceLocation location = model.expectShape(ShapeId.from("foo.baz#A")).getSourceLocation();
        Collection<SourceContextLoader.Line> context = loader.loadContext(location);

        assertThat(context, not(empty()));
        assertThat(context.iterator().next().toString(), containsString("string A"));
    }

    @Test
    public void modelLoadsLeadingLinesUpToShape() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(model, 4);
        Shape shape = model.expectShape(ShapeId.from("example.smithy#Baz"));
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .shape(shape)
                .message("Test")
                .build();
        Collection<SourceContextLoader.Line> context = loader.loadContext(event);
        Iterator<SourceContextLoader.Line> iter = context.iterator();

        assertThat(context, hasSize(3));
        assertThat(iter.next().toString(), containsString("/// Docs"));
        assertThat(iter.next().toString(), containsString("@deprecated"));
        assertThat(iter.next().toString(), containsString("structure Baz {"));
    }

    @Test
    public void showsContainerAndMember() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(model, 4);
        Shape shape = model.expectShape(ShapeId.from("example.smithy#Baz$bam"));
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .shape(shape)
                .message("Test")
                .build();
        Collection<SourceContextLoader.Line> context = loader.loadContext(event);
        Iterator<SourceContextLoader.Line> iter = context.iterator();

        assertThat(context, hasSize(2));
        assertThat(iter.next().toString(), containsString("structure Baz {"));
        assertThat(iter.next().toString(), containsString("  bam: String,"));
    }

    @Test
    public void ignoresInvalidMembersThatAreAboveContainers() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        MemberShape member = model.expectShape(ShapeId.from("example.smithy#Baz$bam"), MemberShape.class);
        // Create a modified member that is incorrectly above the container shape.
        MemberShape modified = member.toBuilder()
                .addTrait(new SensitiveTrait()) // change it so the change takes effect
                .source(new SourceLocation(member.getSourceLocation().getFilename(), 1, 1))
                .build();
        Model updated = ModelTransformer.create().replaceShapes(model, Collections.singleton(modified));
        SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(updated, 4);
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .shape(modified)
                .message("Test")
                .build();
        Collection<SourceContextLoader.Line> context = loader.loadContext(event);

        assertThat(context, empty());
    }

    @Test
    public void showsMemberAfterTrait() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(model, 4);
        Shape shape = model.expectShape(ShapeId.from("example.smithy#Baz$bam"));
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .shape(shape)
                .sourceLocation(shape.expectTrait(DocumentationTrait.class))
                .message("Test")
                .build();
        Collection<SourceContextLoader.Line> context = loader.loadContext(event);
        Iterator<SourceContextLoader.Line> iter = context.iterator();

        assertThat(context, hasSize(2));
        assertThat(iter.next().toString(), containsString("/// Hello!"));
        assertThat(iter.next().toString(), containsString("bam: String,"));
    }

    @Test
    public void applyStatementsJustShowTheStatement() {
        Model model = Model.assembler().addImport(getClass().getResource("context.smithy")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(model, 4);
        Shape shape = model.expectShape(ShapeId.from("example.smithy#Foo"));
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .shape(shape)
                .sourceLocation(shape.expectTrait(DocumentationTrait.class))
                .message("Test")
                .build();
        Collection<SourceContextLoader.Line> context = loader.loadContext(event);
        Iterator<SourceContextLoader.Line> iter = context.iterator();

        assertThat(context, hasSize(1));
        assertThat(iter.next().toString(), containsString("apply Foo @documentation(\"applied\")"));
    }

    @Test
    public void showsJsonModelTraits() {
        Model model = Model.assembler().addImport(getClass().getResource("context.json")).assemble().unwrap();
        SourceContextLoader loader = SourceContextLoader.createModelAwareLoader(model, 4);
        Shape shape = model.expectShape(ShapeId.from("example.smithy#Foo"));
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id("Foo")
                .shape(shape)
                .sourceLocation(shape.expectTrait(SensitiveTrait.class))
                .message("Test")
                .build();
        Collection<SourceContextLoader.Line> context = loader.loadContext(event);
        Iterator<SourceContextLoader.Line> iter = context.iterator();

        assertThat(context, hasSize(2));
        assertThat(iter.next().toString(), containsString("\"example.smithy#Foo\": {"));
        assertThat(iter.next().toString(), containsString("\"smithy.api#sensitive\": {}"));
    }
}
