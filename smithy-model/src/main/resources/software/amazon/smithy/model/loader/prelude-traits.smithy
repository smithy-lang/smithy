$version: "0.4.0"

namespace smithy.api

/// Makes a shape a trait.
@trait(selector: ":each(simpleType, list, map, set, structure, union)")
@tags(["diff.error.add", "diff.error.remove"])
structure trait {
    /// The valid places in a model that the trait can be applied.
    selector: String,

    /// Whether or not only a single member in a structure can have this trait.
    structurallyExclusive: PrimitiveBoolean,

    /// The traits that this trait conflicts with.
    conflicts: NonEmptyStringList,
}

/// Indicates that a shape is boxed.
///
/// When a boxed shape is the target of a member, the member
/// may or may not contain a value, and the member has no default value.
@trait(selector: """
    :test(boolean, byte, short, integer, long, float, double,
          member > :test(boolean, byte, short, integer, long, float, double))""")
@tags(["diff.error.const"])
structure box {}

/// Marks a shape or member as deprecated.
@trait
structure deprecated {
    /// The reason for deprecation.
    message: String,

    /// A description of when the shape was deprecated (e.g., a date or version).
    since: String,
}

/// Adds documentation to a shape or member using CommonMark syntax.
@trait
string documentation

/// Defines the protocols supported by a service in priority order.
@trait(selector: "service")
list protocols {
    member: Protocol,
}

@private
structure Protocol {
    /// The name that identifies the protocol.
    ///
    /// This name must be unique across the entire list.
    @required
    name: ProtocolOrAuthName,

    /// Attaches a list of tags that allow protocols to be categorized and grouped.
    tags: NonEmptyStringList,

    /// A priority ordered list of authentication schemes used with this protocol.
    auth: AuthenticationSchemes,
}

@private
@uniqueItems
list AuthenticationSchemes {
    member: ProtocolOrAuthName
}

@pattern("^[a-z][a-z0-9\\-.+]*$")
@private
string ProtocolOrAuthName

/// Defines the authentication schemes supported by a service or operation.
@trait(selector: ":test(service, operation)")
@uniqueItems
list auth {
    member: ProtocolOrAuthName
}

/// Provides a link to additional documentation.
@trait
string externalDocumentation

/// Provides example inputs and outputs for operations.
@trait(selector: "operation")
list examples {
    member: Example,
}

@private
structure Example {
    @required
    title: String,

    documentation: String,

    input: Document,

    output: Document
}

/// Indicates that a structure shape represents an error.
///
/// All shapes referenced by the errors list of an operation MUST be
/// targeted with this trait.
@trait(selector: "structure", conflicts: [trait])
@tags(["diff.error.const"])
@enum(client: {name: "CLIENT"},
      server: {name: "SERVER"})
string error

/// Indicates that an error MAY be retried by the client.
@trait(selector: "structure[trait|error]")
structure retryable {
    /// Classifies the retry as throttling.
    throttling: Boolean,
}

/// Indicates that an operation is effectively read-only.
@trait(selector: "operation", conflicts: [idempotent])
structure readonly {}

/// Indicates that the intended effect on the server of multiple identical
/// requests with an operation is the same as the effect for a single
/// such request.
@trait(selector: "operation", conflicts: [readonly])
@tags(["diff.error.remove"])
structure idempotent {}

/// Defines the input member of an operation that is used by the server to
/// identify and discard replayed requests.
@trait(selector: ":test(member:of(structure) > string)",
       structurallyExclusive: true)
structure idempotencyToken {}

/// The jsonName trait allows a serialized object property name to differ
/// from a structure member name used in the model.
@trait(selector: "member:of(structure)")
@tags(["diff.error.const"])
string jsonName

/// Moves a serialized object property to an attribute of the enclosing structure.
@trait(selector: ":test(member:of(structure) > :test(boolean, number, string, timestamp))",
       conflicts: ["xmlNamespace"])
@tags(["diff.error.const"])
structure xmlAttribute {}

/// Moves serialized collection members from their collection element to that of
/// the collection's container.
@trait(selector: ":test(map, collection, member:of(structure) > :test(map, collection))")
@tags(["diff.error.const"])
structure xmlFlattened {}

/// Allows a serialized object property name to differ from a structure member name
/// used in the model.
@trait
@tags(["diff.error.const"])
@pattern("^[a-zA-Z_][a-zA-Z_0-9-]*(:[a-zA-Z_][a-zA-Z_0-9-]*)?$")
string xmlName

/// Adds an xmlns namespace definition URI to an XML element.
@trait(conflicts: ["xmlAttribute"])
@tags(["diff.error.const"])
structure xmlNamespace {
    /// The namespace URI for scoping this XML element.
    @required
    uri: NonEmptyString,
    /// The prefix for the given namespace.
    @pattern("^[a-zA-Z_][a-zA-Z_0-9-]*$")
    prefix: NonEmptyString,
}

@private
@length(min: 1)
string NonEmptyString

/// Describes the contents of a blob shape using a media type as defined by
/// RFC 6838 (e.g., "video/quicktime").
@trait(selector: ":each(blob, string)")
@tags(["diff.error.remove"])
string mediaType

/// Defines the resource shapes that are referenced by a string shape or a
/// structure shape and the members of the structure that provide values for
/// the identifiers of the resource.
@trait(selector: ":test(structure, string)")
list references {
    member: Reference
}

@private
structure Reference {
    /// The shape ID of the referenced resource.
    @required
    resource: NonEmptyString,

    /// Defines a mapping of each resource identifier name to a structure member
    /// name that provides its value. Each key in the map MUST refer to one of the
    /// identifier names in the identifiers property of the resource, and each
    /// value in the map MUST refer to a valid structure member name that targets
    /// a string shape.
    ids: NonEmptyStringMap,

    /// Providing a service makes the reference specific to a particular binding
    /// of the resource to a service. When omitted, the reference is late-bound to
    /// a service, meaning the reference is assumed to be a reference to the
    /// resource bound to the service currently in use by the client or server.
    service: NonEmptyString,

    /// Defines the semantics of the relationship. The rel property SHOULD
    /// contain a link relation as defined in RFC 5988#section-4.
    rel: NonEmptyString,
}

@private
map NonEmptyStringMap {
    key: NonEmptyString,
    value: NonEmptyString
}

/// Indicates that the targeted structure member provides an identifier for a resource.
@trait(selector: ":test(member:of(structure)[trait|required] > string)")
@tags(["diff.error.remove"])
@length(min: 1)
string resourceIdentifier

/// Prevents models defined in a different namespace from referencing the targeted shape.
@trait
structure private {}

/// Indicates that the data stored in the shape or member is sensitive and MUST be handled with care.
@trait(selector: ":not(:test(service, operation, resource))")
structure sensitive {}

/// Defines the version or date in which a shape or member was added to the model.
@trait
string since

/// Indicates that the the data stored in the shape is very large and should not
/// be stored in memory, or that the size of the data stored in the shape is
/// unknown at the start of a request
@trait(selector: ":each(operation -[input, output]-> structure > :test(member > blob), :test(member:of(structure[trait|error]) > blob))")
@tags(["diff.error.const"])
structure streaming {
    /// Indicates that the stream must have a known size.
    requiresLength: Boolean,
}

/// Tags a shape with arbitrary tag names that can be used to filter and
/// group shapes in the model.
@trait
list tags {
    member: String
}

/// Defines a proper name for a service or resource shape.
///
/// This title can be used in automatically generated documentation
/// and other contexts to provide a user friendly name for services
/// and resources.
@trait(selector: ":test(service, operation)")
string title

/// Constrains the acceptable values of a string to a fixed set
/// of constant values.
@trait(selector: "string")
@tags(["diff.error.add", "diff.error.remove"])
map enum {
    key: String,
    value: EnumConstantBody
}

/// An enum definition for the enum trait.
@private
structure EnumConstantBody {
    /// Provides optional documentation about the enum constant value.
    documentation: String,

    /// Applies a list of tags to the enum constant.
    tags: NonEmptyStringList,

    name: EnumConstantBodyName,
}

/// The optional name or label of the enum constant value.
///
/// This property is used in code generation to provide a label for
/// each enum value. No two enums can have the same 'name' value.
@private
@pattern("^[a-zA-Z_]+[a-zA-Z_0-9]*$")
string EnumConstantBodyName

/// Constrains a shape to minimum and maximum number of elements or size.
@trait(selector: ":test(collection, map, string, blob, member > :each(collection, map, string, blob))")
structure length {
    /// Integer value that represents the minimum inclusive length of a shape.
    min: Long,

    /// Integer value that represents the maximum inclusive length of a shape.
    max: Long,
}

/// Restricts allowed values of byte, short, integer, long, float, double,
/// bigDecimal, and bigInteger shapes within an acceptable lower and upper bound.
@trait(selector: ":test(number, member > number)")
structure range {
    /// Specifies the allowed inclusive minimum value.
    min: BigDecimal,

    /// Specifies the allowed inclusive maximum value.
    max: BigDecimal,
}

/// Restricts string shape values to a specified regular expression.
@trait(selector: ":test(string, member > string)")
string pattern

/// Marks a structure member as required, meaning a value for the member MUST be present.
@trait(selector: "member:of(structure)")
@tags(["diff.error.add"])
structure required {}

/// Indicates that the items in a list MUST be unique.
@trait(selector: "list")
structure uniqueItems {}

/// The paginated trait indicates that an operation intentionally limits the number
/// of results returned in a single response and that multiple invocations might be
/// necessary to retrieve all results.
@trait(selector: ":each(service, operation)")
@tags(["diff.error.remove"])
structure paginated {
    /// The name of the operation input member that represents the continuation token.
    ///
    /// When this value is provided as operation input, the service returns results
    /// from where the previous response left off. This input member MUST NOT be
    /// required and MUST target a string shape.
    inputToken: NonEmptyString,

    /// The name of the operation output member that represents the continuation token.
    ///
    /// When this value is present in operation output, it indicates that there are more
    /// results to retrieve. To get the next page of results, the client uses the output
    /// token as the input token of the next request. This output member MUST NOT be
    /// required and MUST target a string shape.
    outputToken: NonEmptyString,

    /// The name of a top-level output member of the operation that is the data
    /// that is being paginated across many responses.
    ///
    /// The named output member, if specified, MUST target a list or map.
    items: NonEmptyString,

    /// The name of an operation input member that limits the maximum number of
    /// results to include in the operation output. This input member MUST NOT be
    /// required and MUST target an integer shape.
    pageSize: NonEmptyString,
}

/// Configures the HTTP bindings of an operation.
@trait(selector: "operation")
@tags(["diff.error.remove"])
structure http {
    /// The HTTP method of the operation.
    @required
    method: NonEmptyString,

    /// The URI pattern of the operation.
    ///
    /// Labels defined in the URI pattern are used to bind operation input
    /// members to the URI.
    @required
    uri: NonEmptyString,

    /// The HTTP status code of a successful response.
    ///
    /// Defaults to 200 if not provided.
    code: PrimitiveInteger,
}

/// Binds an operation input structure member to an HTTP label.
@trait(selector: ":test(member:of(structure) > :test(string, number, boolean, timestamp))",
       conflicts: [httpHeader, httpQuery, httpPrefixHeaders, httpPayload])
@tags(["diff.error.const"])
structure httpLabel {}

/// Binds an operation input structure member to a query string parameter.
@trait(selector: ":test(member:of(structure) > :test(simpleType, collection > member > simpleType))",
       conflicts: [httpLabel, httpHeader, httpPrefixHeaders, httpPayload])
@length(min: 1)
@tags(["diff.error.const"])
string httpQuery

/// Binds a structure member to an HTTP header.
@trait(selector: ":test(member:of(structure) > :test(boolean, number, string, timestamp, collection > member > :test(boolean, number, string, timestamp)))",
       conflicts: [httpLabel, httpQuery, httpPrefixHeaders, httpPayload])
@length(min: 1)
@tags(["diff.error.const"])
string httpHeader

/// Binds a map of key-value pairs to prefixed HTTP headers.
@trait(selector: ":test(member:of(structure) > map > member[id|member=value] > :test(simpleType, collection > member > simpleType))",
       structurallyExclusive: true,
       conflicts: [httpLabel, httpQuery, httpHeader, httpPayload])
@tags(["diff.error.const"])
string httpPrefixHeaders

/// Binds a single structure member to the body of an HTTP request.
@trait(selector: ":test(member:of(structure) > :test(string, blob, structure, union))",
       conflicts: [httpLabel, httpQuery, httpHeader, httpPrefixHeaders],
       structurallyExclusive: true)
@tags(["diff.error.const"])
structure httpPayload {}

/// Defines an HTTP response code for an operation error.
@trait(selector: "structure[trait|error]")
@tags(["diff.error.const"])
integer httpError

/// Defines how a service supports cross-origin resource sharing.
@trait(selector: "service")
@tags(["diff.error.remove"])
structure cors {
    /// The origin from which browser script-originating requests will be allowed.
    ///
    /// Defaults to *.
    origin: NonEmptyString,

    /// The maximum number of seconds for which browsers are allowed to cache
    /// the results of a preflight OPTIONS request.
    ///
    /// Defaults to 600, the maximum age permitted by several browsers.
    /// Set to -1 to disable caching entirely.
    maxAge: Integer,

    /// The names of headers that should be included in the
    /// Access-Control-Allow-Headers header in responses to preflight OPTIONS
    /// requests. This list will be used in addition to the names of all
    /// request headers bound to an input data member via the httpHeader, as
    /// well as any headers required by the protocol or authentication scheme.
    additionalAllowedHeaders: NonEmptyStringList,

    /// The names of headers that should be included in the
    /// Access-Control-Expose-Headers header in all responses sent by the
    /// service. This list will be used in addition to the names of all
    /// request headers bound to an output data member via the httpHeader,
    /// as well as any headers required by the protocol or authentication
    /// scheme.
    additionalExposedHeaders: NonEmptyStringList,
}

@private
list NonEmptyStringList {
    member: NonEmptyString,
}

/// Marks a member as the payload of an event.
@trait(selector: "member:of(structure):test(> :each(blob, string, structure, union))",
       conflicts: [eventHeader],
       structurallyExclusive: true)
@tags(["diff.error.const"])
structure eventPayload {}

/// Marks a member as a header of an event.
@trait(selector: "member:of(structure):test( > :each(boolean, byte, short, integer, long, blob, string, timestamp))",
       conflicts: [eventPayload])
@tags(["diff.error.const"])
structure eventHeader {}

/// Binds an input or output member as an event stream.
/// The targeted member must be targeted by the input or output of
/// an operation, and must target a union or structure. The
/// targeted member must not be marked as required.
@trait(selector: "operation -[input, output]-> structure > :test(member > :each(structure, union))",
       structurallyExclusive: true,
       conflicts: [required])
@tags(["diff.error.const"])
structure eventStream {}

/// Indicates that a string value MUST contain a valid shape ID.
///
/// The provided shape ID MAY be absolute or relative to the shape to which
/// the trait is applied. A relative shape ID that does not resolve to a
/// shape defined in the same namespace resolves to a shape defined in the
/// prelude if the prelude shape is not marked with the private trait.
@trait(selector: ":test(string, member > string)")
structure idRef {
    /// Defines the selector that the resolved shape, if found, MUST match.
    ///
    /// selector defaults to * when not defined.
    selector: String,

    /// When set to `true`, the shape ID MUST target a shape that can be
    /// found in the model.
    failWhenMissing: PrimitiveBoolean,

    /// Defines a custom error message to use when the shape ID cannot be
    /// found or does not match the selector.
    ///
    /// A default message is generated when errorMessage is not defined.
    errorMessage: String,
}

@trait(selector: ":test(timestamp, member > timestamp)")
@tags(["diff.error.const"])
@enum(
    "date-time": {
        documentation: """
            Date time as defined by the date-time production in RFC3339 section 5.6
            with no UTC offset (for example, 1985-04-12T23:20:50.52Z)."""
    },
    "epoch-seconds": {
        documentation: """
            Also known as Unix time, the number of seconds that have elapsed since
            00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970,
            with decimal precision (for example, 1515531081.1234)."""
    },
    "http-date": {
        documentation: """
            An HTTP date as defined by the IMF-fixdate production in
            RFC 7231#section-7.1.1.1 (for example, Tue, 29 Apr 2014 18:30:38 GMT)."""
    })
string timestampFormat

/// Configures a custom operation endpoint.
@trait(selector: "operation")
@tags(["diff.error.const"])
structure endpoint {
    /// A host prefix pattern for the operation.
    ///
    /// Labels defined in the host pattern are used to bind top-level
    /// operation input members to the host.
    @required
    hostPrefix: NonEmptyString,
}

/// Binds a top-level operation input structure member to a label
/// in the hostPrefix of an endpoint trait.
@trait(selector: ":test(member:of(structure)[trait|required] > string)")
@tags(["diff.error.const"])
structure hostLabel {}
