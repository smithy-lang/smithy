/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class ArnTraitTest {

    @Test
    public void loadsTraitWithFromNode() {
        Node node = Node.parse("{\"template\": \"resourceName\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ArnTrait.class));
        ArnTrait arnTrait = (ArnTrait) trait.get();
        assertThat(arnTrait.getTemplate(), equalTo("resourceName"));
        assertThat(arnTrait.isNoAccount(), is(false));
        assertThat(arnTrait.isNoRegion(), is(false));
        assertThat(arnTrait.isAbsolute(), is(false));
        assertThat(arnTrait.getLabels(), empty());
        assertThat(arnTrait.getResourceDelimiter().isPresent(), is(false));
        assertThat(arnTrait.isReusable(), is(false));
    }

    @Test
    public void canSetOtherFields() {
        Node node = Node.parse(
                "{\"noAccount\": true, \"noRegion\": true, \"absolute\": false, \"template\": \"foo\", \"reusable\": true}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        ArnTrait arnTrait = (ArnTrait) trait.get();
        assertThat(arnTrait.getTemplate(), equalTo("foo"));
        assertThat(arnTrait.isNoAccount(), is(true));
        assertThat(arnTrait.isNoRegion(), is(true));
        assertThat(arnTrait.isAbsolute(), is(false));
        assertThat(arnTrait.toNode(), equalTo(node));
        assertThat(arnTrait.toBuilder().build(), equalTo(arnTrait));
        assertThat(arnTrait.isReusable(), is(true));
    }

    @Test
    public void canSetAbsoluteAndDelimiter() {
        Node node = Node.parse("{\"absolute\": true, \"template\": \"foo\", \"resourceDelimiter\": \":\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        ArnTrait arnTrait = (ArnTrait) trait.get();
        assertThat(arnTrait.getTemplate(), equalTo("foo"));
        assertThat(arnTrait.toBuilder().build(), equalTo(arnTrait));
        assertThat(arnTrait.isAbsolute(), is(true));
        assertThat(arnTrait.getResourceDelimiter().get(), equalTo(ArnTrait.ResourceDelimiter.COLON));
    }

    @Test
    public void canSetIncludeTemplateExpressions() {
        Node node = Node
                .parse("{\"noAccount\": false, \"noRegion\": false, \"template\": \"foo/{Baz}/bar/{Bam}/boo/{Boo}\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        ArnTrait arnTrait = (ArnTrait) trait.get();
        assertThat(arnTrait.getTemplate(), equalTo("foo/{Baz}/bar/{Bam}/boo/{Boo}"));
        assertThat(arnTrait.getLabels(), contains("Baz", "Bam", "Boo"));
    }

    @Test
    public void resourcePartCannotStartWithSlash() {
        assertThrows(SourceException.class, () -> {
            Node node = Node.parse("{\"template\": \"/resource\"}");
            TraitFactory.createServiceFactory()
                    .createTrait(ArnTrait.ID,
                            ShapeId.from("ns.foo#foo"),
                            node);
        });
    }

    @Test
    public void validatesAccountValue() {
        assertThrows(SourceException.class, () -> {
            Node node = Node.parse("{\"template\": \"foo\", \"noAccount\": \"invalid\"}");
            TraitFactory.createServiceFactory()
                    .createTrait(ArnTrait.ID,
                            ShapeId.from("ns.foo#foo"),
                            node);
        });
    }

    @Test
    public void validatesRegionValue() {
        assertThrows(SourceException.class, () -> {
            Node node = Node.parse("{\"template\": \"foo\", \"noRegion\": \"invalid\"}");
            TraitFactory.createServiceFactory()
                    .createTrait(ArnTrait.ID,
                            ShapeId.from("ns.foo#foo"),
                            node);
        });
    }
}
