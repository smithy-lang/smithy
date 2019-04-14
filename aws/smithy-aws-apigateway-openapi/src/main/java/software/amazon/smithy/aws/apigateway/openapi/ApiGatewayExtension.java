package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension;
import software.amazon.smithy.utils.ListUtils;

public final class ApiGatewayExtension implements Smithy2OpenApiExtension {
    @Override
    public List<OpenApiMapper> getOpenApiMappers() {
        return ListUtils.of(
                new AddApiKeySource(),
                new AddAuthorizers(),
                new AddBinaryTypes(),
                new AddIntegrations(),
                new AddRequestValidators(),
                new CloudFormationSubstitution()
        );
    }

    @Override
    public List<SecuritySchemeConverter> getSecuritySchemeConverters() {
        return ListUtils.of(new CognitoUserPoolsConverter());
    }
}
