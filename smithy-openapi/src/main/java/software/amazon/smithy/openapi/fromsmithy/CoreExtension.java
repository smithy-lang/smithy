package software.amazon.smithy.openapi.fromsmithy;

import java.util.List;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.openapi.fromsmithy.mappers.CheckForGreedyLabels;
import software.amazon.smithy.openapi.fromsmithy.mappers.CheckForPrefixHeaders;
import software.amazon.smithy.openapi.fromsmithy.mappers.JsonSubstitutionsMapper;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponentsMapper;
import software.amazon.smithy.openapi.fromsmithy.mappers.UnsupportedTraitsMapper;
import software.amazon.smithy.openapi.fromsmithy.protocols.AwsRestJsonProtocol;
import software.amazon.smithy.openapi.fromsmithy.security.AwsV4;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBasic;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBearer;
import software.amazon.smithy.openapi.fromsmithy.security.HttpDigest;
import software.amazon.smithy.openapi.fromsmithy.security.XApiKey;
import software.amazon.smithy.utils.ListUtils;

/**
 * Registers the core Smithy2OpenApi functionality.
 */
public final class CoreExtension implements Smithy2OpenApiExtension {
    @Override
    public List<SecuritySchemeConverter> getSecuritySchemeConverters() {
        return ListUtils.of(
            new AwsV4(),
            new HttpBasic(),
            new HttpBearer(),
            new HttpDigest(),
            new XApiKey()
        );
    }

    @Override
    public List<OpenApiProtocol> getProtocols() {
        return ListUtils.of(new AwsRestJsonProtocol());
    }

    @Override
    public List<OpenApiMapper> getOpenApiMappers() {
        return ListUtils.of(
                new CheckForGreedyLabels(),
                new CheckForPrefixHeaders(),
                new JsonSubstitutionsMapper(),
                new RemoveUnusedComponentsMapper(),
                new UnsupportedTraitsMapper()
        );
    }

    @Override
    public List<JsonSchemaMapper> getJsonSchemaMappers() {
        return ListUtils.of(new OpenApiJsonSchemaMapper());
    }
}
