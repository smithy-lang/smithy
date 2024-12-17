/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Adds API Gateway binary media types as a top-level key in the OpenAPI model
 * named {@code x-amazon-apigateway-binary-media-types}.
 *
 * <p>This data is used by API Gateway to determine which content-types do
 * not contain UTF-8 data.
 */
final class AddBinaryTypes implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(AddBinaryTypes.class.getName());
    private static final String EXTENSION_NAME = "x-amazon-apigateway-binary-media-types";
    private static final String DEFAULT_BINARY_TYPE = "application/octet-stream";

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST, ApiGatewayConfig.ApiType.HTTP);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        List<String> binaryTypes = supportedMediaTypes(context).sorted().collect(Collectors.toList());

        if (!binaryTypes.isEmpty()) {
            LOGGER.fine(() -> "Adding recognized binary types to model: " + binaryTypes);
            return openApi.toBuilder()
                    .putExtension(EXTENSION_NAME,
                            Stream.concat(binaryTypes.stream(), Stream.of(DEFAULT_BINARY_TYPE))
                                    .distinct()
                                    .map(Node::from)
                                    .collect(ArrayNode.collect()))
                    .build();
        }

        return openApi;
    }

    private Stream<String> supportedMediaTypes(Context<? extends Trait> context) {
        Model model = context.getModel();
        HttpBindingIndex httpBindingIndex = HttpBindingIndex.of(context.getModel());
        TopDownIndex topDownIndex = TopDownIndex.of(context.getModel());

        // Find the media types defined on all request and response bindings.
        return topDownIndex.getContainedOperations(context.getService())
                .stream()
                .flatMap(operation -> Stream.concat(
                        OptionalUtils.stream(
                                binaryMediaType(model, httpBindingIndex.getRequestBindings(operation))),
                        OptionalUtils.stream(
                                binaryMediaType(model, httpBindingIndex.getResponseBindings(operation)))));
    }

    private Optional<String> binaryMediaType(Model model, Map<String, HttpBinding> httpBindings) {
        return httpBindings.values()
                .stream()
                .filter(binding -> binding.getLocation().equals(HttpBinding.Location.PAYLOAD))
                .map(HttpBinding::getMember)
                .flatMap(member -> OptionalUtils.stream(member.getMemberTrait(model, MediaTypeTrait.class)))
                .map(MediaTypeTrait::getValue)
                .findFirst();
    }
}
