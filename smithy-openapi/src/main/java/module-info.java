import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.jsonschema.SchemaBuilderMapper;
import software.amazon.smithy.openapi.fromsmithy.OpenApiJsonSchemaMapper;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApi;
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.fromsmithy.plugins.CheckForGreedyLabels;
import software.amazon.smithy.openapi.fromsmithy.plugins.CheckForPrefixHeaders;
import software.amazon.smithy.openapi.fromsmithy.plugins.RemoveUnusedComponentsPlugin;
import software.amazon.smithy.openapi.fromsmithy.plugins.UnsupportedTraitsPlugin;
import software.amazon.smithy.openapi.fromsmithy.protocols.AwsRestJsonProtocol;
import software.amazon.smithy.openapi.fromsmithy.security.AmazonCognitoUserPools;
import software.amazon.smithy.openapi.fromsmithy.security.AwsV4;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBasic;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBearer;
import software.amazon.smithy.openapi.fromsmithy.security.HttpDigest;
import software.amazon.smithy.openapi.fromsmithy.security.XApiKey;

module software.amazon.smithy.openapi {
    requires java.logging;
    requires transitive software.amazon.smithy.jsonschema;
    requires software.amazon.smithy.model;
    requires software.amazon.smithy.build;

    exports software.amazon.smithy.openapi;
    exports software.amazon.smithy.openapi.fromsmithy;
    exports software.amazon.smithy.openapi.model;

    uses OpenApiProtocol;
    uses SchemaBuilderMapper;
    uses SmithyOpenApiPlugin;
    uses SecuritySchemeConverter;

    provides OpenApiProtocol with AwsRestJsonProtocol;

    provides SchemaBuilderMapper with OpenApiJsonSchemaMapper;

    provides SmithyOpenApiPlugin with
            CheckForGreedyLabels,
            CheckForPrefixHeaders,
            RemoveUnusedComponentsPlugin,
            UnsupportedTraitsPlugin;

    provides SecuritySchemeConverter with
            AmazonCognitoUserPools,
            AwsV4,
            HttpBasic,
            HttpDigest,
            HttpBearer,
            XApiKey;

    provides SmithyBuildPlugin with Smithy2OpenApi;
}
