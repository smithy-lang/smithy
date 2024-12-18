/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ArnIndexTest {
    private static Model model;

    @BeforeAll
    public static void beforeClass() {
        model = Model.assembler()
                .discoverModels(ArnIndexTest.class.getClassLoader())
                .addImport(ArnIndexTest.class.getResource("test-model.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void loadsFromModel() {
        ArnIndex arnIndex = new ArnIndex(model);
        ShapeId id = ShapeId.from("ns.foo#SomeService");
        Shape someResource = model.getShape(ShapeId.from("ns.foo#SomeResource")).get();
        ArnTrait template1 = ArnTrait.builder()
                .template("someresource/{someId}")
                .build();
        Shape childResource = model.getShape(ShapeId.from("ns.foo#ChildResource")).get();
        ArnTrait template2 = ArnTrait.builder()
                .template("someresource/{someId}/{childId}")
                .build();
        Shape rootArnResource = model.getShape(ShapeId.from("ns.foo#RootArnResource")).get();
        ArnTrait template3 = ArnTrait.builder()
                .template("rootArnResource")
                .noAccount(true)
                .noRegion(true)
                .build();

        assertThat(arnIndex.getServiceArnNamespace(id), equalTo("service"));
        Map<ShapeId, ArnTrait> templates = arnIndex.getServiceResourceArns(id);
        assertThat(templates, hasKey(someResource.getId()));
        assertThat(templates, hasKey(childResource.getId()));
        assertThat(templates, hasKey(rootArnResource.getId()));
        assertThat(templates.get(someResource.getId()), equalTo(template1));
        assertThat(templates.get(childResource.getId()), equalTo(template2));
        assertThat(templates.get(rootArnResource.getId()), equalTo(template3));
    }

    @Test
    public void computesFullArnTemplate() {
        ArnIndex arnIndex = new ArnIndex(model);
        ShapeId service = ShapeId.from("ns.foo#SomeService");

        assertThat(arnIndex.getFullResourceArnTemplate(service, ShapeId.from("ns.foo#SomeResource")),
                equalTo(Optional
                        .of("arn:{AWS::Partition}:service:{AWS::Region}:{AWS::AccountId}:someresource/{someId}")));
        assertThat(arnIndex.getFullResourceArnTemplate(service, ShapeId.from("ns.foo#ChildResource")),
                equalTo(Optional.of(
                        "arn:{AWS::Partition}:service:{AWS::Region}:{AWS::AccountId}:someresource/{someId}/{childId}")));
        assertThat(arnIndex.getFullResourceArnTemplate(service, ShapeId.from("ns.foo#RootArnResource")),
                equalTo(Optional.of("arn:{AWS::Partition}:service:::rootArnResource")));
        assertThat(arnIndex.getFullResourceArnTemplate(service, ShapeId.from("ns.foo#Invalid")),
                equalTo(Optional.empty()));
        assertThat(arnIndex.getFullResourceArnTemplate(service, ShapeId.from("ns.foo#AbsoluteResource")),
                equalTo(Optional.of("{arn}")));
    }

    @Test
    public void returnsDefaultServiceArnNamespace() {
        ArnIndex arnIndex = new ArnIndex(model);

        assertThat(arnIndex.getServiceArnNamespace(ShapeId.from("ns.foo#NonAwsService")), equalTo("nonawsservice"));
    }

    @Test
    public void returnsDefaultServiceArnNamespaceForAwsService() {
        ArnIndex arnIndex = new ArnIndex(model);

        assertThat(arnIndex.getServiceArnNamespace(ShapeId.from("ns.foo#EmptyAwsService")),
                equalTo("emptyawsservice"));
    }

    @Test
    public void findsEffectiveArns() {
        Model m = Model.assembler()
                .discoverModels(ArnIndexTest.class.getClassLoader())
                .addImport(ArnIndexTest.class.getResource("effective-arns.smithy"))
                .assemble()
                .unwrap();
        ArnIndex index = ArnIndex.of(m);
        ShapeId service = ShapeId.from("ns.foo#SomeService");

        assertThat(
                index.getEffectiveOperationArn(service, ShapeId.from("ns.foo#InstanceOperation"))
                        .map(ArnTrait::getTemplate),
                equalTo(Optional.of("foo/{id}")));
        assertThat(
                index.getEffectiveOperationArn(service, ShapeId.from("ns.foo#CollectionOperation"))
                        .map(ArnTrait::getTemplate),
                equalTo(Optional.of("foo")));
    }
}
