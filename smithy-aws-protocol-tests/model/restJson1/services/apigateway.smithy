$version: "2.0"

namespace com.amazonaws.apigateway

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.test#httpRequestTests

@service(
    sdkId: "API Gateway",
    arnNamespace: "apigateway",
    cloudFormationName: "ApiGateway",
    cloudTrailEventSource: "apigateway.amazonaws.com",
    endpointPrefix: "apigateway"
)
@sigv4(
    name: "apigateway",
)
@restJson1
@title("Amazon API Gateway")
service BackplaneControlService {
    version: "2015-07-09",
    operations: [
        GetRestApis,
    ],
}


@httpRequestTests([
    {
        id: "ApiGatewayAccept",
        documentation: "API Gateway requires that this Accept header is set on all requests.",
        protocol: restJson1,
        method: "GET",
        uri: "/restapis",
        headers: {
            "Accept": "application/json",
        },
        body: "",
        params: {},
    }
])
@http(
    method: "GET",
    uri: "/restapis",
    code: 200,
)
@paginated(
    inputToken: "position",
    outputToken: "position",
    items: "items",
    pageSize: "limit",
)
@readonly
operation GetRestApis {
    input: GetRestApisRequest,
    output: RestApis,
    errors: [
        BadRequestException,
        TooManyRequestsException,
        UnauthorizedException,
    ],
}

@error("client")
@httpError(400)
structure BadRequestException {
    message: String,
}

structure EndpointConfiguration {
    types: ListOfEndpointType,
    vpcEndpointIds: ListOfString,
}

structure GetRestApisRequest {
    @httpQuery("position")
    position: String,

    @httpQuery("limit")
    limit: NullableInteger,
}

structure RestApi {
    id: String,
    name: String,
    description: String,
    createdDate: Timestamp,
    version: String,
    warnings: ListOfString,
    binaryMediaTypes: ListOfString,
    minimumCompressionSize: NullableInteger,
    apiKeySource: ApiKeySourceType,
    endpointConfiguration: EndpointConfiguration,
    policy: String,
    tags: MapOfStringToString,
    disableExecuteApiEndpoint: Boolean,
}

structure RestApis {
    @jsonName("item")
    items: ListOfRestApi,

    position: String,
}

@error("client")
@httpError(429)
structure TooManyRequestsException {
    @httpHeader("Retry-After")
    retryAfterSeconds: String,
    message: String,
}

@error("client")
@httpError(401)
structure UnauthorizedException {
    message: String,
}

list ListOfEndpointType {
    member: EndpointType,
}

list ListOfString {
    member: String,
}

list ListOfRestApi {
    member: RestApi,
}

map MapOfStringToString {
    key: String,
    value: String,
}

enum ApiKeySourceType {
    HEADER
    AUTHORIZER
}

boolean Boolean

enum EndpointType {
    REGIONAL
    EDGE
    PRIVATE
}

integer NullableInteger

string String

timestamp Timestamp
