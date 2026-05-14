$version: "2.0"

namespace ns.foo

@trait(selector: "service")
@protocolDefinition(traits: [
    smithy.api#cors
    smithy.api#endpoint
    smithy.api#hostLabel
    smithy.api#http
    smithy.api#httpError
    smithy.api#httpHeader
    smithy.api#httpLabel
    smithy.api#httpPayload
    smithy.api#httpPrefixHeaders
    smithy.api#httpQuery
    smithy.api#httpQueryParams
    smithy.api#httpResponseCode
    smithy.api#jsonName
    smithy.api#timestampFormat
])
@documentation("A simple clone of AWS restJson1.")
structure restProtocol {}

@restProtocol
service MyService {
    version: "2017-01-17"
    operations: [MissingBindings1, MissingBindings2]
}

@readonly
operation MissingBindings1 {
    input := {}
    output := {}
}

@readonly
operation MissingBindings2 {
    input := {}
    output := {}
}
