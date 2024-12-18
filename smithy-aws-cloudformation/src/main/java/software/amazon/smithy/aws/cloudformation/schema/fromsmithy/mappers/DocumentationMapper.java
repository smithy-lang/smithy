/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import static java.util.function.Function.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates the schema doc urls based on the resource's {@code @externalDocumentation}
 * trait. This is configurable based on the {@code "sourceDocKeys"} and
 * {@code "externalDocKeys"} plugin properties.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-sourceUrl">sourceUrl Docs</a>
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-documentationurl">documentationUrl Docs</a>
 */
@SmithyInternalApi
final class DocumentationMapper implements CfnMapper {

    @Override
    public void before(Context context, ResourceSchema.Builder builder) {
        ResourceShape resource = context.getResource();
        ExternalDocumentationTrait trait = resource.getTrait(ExternalDocumentationTrait.class).orElse(null);

        if (trait == null) {
            return;
        }

        CfnConfig config = context.getConfig();

        getResolvedExternalDocs(trait, config.getSourceDocs()).ifPresent(builder::sourceUrl);
        getResolvedExternalDocs(trait, config.getExternalDocs()).ifPresent(builder::documentationUrl);
    }

    private Optional<String> getResolvedExternalDocs(ExternalDocumentationTrait trait, List<String> enabledKeys) {
        // Get the valid list of lower case names to look for when converting.
        List<String> externalDocKeys = listToLowerCase(enabledKeys);

        // Get lower case keys to check for when converting.
        Map<String, String> traitUrls = trait.getUrls();
        Map<String, String> lowercaseKeyMap = traitUrls.keySet()
                .stream()
                .collect(MapUtils.toUnmodifiableMap(this::toLowerCase, identity()));

        for (String externalDocKey : externalDocKeys) {
            // Compare the lower case name, but use the specified name.
            if (lowercaseKeyMap.containsKey(externalDocKey)) {
                String traitKey = lowercaseKeyMap.get(externalDocKey);
                // Return the url from the trait.
                return Optional.of(traitUrls.get(traitKey));
            }
        }

        // We didn't find any external docs with the a name in the specified set.
        return Optional.empty();
    }

    private List<String> listToLowerCase(List<String> inputs) {
        List<String> outputs = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            outputs.add(toLowerCase(input));
        }
        return outputs;
    }

    private String toLowerCase(String input) {
        return input.toLowerCase(Locale.US);
    }
}
