/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode.NO_AUTH_AWARE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.synthetic.NoAuthTrait;

public class ServiceIndexTest {

    private static final ShapeId CUSTOM_AUTH_ID = ShapeId.from("smithy.example#customAuth");

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(ServiceIndexTest.class.getResource("service-index-finds-auth-schemes.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void protocolsOfService() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-index-loads-protocols.smithy"))
                .assemble()
                .unwrap();
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Map<ShapeId, Trait> protocols = serviceIndex.getProtocols(ShapeId.from("smithy.example#TestService"));

        assertThat(protocols, hasKey(ShapeId.from("smithy.example#fooJson")));
        assertThat(protocols, hasKey(ShapeId.from("smithy.example#fooXml")));
        assertThat(protocols.keySet(), hasSize(2));
    }

    @Test
    public void authSchemesOfService() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Map<ShapeId, Trait> auth = serviceIndex.getAuthSchemes(
                ShapeId.from("smithy.example#ServiceWithoutAuthTrait"));
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID, CUSTOM_AUTH_ID);
    }

    @Test
    public void authSchemesOfServiceWithoutAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithoutAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID, CUSTOM_AUTH_ID);

        auth = serviceIndex.getEffectiveAuthSchemes(service, NO_AUTH_AWARE);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID, CUSTOM_AUTH_ID);
    }

    @Test
    public void authSchemesOfServiceWithAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpDigestAuthTrait.ID);

        auth = serviceIndex.getEffectiveAuthSchemes(service, NO_AUTH_AWARE);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpDigestAuthTrait.ID);
    }

    @Test
    public void authSchemesOfServiceWithEmptyAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithEmptyAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfServiceWithoutAuthDefinitionTraits() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithoutAuthDefinitionTraits");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfOperationWithoutAuthTraitAndServiceWithoutAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithoutAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithoutAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID, CUSTOM_AUTH_ID);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID, CUSTOM_AUTH_ID);
    }

    @Test
    public void authSchemesOfOperationWithoutAuthTraitAndServiceWithAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithoutAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpDigestAuthTrait.ID);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpDigestAuthTrait.ID);
    }

    @Test
    public void authSchemesOfOperationWithoutAuthTraitAndServiceWithEmptyAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithEmptyAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithoutAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfOperationWithAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(
                ShapeId.from("smithy.example#ServiceWithAuthTrait"),
                ShapeId.from("smithy.example#OperationWithAuthTrait"));
        assertAuthSchemes(auth, HttpDigestAuthTrait.ID);
    }

    @Test
    public void authSchemesOfOperationWithEmptyAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithEmptyAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfOperationWithOptionalAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithOptionalAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpDigestAuthTrait.ID);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpDigestAuthTrait.ID, NoAuthTrait.ID);
    }

    // Test to assert that smithy.api#noAuth trait is not part of traits that are sorted alphabetically, but last.
    // The authSchemesOfOperationWithOptionalAuthTrait() test above doesn't really assert that, because
    // smithy.api#noAuth would have been last if included in sorting.
    @Test
    public void authSchemesOfOperationWithOptionalAuthTraitAndServiceWithoutAuthTrait() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithoutAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithOptionalAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID, CUSTOM_AUTH_ID);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth,
                HttpBasicAuthTrait.ID,
                HttpBearerAuthTrait.ID,
                HttpDigestAuthTrait.ID,
                CUSTOM_AUTH_ID,
                NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfInvalidService() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#Invalid");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfInvalidServiceWithInvalidOperation() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#Invalid");
        ShapeId operation = ShapeId.from("smithy.example#OperationWithAuthTrait");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    @Test
    public void authSchemesOfServiceWithInvalidOperation() {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#ServiceWithoutAuthTrait");
        ShapeId operation = ShapeId.from("smithy.example#InvalidOperation");

        Map<ShapeId, Trait> auth = serviceIndex.getEffectiveAuthSchemes(service, operation);
        assertAuthSchemes(auth);

        auth = serviceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE);
        assertAuthSchemes(auth, NoAuthTrait.ID);
    }

    private void assertAuthSchemes(Map<ShapeId, Trait> auth, ShapeId... authSchemes) {
        List<ShapeId> ids = new ArrayList<>(auth.keySet());
        assertThat(ids, hasSize(authSchemes.length));
        assertThat(ids, equalTo(Arrays.asList(authSchemes)));
    }
}
