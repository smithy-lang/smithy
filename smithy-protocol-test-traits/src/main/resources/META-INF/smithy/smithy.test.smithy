$version: "2.0"

namespace smithy.test

/// Define how an HTTP request is serialized given a specific protocol,
/// authentication scheme, and set of input parameters.
@trait(selector: "operation")
@length(min: 1)
list httpRequestTests {
    member: HttpRequestTestCase
}

@private
structure HttpRequestTestCase with [HttpRequestMixin] {
    /// The identifier of the test case. This identifier can be used by
    /// protocol test implementations to filter out unsupported test
    /// cases by ID, to generate test case names, etc. The provided `id`
    /// MUST match Smithy's `identifier` ABNF. No two `httpRequestTests`
    /// test cases can share the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String

    /// The name of the protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String

    /// The optional authentication scheme shape ID to assume. It's
    /// possible that specific authentication schemes might influence
    /// the serialization logic of an HTTP request.
    @idRef(selector: "[trait|authDefinition]", failWhenMissing: true)
    authScheme: String

    /// Defines the input parameters used to generated the HTTP request.
    ///
    /// These parameters MUST be compatible with the input of the operation.
    params: Document

    /// Defines vendor-specific parameters that are used to influence the
    /// request. For example, some vendors might utilize environment
    /// variables, configuration files on disk, or other means to influence
    /// the serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible
    /// with that shape's definition.
    vendorParams: Document

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(failWhenMissing: true)
    vendorParamsShape: String

    /// A description of the test and what is being asserted.
    documentation: String

    /// Applies a list of tags to the test.
    tags: NonEmptyStringList

    /// Indicates that the test case is only to be implemented by "client" or
    /// "server" implementations. This property is useful for identifying and
    /// testing edge cases of clients and servers that are impossible or
    /// undesirable to test in *both* client and server implementations.
    appliesTo: AppliesTo
}

@private
map StringMap {
    key: String
    value: String
}

@private
list StringList {
    member: String
}

/// Define how an HTTP response is serialized given a specific protocol,
/// authentication scheme, and set of output or error parameters.
@trait(selector: ":test(operation, structure[trait|error])")
@length(min: 1)
list httpResponseTests {
    member: HttpResponseTestCase
}

@private
structure HttpResponseTestCase with [HttpResponseMixin] {
    /// The identifier of the test case. This identifier can be used by
    /// protocol test implementations to filter out unsupported test
    /// cases by ID, to generate test case names, etc. The provided `id`
    /// MUST match Smithy's `identifier` ABNF. No two `httpResponseTests`
    /// test cases can share the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String

    /// The shape ID of the protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String

    /// The optional authentication scheme shape ID to assume. It's possible
    /// that specific authentication schemes might influence the serialization
    /// logic of an HTTP response.
    @idRef(selector: "[trait|authDefinition]", failWhenMissing: true)
    authScheme: String

    /// Defines the output parameters deserialized from the HTTP response.
    ///
    /// These parameters MUST be compatible with the output of the operation.
    params: Document

    /// Defines vendor-specific parameters that are used to influence the
    /// response. For example, some vendors might utilize environment
    /// variables, configuration files on disk, or other means to influence
    /// the serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible
    /// with that shape's definition.
    vendorParams: Document

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(failWhenMissing: true)
    vendorParamsShape: String

    /// A description of the test and what is being asserted.
    documentation: String

    /// Applies a list of tags to the test.
    tags: NonEmptyStringList

    /// Indicates that the test case is only to be implemented by "client" or
    /// "server" implementations. This property is useful for identifying and
    /// testing edge cases of clients and servers that are impossible or
    /// undesirable to test in *both* client and server implementations.
    appliesTo: AppliesTo
}

@private
list NonEmptyStringList {
    member: NonEmptyString
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
    id: String

    /// The name of the protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String

    /// The malformed request to send.
    @required
    request: HttpMalformedRequestDefinition

    /// The expected response.
    @required
    response: HttpMalformedResponseDefinition

    /// A description of the test and what is being asserted.
    documentation: String

    /// Applies a list of tags to the test.
    tags: NonEmptyStringList

    /// An optional set of test parameters for parameterized testing.
    testParameters: HttpMalformedRequestTestParametersDefinition
}

@private
structure HttpMalformedRequestDefinition {
    /// The HTTP request method.
    @required
    @length(min: 1)
    method: String

    /// The request-target of the HTTP request, not including
    /// the query string (for example, "/foo/bar").
    @required
    @length(min: 1)
    uri: String

    /// The host / endpoint provided to the client, not including the path
    /// or scheme (for example, "example.com").
    host: String

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
    queryParams: StringList

    /// Defines a map of HTTP headers to include in the request
    headers: StringMap

    /// The HTTP message body to include in the request
    body: String

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
    headers: StringMap

    /// Defines the HTTP response code.
    @required
    @range(min: 100, max: 599)
    code: Integer

    /// The expected response body.
    body: HttpMalformedResponseBodyDefinition
}

@private
structure HttpMalformedResponseBodyDefinition {
    /// The assertion to execute against the response body.
    @required
    assertion: HttpMalformedResponseBodyAssertion

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
    contents: String

    /// A regex to evaluate against the `message` field in the body. For
    /// responses that may have some variance from platform to platform,
    /// such as those that include messages from a parser.
    messageRegex: String
}

@private
map HttpMalformedRequestTestParametersDefinition {
    key: String
    value: StringList
}

/// Defines a list of protocol tests that enforce how an event stream
/// is serialized / deserialized for a specific protocol.
@trait(selector: "operation :test(-[input, output]-> structure > member > union[trait|streaming])")
@length(min: 1)
list eventStreamTests {
    member: EventStreamTestCase
}

/// A single event stream test case.
@private
structure EventStreamTestCase {
    /// The identifier of the test case. This identifier can be used by
    /// protocol test implementations to filter out unsupported test
    /// cases by ID, to generate test case names, etc. The provided `id`
    /// MUST match Smithy's `identifier` ABNF. No two test cases can share
    /// the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String

    /// The protocol to test.
    @required
    @idRef(selector: "[trait|protocolDefinition]", failWhenMissing: true)
    protocol: String

    /// The input parameters used to generate the initial request.
    ///
    /// These parameters MUST be compatible with the input shape of the operation.
    initialRequestParams: Document

    /// The protocol-specific initial request.
    ///
    /// If an `initialRequestShape` is set, this value MUST be compatible with that
    /// shape's definition.
    initialRequest: Document

    /// A shape to be used to validate the `initialRequest` member's contents.
    ///
    /// If set, the value in `initialRequest` MUST be compatible with this shape's
    /// definition.
    ///
    /// For HTTP protocols, use `smithy.test#InitialHttpRequest`
    @idRef(selector: "structure", failWhenMissing: true)
    initialRequestShape: String

    /// The output parameters used to generate the initial response.
    ///
    /// These parameters MUST be compatible with the output shape of the operation.
    initialResponseParams: Document

    /// The protocol-specific initial response.
    ///
    /// If an `initialResponseShape` is set, this value MUST be compatible with that
    /// shape's definition.
    initialResponse: Document

    /// A shape to be used to validate the `initialResponse` member's contents.
    ///
    /// If set, the value in `initialResponse` MUST be compatible with this shape's
    /// definition.
    ///
    /// For HTTP protocols, use `smithy.test#InitialHttpResponse`
    @idRef(selector: "structure", failWhenMissing: true)
    initialResponseShape: String

    /// A list of events to be sent over the event stream. This includes input message,
    /// output message, and error events.
    events: Events

    /// The kind of result that is expected from the event stream.
    ///
    /// If not set, the result is expected to be success.
    expectation: TestExpectation

    /// Defines vendor-specific parameters that are used to influence the
    /// request. For example, some vendors might utilize environment
    /// variables, configuration files on disk, or other means to influence
    /// the serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible
    /// with that shape's definition.
    vendorParams: Document

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(selector: "structure", failWhenMissing: true)
    vendorParamsShape: String

    /// A description of the test and what is being asserted.
    documentation: String

    /// Indicates that the test case is only to be implemented by "client" or
    /// "server" implementations. This property is useful for identifying and
    /// testing edge cases of clients and servers that are impossible or
    /// undesirable to test in *both* client and server implementations.
    appliesTo: AppliesTo
}

/// A structure defining http request shapes for initial requests.
structure InitialHttpRequest with [HttpRequestMixin] {}

/// A structure defining http response shapes for initial responses.
structure InitialHttpResponse with [HttpResponseMixin] {}

/// A list of events sent over the event stream.
@private
list Events {
    member: Event
}

/// An event sent over the event stream.
@private
structure Event {
    /// The type of event - request or response.
    @required
    type: EventType

    /// The parameters used to generate the event.
    ///
    /// If set, these parameters MUST be compatible with a modeled event.
    ///
    /// If not set, this event represents an unmodeled event.
    params: Document

    /// A map of expected headers.
    ///
    /// Headers that are not listed in this map are ignored unless they are
    /// explicitly forbidden through `forbidHeaders`.
    headers: EventHeaders

    /// A list of header field names that must not appear in the serialized
    /// event.
    forbidHeaders: StringList

    /// A list of header field names that must appear in the serialized
    /// event, but no assertion is made on the value.
    ///
    /// Headers listed in `headers` do not need to appear in this list.
    requireHeaders: StringList

    /// The expected event body.
    ///
    /// If no request body is defined, then no assertions are made about
    /// the body of the event.
    body: Blob

    /// The media type of the `body`.
    ///
    /// This is used to help test runners to parse and validate the expected
    /// data against generated data.
    bodyMediaType: String

    /// An optional binary representation of the entire event.
    ///
    /// This is used to test deserialization. If set, implementations SHOULD
    /// use this value to represent the binary value of received events rather
    /// than constructing that binary value from the other properties of the
    /// event.
    ///
    /// This value SHOULD NOT be used to make assertions about serialized
    /// events as such assertions likely would not be reliable. They would
    /// suffer from the same problems of making body assertions without a
    /// bodyMediaType where nonspecified ordering and optional whitespace
    /// can cause semantically equivalent values to have different bytes. This
    /// is made worse by headers having no defined order, and is likely made
    /// even worse by common event framing features such as checksums.
    bytes: Blob

    /// Defines vendor-specific parameters that are used to influence the
    /// request. For example, some vendors might utilize environment
    /// variables, configuration files on disk, or other means to influence
    /// the serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible
    /// with that shape's definition.
    vendorParams: Document = {}

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(selector: "structure", failWhenMissing: true)
    vendorParamsShape: String
}

/// The different types of event that are able to be sent over event streams.
@private
enum EventType {
    /// Indicates the event is a request message.
    REQUEST = "request"

    /// Indicates the event is a response message.
    RESPONSE = "response"
}

/// A map of event headers. The value is a union to indicate type,
/// but it MUST be serialized / deserialized without any sort of
/// wrapping from the union.
///
/// The union value type is needed to disambiguate types that the
/// Smithy Node would otherwise not be able to. This can't be a
/// simple map like for HTTP headers because HTTP headers only have
/// one value type, and the Smithy IDL isn't able to distinguish
/// between types like byte/short without some additional layer.
@private
map EventHeaders {
    key: String
    value: EventHeaderValue
}

/// A typed event header value. This is needed to disambiguate
/// types that the Smithy Node would otherwise not be able to.
@private
union EventHeaderValue {
    boolean: Boolean
    byte: Byte
    short: Short
    integer: Integer
    long: Long
    blob: Blob
    string: String
    timestamp: Timestamp
}

/// The different kinds of outcomes a test case may have.
@private
union TestExpectation {
    /// Indicates that the test should pass successfully.
    success: Unit

    /// Indicates that the test is expected to throw an exception.
    failure: TestFailureExpectation
}

/// Indicates that the test is expected to throw an error.
@private
structure TestFailureExpectation {
    /// If specified, the error must be of the targeted type.
    @idRef(failWhenMissing: true, selector: "[trait|error]")
    errorId: String
}

// The following mixins are shared mixins for HTTP protocol test traits.
/// A mixin that adds HTTP message members and supporting protocol test members,
/// suitable for both requests and responses.
@private
@mixin
structure HttpMessageMixin {
    /// Defines a map of expected HTTP headers.
    ///
    /// Headers that are not listed in this map are ignored unless they are
    /// explicitly forbidden through `forbidHeaders`.
    headers: StringMap

    /// A list of header field names that must not appear in the serialized
    /// HTTP request.
    forbidHeaders: StringList

    /// A list of header field names that must appear in the serialized
    /// HTTP message, but no assertion is made on the value.
    ///
    /// Headers listed in `headers` do not need to appear in this list.
    requireHeaders: StringList

    /// The expected HTTP message body.
    ///
    /// If no request body is defined, then no assertions are made about
    /// the body of the message.
    body: String

    /// The media type of the `body`.
    ///
    /// This is used to help test runners to parse and validate the expected
    /// data against generated data.
    bodyMediaType: String
}

/// A mixin that adds HTTP request members and supporting protocol test members.
@private
@mixin
structure HttpRequestMixin with [HttpMessageMixin] {
    /// The expected serialized HTTP request method.
    @required
    @length(min: 1)
    method: String

    /// The request-target of the HTTP request, not including
    /// the query string (for example, "/foo/bar").
    @required
    @length(min: 1)
    uri: String

    /// The host / endpoint provided to the client, not including the path
    /// or scheme (for example, "example.com").
    host: String

    /// The host / endpoint that the client should send to, not including
    /// the path or scheme (for example, "prefix.example.com").
    ///
    /// This can differ from the host provided to the client if the `hostPrefix`
    /// member of the `endpoint` trait is set, for instance.
    resolvedHost: String

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
    queryParams: StringList

    /// A list of query string parameter names that must not appear in the
    /// serialized HTTP request.
    ///
    /// Each value MUST appear in the format in which it is sent over the
    /// wire; if a key needs to be percent-encoded, then it MUST appear
    /// percent-encoded in this list.
    forbidQueryParams: StringList

    /// A list of query string parameter names that MUST appear in the
    /// serialized request URI, but no assertion is made on the value.
    ///
    /// Each value MUST appear in the format in which it is sent over the
    /// wire; if a key needs to be percent-encoded, then it MUST appear
    /// percent-encoded in this list.
    requireQueryParams: StringList
}

/// A mixin that adds HTTP response members and supporting protocol test members.
@private
@mixin
structure HttpResponseMixin with [HttpMessageMixin] {
    /// Defines the HTTP response code.
    @required
    @range(min: 100, max: 599)
    code: Integer
}
