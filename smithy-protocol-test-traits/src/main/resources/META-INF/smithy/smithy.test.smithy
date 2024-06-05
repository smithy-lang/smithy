$version: "2.0"

namespace smithy.test

/// Define how an HTTP request is serialized given a specific protocol,
/// authentication scheme, and set of input parameters.
@trait(selector: "operation")
@length(min: 1)
list httpRequestTests {
    member: HttpRequestTestCase,
}

@private
structure HttpRequestTestCase {
    /// The identifier of the test case. This identifier can be used by
    /// protocol test implementations to filter out unsupported test
    /// cases by ID, to generate test case names, etc. The provided `id`
    /// MUST match Smithy's `identifier` ABNF. No two `httpRequestTests`
    /// test cases can share the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String,

    /// The name of the protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String,

    /// The expected serialized HTTP request method.
    @required
    @length(min: 1)
    method: String,

    /// The request-target of the HTTP request, not including
    /// the query string (for example, "/foo/bar").
    @required
    @length(min: 1)
    uri: String,

    /// The host / endpoint provided to the client, not including the path
    /// or scheme (for example, "example.com").
    host: String,

    /// The host / endpoint that the client should send to, not including
    /// the path or scheme (for example, "prefix.example.com").
    ///
    /// This can differ from the host provided to the client if the `hostPrefix`
    /// member of the `endpoint` trait is set, for instance.
    resolvedHost: String,

    /// The optional authentication scheme shape ID to assume. It's
    /// possible that specific authentication schemes might influence
    /// the serialization logic of an HTTP request.
    @idRef(selector: "[trait|authDefinition]", failWhenMissing: true)
    authScheme: String,

    /// A list of the expected serialized query string parameters.
    ///
    /// Each element in the list is a query string key value pair
    /// that starts with the query string parameter name optionally
    /// followed by "=", optionally followed by the query string
    /// parameter value. For example, "foo=bar", "foo=", and "foo"
    /// are all valid values. The query string parameter name and
    /// the value MUST appear in the format in which it is expected
    /// to be sent over the wire; if a key or value needs to be
    /// percent-encoded, then it MUST appear percent-encoded in this list.
    ///
    /// A serialized HTTP request is not in compliance with the protocol
    /// if any query string parameter defined in `queryParams` is not
    /// defined in the request or if the value of a query string parameter
    /// in the request differs from the expected value.
    ///
    /// `queryParams` applies no constraints on additional query parameters.
    queryParams: StringList,

    /// A list of query string parameter names that must not appear in the
    /// serialized HTTP request.
    ///
    /// Each value MUST appear in the format in which it is sent over the
    /// wire; if a key needs to be percent-encoded, then it MUST appear
    /// percent-encoded in this list.
    forbidQueryParams: StringList,

    /// A list of query string parameter names that MUST appear in the
    /// serialized request URI, but no assertion is made on the value.
    ///
    /// Each value MUST appear in the format in which it is sent over the
    /// wire; if a key needs to be percent-encoded, then it MUST appear
    /// percent-encoded in this list.
    requireQueryParams: StringList,

    /// Defines a map of expected HTTP headers.
    ///
    /// Headers that are not listed in this map are ignored unless they are
    /// explicitly forbidden through `forbidHeaders`.
    headers: StringMap,

    /// A list of header field names that must not appear in the serialized
    /// HTTP request.
    forbidHeaders: StringList,

    /// A list of header field names that must appear in the serialized
    /// HTTP message, but no assertion is made on the value.
    ///
    /// Headers listed in `headers` do not need to appear in this list.
    requireHeaders: StringList,

    /// The expected HTTP message body.
    ///
    /// If no request body is defined, then no assertions are made about
    /// the body of the message.
    body: String,

    /// The media type of the `body`.
    ///
    /// This is used to help test runners to parse and validate the expected
    /// data against generated data.
    bodyMediaType: String,

    /// Defines the input parameters used to generated the HTTP request.
    ///
    /// These parameters MUST be compatible with the input of the operation.
    params: Document,

    /// Defines vendor-specific parameters that are used to influence the
    /// request. For example, some vendors might utilize environment
    /// variables, configuration files on disk, or other means to influence
    /// the serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible
    /// with that shape's definition.
    vendorParams: Document,

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(failWhenMissing: true)
    vendorParamsShape: String,

    /// A description of the test and what is being asserted.
    documentation: String,

    /// Applies a list of tags to the test.
    tags: NonEmptyStringList,

    /// Indicates that the test case is only to be implemented by "client" or
    /// "server" implementations. This property is useful for identifying and
    /// testing edge cases of clients and servers that are impossible or
    /// undesirable to test in *both* client and server implementations.
    appliesTo: AppliesTo,
}

@private
map StringMap {
    key: String,
    value: String,
}

@private
list StringList {
    member: String,
}

/// Define how an HTTP response is serialized given a specific protocol,
/// authentication scheme, and set of output or error parameters.
@trait(selector: ":test(operation, structure[trait|error])")
@length(min: 1)
list httpResponseTests {
    member: HttpResponseTestCase,
}

@private
structure HttpResponseTestCase {
    /// The identifier of the test case. This identifier can be used by
    /// protocol test implementations to filter out unsupported test
    /// cases by ID, to generate test case names, etc. The provided `id`
    /// MUST match Smithy's `identifier` ABNF. No two `httpResponseTests`
    /// test cases can share the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String,

    /// The shape ID of the protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String,

    /// Defines the HTTP response code.
    @required
    @range(min: 100, max: 599)
    code: Integer,

    /// The optional authentication scheme shape ID to assume. It's possible
    /// that specific authentication schemes might influence the serialization
    /// logic of an HTTP response.
    @idRef(selector: "[trait|authDefinition]", failWhenMissing: true)
    authScheme: String,

    /// A map of expected HTTP headers. Each key represents a header field
    /// name and each value represents the expected header value. An HTTP
    /// response is not in compliance with the protocol if any listed header
    /// is missing from the serialized response or if the expected header
    /// value differs from the serialized response value.
    ///
    /// `headers` applies no constraints on additional headers.
    headers: StringMap,

    /// A list of header field names that must not appear.
    forbidHeaders: StringList,

    /// A list of header field names that must appear in the serialized
    /// HTTP message, but no assertion is made on the value.
    ///
    /// Headers listed in `headers` map do not need to appear in this list.
    requireHeaders: StringList,

    /// Defines the HTTP message body.
    ///
    /// If no response body is defined, then no assertions are made about
    /// the body of the message.
    body: String,

    /// The media type of the `body`.
    ///
    /// This is used to help test runners to parse and validate the expected
    /// data against generated data. Binary media type formats require that
    /// the contents of `body` are base64 encoded.
    bodyMediaType: String,

    /// Defines the output parameters deserialized from the HTTP response.
    ///
    /// These parameters MUST be compatible with the output of the operation.
    params: Document,

    /// Defines vendor-specific parameters that are used to influence the
    /// response. For example, some vendors might utilize environment
    /// variables, configuration files on disk, or other means to influence
    /// the serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible
    /// with that shape's definition.
    vendorParams: Document,

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(failWhenMissing: true)
    vendorParamsShape: String,

    /// A description of the test and what is being asserted.
    documentation: String,

    /// Applies a list of tags to the test.
    tags: NonEmptyStringList,

    /// Indicates that the test case is only to be implemented by "client" or
    /// "server" implementations. This property is useful for identifying and
    /// testing edge cases of clients and servers that are impossible or
    /// undesirable to test in *both* client and server implementations.
    appliesTo: AppliesTo,
}

@private
list NonEmptyStringList {
    member: NonEmptyString,
}

@private
@length(min: 1)
string NonEmptyString

@private
enum AppliesTo {
    /// The test only applies to client implementations.
    @enumValue("client")
    CLIENT

    /// The test only applies to server implementations.
    @enumValue("server")
    SERVER
}

/// Define how a malformed HTTP request is rejected by a server given a specific protocol
@trait(selector: "operation")
@length(min: 1)
@unstable
list httpMalformedRequestTests {
    member: HttpMalformedRequestTestCase
}

@private
structure HttpMalformedRequestTestCase {
    /// The identifier of the test case. This identifier can be used by
    /// protocol test implementations to filter out unsupported test
    /// cases by ID, to generate test case names, etc. The provided `id`
    /// MUST match Smithy's `identifier` ABNF. No two `httpMalformedRequestTests`
    /// test cases can share the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String,

    /// The name of the protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String,

    /// The malformed request to send.
    @required
    request: HttpMalformedRequestDefinition,

    /// The expected response.
    @required
    response: HttpMalformedResponseDefinition,

    /// A description of the test and what is being asserted.
    documentation: String,

    /// Applies a list of tags to the test.
    tags: NonEmptyStringList,

    /// An optional set of test parameters for parameterized testing.
    testParameters: HttpMalformedRequestTestParametersDefinition,
}

@private
structure HttpMalformedRequestDefinition {

    /// The HTTP request method.
    @required
    @length(min: 1)
    method: String,

    /// The request-target of the HTTP request, not including
    /// the query string (for example, "/foo/bar").
    @required
    @length(min: 1)
    uri: String,

    /// The host / endpoint provided to the client, not including the path
    /// or scheme (for example, "example.com").
    host: String,

    /// A list of the serialized query string parameters to include in the request.
    ///
    /// Each element in the list is a query string key value pair
    /// that starts with the query string parameter name optionally
    /// followed by "=", optionally followed by the query string
    /// parameter value. For example, "foo=bar", "foo=", and "foo"
    /// are all valid values. The query string parameter name and
    /// the value MUST appear in the format in which it is expected
    /// to be sent over the wire; if a key or value needs to be
    /// percent-encoded, then it MUST appear percent-encoded in this list.
    queryParams: StringList,

    /// Defines a map of HTTP headers to include in the request
    headers: StringMap,

    /// The HTTP message body to include in the request
    body: String,

    /// The media type of the `body`.
    ///
    /// This is used to help test runners to parse and validate the expected
    /// data against generated data.
    bodyMediaType: String
}

@private
structure HttpMalformedResponseDefinition {

    /// Defines a map of expected HTTP headers.
    ///
    /// Headers that are not listed in this map are ignored.
    headers: StringMap,

    /// Defines the HTTP response code.
    @required
    @range(min: 100, max: 599)
    code: Integer,

    /// The expected response body.
    body: HttpMalformedResponseBodyDefinition,
}

@private
structure HttpMalformedResponseBodyDefinition {
    /// The assertion to execute against the response body.
    @required
    assertion: HttpMalformedResponseBodyAssertion,

    /// The media type of the response body.
    ///
    /// This is used to help test runners to parse and evaluate
    /// `contents' and `messageRegex` in the assertion
    @required
    mediaType: String
}

@private
union HttpMalformedResponseBodyAssertion {
    /// Defines the expected serialized response body, which will be matched
    /// exactly.
    contents: String,

    /// A regex to evaluate against the `message` field in the body. For
    /// responses that may have some variance from platform to platform,
    /// such as those that include messages from a parser.
    messageRegex: String
}

@private
map HttpMalformedRequestTestParametersDefinition {
    key: String,
    value: StringList
}
