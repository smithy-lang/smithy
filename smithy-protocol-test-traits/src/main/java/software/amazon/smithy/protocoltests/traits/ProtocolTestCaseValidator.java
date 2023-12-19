/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.protocoltests.traits;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;
import software.amazon.smithy.utils.MediaType;

/**
 * Validates the following:
 *
 * <ul>
 *     <li>XML and JSON bodyMediaTypes contain valid content.</li>
 *     <li>vendorParamsShape is a valid shape.</li>
 *     <li>Vendor params are compatible with any referenced shape.</li>
 *     <li>Params for a test case are valid for the model.</li>
 * </ul>
 *
 * @param <T> Type of test case to validate.
 */
abstract class ProtocolTestCaseValidator<T extends Trait> extends AbstractValidator {

    private final Class<T> traitClass;
    private final ShapeId traitId;
    private final String descriptor;
    private final DocumentBuilderFactory documentBuilderFactory;

    ProtocolTestCaseValidator(ShapeId traitId, Class<T> traitClass, String descriptor) {
        this.traitId = traitId;
        this.traitClass = traitClass;
        this.descriptor = descriptor;
        documentBuilderFactory = DocumentBuilderFactory.newInstance();

        // Disallow loading DTDs and more for protocol test contents.
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setXIncludeAware(false);
            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(traitClass)) {
            events.addAll(validateShape(model, operationIndex, shape, shape.expectTrait(traitClass)));
        }

        return events;
    }

    abstract StructureShape getStructure(Shape shape, OperationIndex operationIndex);

    abstract List<? extends HttpMessageTestCase> getTestCases(T trait);

    boolean isValidatedBy(Shape shape) {
        return shape instanceof OperationShape;
    }

    private List<ValidationEvent> validateShape(
            Model model,
            OperationIndex operationIndex,
            Shape shape,
            T trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        List<? extends HttpMessageTestCase> testCases = getTestCases(trait);

        for (int i = 0; i < testCases.size(); i++) {
            HttpMessageTestCase testCase = testCases.get(i);

            // Validate the syntax of known media types like XML and JSON.
            events.addAll(validateMediaType(shape, trait, testCase));

            // Validate the vendorParams for the test case if we have a shape defined.
            Optional<ShapeId> vendorParamsShapeOptional = testCase.getVendorParamsShape();
            ObjectNode vendorParams = testCase.getVendorParams();
            if (vendorParamsShapeOptional.isPresent() && isValidatedBy(shape)) {
                if (vendorParams.isEmpty()) {
                    // Warn if vendorParamsShape is set on the case and no vendorParams is set.
                    events.add(warning(shape, trait,
                            "Protocol test case defined a `vendorParamsShape` but no `vendorParams`"));
                } else {
                    // Otherwise, validate the params against the shape.
                    Shape vendorParamsShape = model.expectShape(vendorParamsShapeOptional.get());
                    NodeValidationVisitor vendorParamsValidator = createVisitor(vendorParams, model, shape, i,
                            ".vendorParams");
                    events.addAll(vendorParamsShape.accept(vendorParamsValidator));
                }
            }

            StructureShape struct = getStructure(shape, operationIndex);
            if (struct != null) {
                // Validate the params for the test case.
                NodeValidationVisitor validator = createVisitor(testCase.getParams(), model, shape, i, ".params");
                events.addAll(struct.accept(validator));
            } else if (!testCase.getParams().isEmpty() && isValidatedBy(shape)) {
                events.add(error(shape, trait, String.format(
                        "Protocol test %s parameters provided for operation with no %s: `%s`",
                        descriptor, descriptor, Node.printJson(testCase.getParams()))));
            }
        }

        return events;
    }

    private NodeValidationVisitor createVisitor(
            ObjectNode value,
            Model model,
            Shape shape,
            int position,
            String contextSuffix
    ) {
        return NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext(traitId + "." + position + contextSuffix)
                .eventId(getName())
                .timestampValidationStrategy(TimestampValidationStrategy.EPOCH_SECONDS)
                .addFeature(NodeValidationVisitor.Feature.ALLOW_OPTIONAL_NULLS)
                .build();
    }

    private List<ValidationEvent> validateMediaType(Shape shape, Trait trait, HttpMessageTestCase test) {
        // Only validate the body if it's a non-empty string. Some protocols
        // require a content-type header even with no payload.
        if (!test.getBody().filter(s -> !s.isEmpty()).isPresent()) {
            return Collections.emptyList();
        }

        String rawMediaType = test.getBodyMediaType().orElse("application/octet-stream");
        MediaType mediaType = MediaType.from(rawMediaType);
        List<ValidationEvent> events = new ArrayList<>();
        if (isXml(mediaType)) {
            validateXml(shape, trait, test).ifPresent(events::add);
        } else if (isJson(mediaType)) {
            validateJson(shape, trait, test).ifPresent(events::add);
        }

        return events;
    }

    private boolean isXml(MediaType mediaType) {
        return mediaType.getSubtype().equals("xml") || mediaType.getSuffix().orElse("").equals("xml");
    }

    private boolean isJson(MediaType mediaType) {
        return mediaType.getSubtype().equals("json") || mediaType.getSuffix().orElse("").equals("json");
    }

    private Optional<ValidationEvent> validateXml(Shape shape, Trait trait, HttpMessageTestCase test) {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(test.getBody().orElse(""))));
            return Optional.empty();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return Optional.of(emitMediaTypeError(shape, trait, test, e));
        }
    }

    private Optional<ValidationEvent> validateJson(Shape shape, Trait trait, HttpMessageTestCase test) {
        try {
            Node.parse(test.getBody().orElse(""));
            return Optional.empty();
        } catch (ModelSyntaxException e) {
            return Optional.of(emitMediaTypeError(shape, trait, test, e));
        }
    }

    private ValidationEvent emitMediaTypeError(Shape shape, Trait trait, HttpMessageTestCase test, Throwable e) {
        return danger(shape, trait, String.format(
                "Invalid %s content in `%s` protocol test case `%s`: %s",
                test.getBodyMediaType().orElse(""),
                trait.toShapeId(),
                test.getId(),
                e.getMessage()));
    }
}
