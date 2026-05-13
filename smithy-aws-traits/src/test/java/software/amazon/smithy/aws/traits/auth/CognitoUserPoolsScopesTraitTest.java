/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class CognitoUserPoolsScopesTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        Node node = Node.fromStrings("email", "profile", "openid");
        Trait trait = factory.createTrait(CognitoUserPoolsScopesTrait.ID, id, node).get();

        assertThat(trait, instanceOf(CognitoUserPoolsScopesTrait.class));
        assertThat(((CognitoUserPoolsScopesTrait) trait).getValues(),
                equalTo(Arrays.asList("email", "profile", "openid")));
        assertThat(factory.createTrait(CognitoUserPoolsScopesTrait.ID, id, trait.toNode()).get(),
                equalTo(trait));
    }

    @Test
    public void roundTripsFromBuilder() {
        CognitoUserPoolsScopesTrait trait = CognitoUserPoolsScopesTrait.builder()
                .addValue("read")
                .addValue("write")
                .build();

        assertThat(trait.getValues(), equalTo(Arrays.asList("read", "write")));
        assertThat(trait.toBuilder().build(), equalTo(trait));
        assertThat(new CognitoUserPoolsScopesTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }
}
