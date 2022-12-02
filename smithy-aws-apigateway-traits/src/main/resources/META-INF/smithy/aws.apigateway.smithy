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
    /// The type of integration with the specified backend.
    @required
    type: IntegrationType

    /// The endpoint URI of the backend. For integrations of the `aws` type,
    /// this is an ARN value. For the HTTP integration, this is the URL of the
    /// HTTP endpoint including the `https` or `http` scheme.
    @required
    uri: Arn

    /// Specifies the credentials required for the integration, if any. For AWS
    /// IAM role-based credentials, specify the ARN of an appropriate IAM role.
    /// If unspecified, credentials will default to resource-based permissions
    /// that must be added manually to allow the API to access the resource.
    credentials: IamRoleArn

    /// Specifies the integration's HTTP method type (for example, `POST`).
    /// For Lambda function invocations, the value must be `POST`.
    @required
    httpMethod: String

    /// Specifies how a request payload of unmapped content type is passed
    /// through the integration request without modification.
    passThroughBehavior: PassThroughBehavior

    /// Request payload content handling.
    contentHandling: ContentHandling

    /// Integration timeouts between 50 ms and 29,000 ms.
    timeoutInMillis: Integer

    /// The ID of a VpcLink for the private integration.
    connectionId: String

    /// The type of the network connection to the integration endpoint. The
    /// valid value is `INTERNET` for connections through the public routable
    /// internet or `VPC_LINK` for private connections between API Gateway and
    /// a network load balancer in a VPC. The default value is `INTERNET`.
    connectionType: ConnectionType = "INTERNET"

    /// An API-specific tag group of related cached parameters.
    cacheNamespace: String

    /// Specifies the format of the payload sent to an integration. Required
    /// for HTTP APIs. For HTTP APIs, supported values for Lambda proxy
    /// integrations are 1.0 and 2.0. For all other integrations, 1.0 is the
    /// only supported value.
    payloadFormatVersion: String

    /// A list of request parameter names whose values are to be cached.
    cacheKeyParameters: StringList

    /// Specifies mappings from method request parameters to integration
    /// request parameters.
    requestParameters: RequestParameters

    /// Mapping templates for a request payload of specified media types.
    requestTemplates: Templates

    /// Defines the method's responses and specifies desired parameter mappings
    /// or payload mappings from integration responses to method responses.
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
    /// Specifies how a request payload of unmapped content type is passed
    /// through the integration request without modification.
    passThroughBehavior: PassThroughBehavior

    /// Specifies mappings from method request parameters to integration
    /// request parameters.
    requestParameters: RequestParameters

    /// Mapping templates for a request payload of specified media types.
    requestTemplates: Templates

    /// Defines the method's responses and specifies desired parameter mappings
    /// or payload mappings from integration responses to method responses.
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

    /// Format version of the payload sent from API Gateway to the authorizer
    /// and how API Gateway interprets the response. Used only by HTTP APIs.
    authorizerPayloadFormatVersion: PayloadFormatVersion

    /// Specifies if the autorizer returns either a boolean or an IAM Policy.
    /// If enabled, authorizer returns a boolean. Used only by HTTP APIs.
    /// Only supported when authorizerPayloadFormatVersion is set to 2.0.
    enableSimpleResponses: Boolean
}

/// Defines a response and specifies parameter mappings.
@private
structure IntegrationResponse {
    /// HTTP status code for the method response; for example, "200". This must
    /// correspond to a matching response in the OpenAPI Operation responses
    /// field.
    statusCode: String

    /// Response payload content handling.
    contentHandling: ContentHandling

    /// Specifies media type-specific mapping templates for the response's
    /// payload.
    responseTemplates: Templates

    /// Specifies parameter mappings for the response. Only the header and
    /// body parameters of the integration response can be mapped to the header
    /// parameters of the method.
    responseParameters: ResponseParameters
}

@private
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

@private
enum ConnectionType {
    /// Connections through the public routable internet.
    INTERNET

    /// Private connections between API Gateway and a network load balancer in
    /// a VPC.
    VPC_LINK
}

/// Defines the contentHandling for the integration.
@private
enum ContentHandling {
    /// For converting a binary payload into a Base64-encoded string or
    /// converting a text payload into a utf-8-encoded string or passing
    /// through the text payload natively without modification
    CONVERT_TO_TEXT

    /// For converting a text payload into Base64-decoded blob or passing
    /// through a binary payload natively without modification.
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

@private
enum IntegrationType {
    /// An integration with AWS Lambda functions or other AWS services such as
    /// Amazon DynamoDB, Amazon Simple Notification Service or Amazon Simple
    /// Queue Service.
    AWS = "aws"

    /// An integration with AWS Lambda functions.
    AWS_PROXY = "aws_proxy"

    /// An integration with an HTTP backend.
    HTTP = "http"

    /// An integration with an HTTP backend.
    HTTP_PROXY = "http_proxy"
}

/// Defines the passThroughBehavior for the integration
@private
enum PassThroughBehavior {
    /// Passes the method request body through the integration request to the
    /// back end without transformation when no mapping template is defined in
    /// the integration request. If a template is defined when this option is
    /// selected, the method request of an unmapped content-type will be
    /// rejected with an HTTP 415 Unsupported Media Type response.
    WHEN_NO_TEMPLATES = "when_no_templates"

    /// Passes the method request body through the integration request to the
    /// back end without transformation when the method request content type
    /// does not match any content type associated with the mapping templates
    /// defined in the integration request.
    WHEN_NO_MATCH = "when_no_match"

    /// Rejects the method request with an HTTP 415 Unsupported Media Type
    /// response when either the method request content type does not match any
    /// content type associated with the mapping templates defined in the
    /// integration request or no mapping template is defined in the integration
    /// request.
    NEVER = "never"
}

/// Defines the payloadFormatVersion used by authorizers
@private
enum PayloadFormatVersion {
    /// Specifies 1.0 version of the format used by the authorizer
    V1_0 = "1.0"
    /// Specifies 2.0 version of the format used by the authorizer
    V2_0 = "2.0"
}
