/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait.UnstableFeatureInfo;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait.UnstableReason;

public class UnstableFeaturesTraitTest {
    @Test
    public void loadsTrait() {
        Node node = Node.parse("{\"MYSERVICE_FIRST_PREVIEW\": "
                + "{\"message\": \"This is my preview operation and is subject to change!\", "
                + "\"reason\": \"PREVIEW\"}}");
        UnstableFeaturesTrait trait = new UnstableFeaturesTrait.Provider()
                .createTrait(ShapeId.from("ns.qux#MyService"), node);

        assertThat(trait.toNode(), equalTo(node));
        assertThat(trait.toBuilder().build(), equalTo(trait));
        assertThat(trait.getFeatures(), hasKey("MYSERVICE_FIRST_PREVIEW"));

        UnstableFeatureInfo info = trait.getFeature("MYSERVICE_FIRST_PREVIEW").get();
        assertThat(info.getMessage().get(), equalTo("This is my preview operation and is subject to change!"));
        assertThat(info.getReason().get(), equalTo(UnstableReason.PREVIEW));
    }

    @Test
    public void loadsEntryWithoutMessage() {
        Node node = Node.parse("{\"MYSERVICE_FIRST_PREVIEW\": {\"reason\": \"PREVIEW\"}}");
        UnstableFeaturesTrait trait = new UnstableFeaturesTrait.Provider()
                .createTrait(ShapeId.from("ns.qux#MyService"), node);

        UnstableFeatureInfo info = trait.getFeature("MYSERVICE_FIRST_PREVIEW").get();
        assertThat(info.getMessage(), equalTo(Optional.empty()));
        assertThat(info.getReason().get(), equalTo(UnstableReason.PREVIEW));
        assertThat(trait.toNode(), equalTo(node));
    }

    @Test
    public void expectsValidReason() {
        Assertions.assertThrows(SourceException.class, () -> {
            new UnstableFeaturesTrait.Provider().createTrait(
                    ShapeId.from("ns.qux#MyService"),
                    Node.parse("{\"MYSERVICE_FIRST_PREVIEW\": {\"reason\": \"GARBAGE\"}}"));
        });
    }

    @Test
    public void convertsToBuilder() {
        UnstableFeaturesTrait trait = UnstableFeaturesTrait.builder()
                .putFeature("MYSERVICE_FIRST_PREVIEW",
                        UnstableFeatureInfo.of("Preview message.", UnstableReason.PREVIEW))
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}
