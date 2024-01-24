/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.FunctionalUtils;

public class FlattenNamespacesTest {

    @Test
    public void flattenNamespacesOfShapesConnectedToSpecifiedService() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("flatten-namespaces.json").toURI()))
                .assemble()
                .unwrap();
        ObjectNode config = Node.objectNode()
                .withMember("namespace", Node.from("ns.qux"))
                .withMember("service", Node.from("ns.foo#MyService"));
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Model result = new FlattenNamespaces().transform(context);
        List<String> ids = result.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(Shape::getId)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(ids, containsInAnyOrder("ns.qux#MyService", "ns.qux#MyOperation", "ns.qux#MyOperationOutput",
                "ns.qux#MyOperationOutput$foo", "ns.corge#UnconnectedFromService", "ns.grault#MyOperationOutput"));
        assertThat(ids, not(containsInAnyOrder("ns.foo#MyService", "ns.bar#MyOperation", "ns.baz#MyOperationOutput",
                "ns.baz#MyOperationOutput$foo", "ns.qux#UnconnectedFromService")));
    }

    @Test
    public void includesAdditionalTaggedShapes() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("flatten-namespaces.json").toURI()))
                .assemble()
                .unwrap();
        ObjectNode config = Node.objectNode()
                .withMember("namespace", Node.from("ns.qux"))
                .withMember("service", Node.from("ns.foo#MyService"))
                .withMember("includeTagged", Node.arrayNode().withValue(Node.from("included")));
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Model result = new FlattenNamespaces().transform(context);
        List<String> ids = result.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(Shape::getId)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(ids, containsInAnyOrder("ns.qux#MyService", "ns.qux#MyOperation", "ns.qux#MyOperationOutput",
                "ns.qux#MyOperationOutput$foo", "ns.qux#UnconnectedFromService", "ns.grault#MyOperationOutput"));
        assertThat(ids, not(containsInAnyOrder("ns.foo#MyService", "ns.bar#MyOperation", "ns.baz#MyOperationOutput",
                "ns.baz#MyOperationOutput$foo", "ns.corge#UnconnectedFromService")));
    }

    @Test
    public void doesNotIncludeAdditionalTaggedShapesWhenTheyConflict() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("flatten-namespaces.json").toURI()))
                .assemble()
                .unwrap();
        ObjectNode config = Node.objectNode()
                .withMember("namespace", Node.from("ns.qux"))
                .withMember("service", Node.from("ns.foo#MyService"))
                .withMember("includeTagged", Node.arrayNode().withValue(Node.from("conflicting")));
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Model result = new FlattenNamespaces().transform(context);
        List<String> ids = result.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(Shape::getId)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(ids, containsInAnyOrder("ns.qux#MyService", "ns.qux#MyOperation", "ns.qux#MyOperationOutput",
                "ns.qux#MyOperationOutput$foo", "ns.corge#UnconnectedFromService", "ns.grault#MyOperationOutput"));
        assertThat(ids, not(containsInAnyOrder("ns.foo#MyService", "ns.bar#MyOperation", "ns.baz#MyOperationOutput",
                "ns.baz#MyOperationOutput$foo", "ns.qux#UnconnectedFromService")));
    }

    @Test
    public void supportsRenamesOnService() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("flatten-namespaces-with-renames.json").toURI()))
                .assemble()
                .unwrap();
        ObjectNode config = Node.objectNode()
                .withMember("namespace", Node.from("ns.qux"))
                .withMember("service", Node.from("ns.foo#MyService"));
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Model result = new FlattenNamespaces().transform(context);
        List<String> ids = result.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(Shape::getId)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(ids, containsInAnyOrder("ns.qux#MyService", "ns.qux#GetSomething", "ns.qux#GetSomethingOutput",
                "ns.qux#GetSomethingOutput$widget1", "ns.qux#GetSomethingOutput$fooWidget", "ns.qux#Widget",
                "ns.qux#FooWidget"));
        assertThat(ids, not(containsInAnyOrder("ns.foo#MyService", "ns.foo#GetSomething", "ns.foo#GetSomethingOutput",
                "ns.bar#Widget", "foo.example#Widget")));
    }

    @Test
    public void removesServiceRenames() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("flatten-namespaces-with-renames.json"))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("namespace", "ns.qux")
                        .withMember("service", "ns.foo#MyService"))
                .build();

        Model result = new FlattenNamespaces().transform(context);
        ShapeId transformedServiceId = ShapeId.from("ns.qux#MyService");

        assertThat(result.getShape(transformedServiceId), not(Optional.empty()));
        assertThat(result.expectShape(transformedServiceId, ServiceShape.class).getRename(), anEmptyMap());
    }

    @Test
    public void serviceShapeIsValidAfterTransform() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("flatten-namespaces-with-renames.json"))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("namespace", "ns.qux")
                        .withMember("service", "ns.foo#MyService"))
                .build();

        Model result = new FlattenNamespaces().transform(context);
        ValidatedResult<Model> validatedResult = Model.assembler()
                .addModel(result)
                .assemble();
        List<ShapeId> validationEventShapeIds = validatedResult.getValidationEvents().stream()
                .map(ValidationEvent::getShapeId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        assertThat(validationEventShapeIds, not(containsInAnyOrder(ShapeId.from("ns.qux#MyService"))));
    }

    @Test
    public void throwsWhenServiceIsNotConfigured() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("namespace", Node.from("ns.bar")))
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new FlattenNamespaces().transform(context));
    }

    @Test
    public void throwsWhenNamespaceIsNotConfigured() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("service", Node.from("ns.foo#MyService")))
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new FlattenNamespaces().transform(context));
    }

    @Test
    public void throwsWhenServiceCannotBeFoundInModel() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        ObjectNode config = Node.objectNode()
                .withMember("namespace", Node.from("ns.qux"))
                .withMember("service", Node.from("ns.foo#MyService"));
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new FlattenNamespaces().transform(context));
    }

    @Test
    public void throwsWhenServiceIsInvalidInModel() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\", \"shapes\": { \"ns.foo#InvalidService\": { \"type\": \"string\" } } }")
                .assemble()
                .unwrap();
        ObjectNode config = Node.objectNode()
                .withMember("namespace", Node.from("ns.qux"))
                .withMember("service", Node.from("ns.foo#InvalidService"));
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Assertions.assertThrows(ExpectationNotMetException.class, () -> new FlattenNamespaces().transform(context));
    }
}
