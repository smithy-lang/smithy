$version: "2.0"

namespace aws.apigateway

use aws.api#arnReference

/// Specifies the source of the caller identifier that will be used to throttle
/// API methods that require a key.
@externalDocumentation(
    "Developer Guide": "https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-api-key-source.html"
)
@internal
@tags(["internal"])
@trait(selector: "service")
string apiKeySource

/// Attaches an authorizer to a service, resource, or operation.
@internal
@tags(["internal"])
@trait(selector: ":test(service, resource, operation)")
string authorizer

/// A list of API Gateway authorizers to augment the service's declared authentication
/// mechanisms.
@internal
@tags(["internal"])
@trait(selector: "service")
map authorizers {
    key: String
    value: AuthorizerDefinition
}

/// Defines an API Gateway integration.
@internal
@tags(["internal"])
@trait(
    selector: ":test(service, resource, operation)"
    conflicts: [
        "aws.apigateway#mockIntegration"
    ]
)
structure integration {
    @required
    type: IntegrationType

    @required
    uri: Arn

    credentials: IamRoleArn

    @required
    httpMethod: String

    passThroughBehavior: PassThroughBehavior

    contentHandling: ContentHandling

    timeoutInMillis: Integer

    connectionId: String

    connectionType: ConnectionType

    cacheNamespace: String

    payloadFormatVersion: String

    cacheKeyParameters: StringList

    requestParameters: RequestParameters

    requestTemplates: Templates

    responses: IntegrationResponses
}

/// Defines an API Gateway mock integration.
@internal
@tags(["internal"])
@trait(
    selector: ":test(service, resource, operation)"
    conflicts: [
        "aws.apigateway#integration"
    ]
)
structure mockIntegration {
    passThroughBehavior: PassThroughBehavior

    requestParameters: RequestParameters

    requestTemplates: Templates

    responses: IntegrationResponses
}

/// Selects which request validation strategy to use. One of: 'full', 'params-only', 'body-only'
@internal
@tags(["internal"])
@trait(selector: ":test(service, operation)")
string requestValidator

/// An object that associates an authorizer and associated metadata with an
/// authentication mechanism.
@private
structure AuthorizerDefinition {
    /// The Smithy authentication scheme used by the client (e.g, aws.v4).
    @idRef(
        selector: "[trait|authDefinition]"
        failWhenMissing: true
        errorMessage: "The scheme of an authorizer definition must reference an auth trait"
    )
    @required
    scheme: String

    /// The type of the authorizer. If specifying information beyond the scheme,
    /// this value is required. The value must be "token", for an authorizer
    /// with the caller identity embedded in an authorization token, or
    /// "request", for an authorizer with the caller identity contained in
    /// request parameters.
    type: String

    /// This value is not used directly by API Gateway but will be used for
    /// OpenAPI exports. This will default to "awsSigV4" if your scheme is
    /// "aws.v4", or "custom" otherwise.
    customAuthType: String

    /// The Uniform Resource Identifier (URI) of the authorizer Lambda function
    uri: String

    /// Credentials required for invoking the authorizer
    credentials: String

    /// Comma-separated list of mapping expressions of the request parameters
    /// as the identity source. Applicable for the authorizer of the "request"
    /// type only.
    identitySource: String

    /// A regular expression for validating the token as the incoming identity
    identityValidationExpression: String

    /// The number of seconds for which the resulting IAM policy is cached.
    resultTtlInSeconds: Integer
}

/// Defines a response and specifies parameter mappings.
@private
structure IntegrationResponse {
    /// HTTP status code for the method response; for example, "200". This must
    /// correspond to a matching response in the OpenAPI Operation responses
    /// field.
    statusCode: String
    contentHandling: ContentHandling
    responseTemplates: Templates
    responseParameters: ResponseParameters
}

list StringList {
    member: String
}

/// A map of response identifiers to responses.
@private
map IntegrationResponses {
    key: String
    value: IntegrationResponse
}

/// A mapping of integration request parameters to the API Gateway data
/// mapping expression that should be used to populate the parameter.
///
/// **Note:** This feature is provided primarily to allow injecting static
/// values and context variables for request parameters. Request data MAY be
/// mapped to headers using the syntax described in
/// [the API Gateway Developer Guide](https://docs.aws.amazon.com/apigateway/latest/developerguide/request-response-data-mappings.html#mapping-response-parameters);
/// however, the data must be identified according to its location in the
/// serialized request, which may differ from protocol to protocol. Here be
/// dragons!
@private
map RequestParameters {
    key: String
    value: String
}

/// Specifies parameter mappings for the response. Only the header and body
/// parameters of the integration response can be mapped to the header
/// parameters of the method.
@private
map ResponseParameters {
    key: String
    value: String
}

/// A map of MIME types to velocity templates allowing you to craft a new
/// integration message from received data.
///
/// **Note:** This feature is provided primarily to allow injecting static
/// values and context variables for request parameters. Request data MAY be
/// mapped to headers using the syntax described in
/// [the API Gateway Developer Guide](https://docs.aws.amazon.com/apigateway/latest/developerguide/request-response-data-mappings.html#mapping-response-parameters);
/// however, the data must be identified according to its location in the
/// serialized request, which may differ from protocol to protocol. Here be
/// dragons!
@private
map Templates {
    key: String
    value: String
}

/// The ARN of an AWS integration target.
///
/// This string MAY contain the literal string `{serviceName}` and/or the
/// literal string `{operationName}`, which will be replaced with the name of
/// the Smithy service shape and the name of the Smithy operation shape,
/// respectively.
@arnReference
@private
string Arn

enum ConnectionType {
    INTERNET
    VPC_LINK
}

/// Defines the contentHandling for the integration
@private
enum ContentHandling {
    CONVERT_TO_TEXT
    CONVERT_TO_BINARY
}

/// The ARN of the IAM role to assume with invoking the integration.
///
/// This string MAY contain the literal string `{serviceName}` and/or the
/// literal string `{operationName}`, which will be replaced with the name of
/// the Smithy service shape and the name of the Smithy operation shape,
/// respectively.
@arnReference(type: "AWS::IAM::Role")
@private
string IamRoleArn

enum IntegrationType {
    @enumValue("aws")
    AWS

    @enumValue("aws_proxy")
    AWS_PROXY

    @enumValue("http")
    HTTP

    @enumValue("http_proxy")
    HTTP_PROXY
}

/// Defines the passThroughBehavior for the integration
@private
enum PassThroughBehavior {
    @enumValue("when_no_templates")
    WHEN_NO_TEMPLATES

    @enumValue("when_no_match")
    WHEN_NO_MATCH

    @enumValue("never")
    NEVER
}
