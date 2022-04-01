$version: "2.0"

namespace smithy.api

string String

blob Blob

bigInteger BigInteger

bigDecimal BigDecimal

timestamp Timestamp

document Document

boolean Boolean

byte Byte

short Short

integer Integer

long Long

float Float

double Double

@unitType
structure Unit {}

/// Makes a shape a trait.
@trait(selector: ":is(simpleType, list, map, set, structure, union)")
@tags(["diff.error.add", "diff.error.remove"])
structure trait {
    /// The valid places in a model that the trait can be applied.
    selector: String,

    /// Whether or not only a single member in a shape can have this trait.
    structurallyExclusive: StructurallyExclusive,

    /// The traits that this trait conflicts with.
    conflicts: NonEmptyStringList,
}

@private
enum StructurallyExclusive {
    /// Only a single member of a shape can be marked with the trait.
    @enumValue("member")
    MEMBER

    /// Only a single member of a shape can target a shape marked with this trait.
    @enumValue("target")
    TARGET
}

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

/// Provides a link to additional documentation.
@trait
@length(min: 1)
map externalDocumentation {
    key: NonEmptyString,
    value: NonEmptyString
}

/// Defines the list of authentication schemes supported by a service or operation.
@trait(selector: ":is(service, operation)")
set auth {
    member: AuthTraitReference
}

/// A string that must target an auth trait.
@idRef(selector: "[trait|authDefinition]")
@private
string AuthTraitReference

/// Marks a trait as a protocol defining trait.
///
/// The targeted trait must only be applied to service shapes, must be a
/// structure, and must have the `trait` trait.
@trait(selector: "structure[trait|trait]")
@tags(["diff.error.add", "diff.error.remove"])
structure protocolDefinition {
    /// Defines a list of traits that protocol implementations must
    /// understand in order to successfully use the protocol.
    traits: TraitShapeIdList,

    /// Set to true if inline documents are not supported by this protocol.
    noInlineDocumentSupport: Boolean,
}

@private
list TraitShapeIdList {
    member: TraitShapeId,
}

@private
@idRef(failWhenMissing: true, selector: "[trait|trait]")
string TraitShapeId

/// Marks a trait as an auth scheme defining trait.
///
/// The targeted trait must only be applied to service shapes or operation
/// shapes, must be a structure, and must have the `trait` trait.
@trait(selector: "structure[trait|trait]")
@tags(["diff.error.add", "diff.error.remove"])
structure authDefinition {
    /// Defines a list of traits that auth implementations must
    /// understand in order to successfully use the scheme.
    traits: TraitShapeIdList,
}

/// Enables HTTP Basic Authentication as defined in RFC 2617
/// on a service or operation.
@trait(selector: "service")
@authDefinition
@externalDocumentation("RFC 2617": "https://tools.ietf.org/html/rfc2617.html")
structure httpBasicAuth {}

/// Enables HTTP Digest Authentication as defined in RFC 2617
/// on a service or operation.
@trait(selector: "service")
@authDefinition
@externalDocumentation("RFC 2617": "https://tools.ietf.org/html/rfc2617.html")
structure httpDigestAuth {}

/// Enables HTTP Bearer Authentication as defined in RFC 6750
/// on a service or operation.
@trait(selector: "service")
@authDefinition
@externalDocumentation("RFC 6750": "https://tools.ietf.org/html/rfc6750.html")
structure httpBearerAuth {}

/// An HTTP-specific authentication scheme that sends an arbitrary
/// API key in a header or query string parameter.
@trait(selector: "service")
@authDefinition
structure httpApiKeyAuth {
    /// Defines the name of the HTTP header or query string parameter
    /// that contains the API key.
    @required
    name: NonEmptyString,

    /// Defines the location of where the key is serialized. This value
    /// can be set to `"header"` or `"query"`.
    @required
    in: HttpApiKeyLocations,

    /// Defines the security scheme to use on the ``Authorization`` header value
    /// This can only be set if the "in" property is set to ``header``.
    scheme: NonEmptyString,
}

@trait(selector: "structure > member :not(> :test(union, structure > :test([trait|required])))",
       conflicts: [required])
@tags(["diff.error.remove"])
structure default {}

@private
enum HttpApiKeyLocations {
    @enumValue("header")
    HEADER

    @enumValue("query")
    QUERY
}

/// Indicates that an operation can be called without authentication.
@trait(selector: "operation")
structure optionalAuth {}

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

    output: Document,

    error: ExampleError,
}

@private
structure ExampleError {
    @idRef(selector: "structure[trait|error]")
    shapeId: String,

    content: Document,
}

/// Indicates that a structure shape represents an error.
///
/// All shapes referenced by the errors list of an operation MUST be
/// targeted with this trait.
@trait(selector: "structure", conflicts: [trait])
@tags(["diff.error.const"])
enum error {
    @enumValue("client")
    CLIENT

    @enumValue("server")
    SERVER
}

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
@trait(selector: "structure > :test(member > string)",
       structurallyExclusive: "member")
@tags(["diff.error.remove"])
structure idempotencyToken {}

/// Shapes marked with the internal trait are meant only for internal use and
/// must not be exposed to customers.
@trait
structure internal {}

/// The jsonName trait allows a serialized object property name to differ
/// from a structure member name used in the model.
@trait(selector: ":is(structure, union) > member")
@tags(["diff.error.const"])
string jsonName

/// Serializes an object property as an XML attribute rather than a nested XML element.
@trait(selector: "structure > :test(member > :test(boolean, number, string, timestamp))",
        conflicts: [xmlNamespace])
@tags(["diff.error.const"])
structure xmlAttribute {}

/// Unwraps the values of a list, set, or map into the containing structure/union.
@trait(selector: ":is(structure, union) > :test(member > :test(collection, map))")
@tags(["diff.error.const"])
structure xmlFlattened {}

/// Changes the serialized element or attribute name of a structure, union, or member.
@trait(selector: ":is(structure, union, member)")
@tags(["diff.error.const"])
@pattern("^[a-zA-Z_][a-zA-Z_0-9-]*(:[a-zA-Z_][a-zA-Z_0-9-]*)?$")
string xmlName

/// Adds an xmlns namespace definition URI to an XML element.
@trait(selector: ":is(service, member, simpleType, collection, map, structure, union)",
       conflicts: [xmlAttribute])
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

/// Indicates that the put lifecycle operation of a resource
/// can only be used to create a resource and cannot replace
/// an existing resource.
@trait(selector: "resource:test(-[put]->)")
structure noReplace {}

/// Describes the contents of a blob shape using a media type as defined by
/// RFC 6838 (e.g., "video/quicktime").
@trait(selector: ":is(blob, string)")
@tags(["diff.error.remove"])
string mediaType

/// Defines the resource shapes that are referenced by a string shape or a
/// structure shape and the members of the structure that provide values for
/// the identifiers of the resource.
@trait(selector: ":is(structure, string)")
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
@trait(selector: "structure > :test(member[trait|required] > string)")
@tags(["diff.error.remove"])
@length(min: 1)
string resourceIdentifier

/// Prevents models defined in a different namespace from referencing the targeted shape.
@trait
structure private {}

/// Indicates that the data stored in the shape is sensitive and MUST be handled with care.
@trait(selector: ":not(:test(service, operation, resource, member))")
structure sensitive {}

/// Defines the version or date in which a shape or member was added to the model.
@trait
string since

/// Indicates that the data stored in the shape is very large and should not
/// be stored in memory, or that the size of the data stored in the shape is
/// unknown at the start of a request. If the target is a union then the shape
/// represents a stream of events.
@trait(selector: ":is(blob, union)", structurallyExclusive: "target")
@tags(["diff.error.const"])
structure streaming {}

/// Indicates that the streaming blob must be finite and has a known size.
@trait(selector: "blob[trait|streaming]")
@tags(["diff.error.const"])
structure requiresLength {}

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
@trait(selector: ":is(service, resource)")
string title

/// Constrains the acceptable values of a string to a fixed set
/// of constant values.
@trait(selector: "string :not(enum)")
@tags(["diff.error.add", "diff.error.remove"])
@length(min: 1)
@deprecated
list enum {
    member: EnumDefinition
}

/// An enum definition for the enum trait.
@private
structure EnumDefinition {
    /// Defines the enum value that is sent over the wire.
    @required
    value: NonEmptyString,

    /// Defines the name, or label, that is used in code to represent this variant.
    name: EnumConstantBodyName,

    /// Provides optional documentation about the enum constant value.
    documentation: String,

    /// Applies a list of tags to the enum constant.
    tags: NonEmptyStringList,

    /// Whether the enum value should be considered deprecated.
    deprecated: Boolean,
}

/// The optional name or label of the enum constant value.
///
/// This property is used in code generation to provide a label for
/// each enum value. No two enums can have the same 'name' value.
@private
@pattern("^[a-zA-Z_]+[a-zA-Z_0-9]*$")
string EnumConstantBodyName

/// Defines the value of an enum member.
@trait(selector: ":is(enum, intEnum) > member")
@tags(["diff.error.const"])
document enumValue

/// Sets an enum member as the default value member.
@trait(
    selector: ":is(enum, intEnum) > member"
    structurallyExclusive: "member"
    conflicts: [enumValue]
)
@tags(["diff.error.const"])
structure enumDefault {}

/// Constrains a shape to minimum and maximum number of elements or size.
@trait(selector: ":test(collection, map, string, blob, member > :is(collection, map, string, blob))")
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
@trait(selector: "structure > member")
@tags(["diff.error.add"])
structure required {}

/// Configures a structure member's resource property mapping behavior
@trait(selector: "structure > member")
@tags(["diff.error.remove", "diff.contents"])
structure property {
    /// Remaps the expected resource property name to this configured one
    name: String
}

/// Marks a structure member as transient, which excludes it from any property mapping.
@trait(selector: "structure > member")
@tags(["diff.error.add"])
structure transient {}

/// Indicates that a structure member SHOULD be set.
@trait(selector: "structure > member", conflicts: [required])
structure recommended {
    /// Provides a reason why the member is recommended.
    reason: String,
}

/// Marks a list or map as sparse.
@trait(selector: ":is(list, map)")
@tags(["diff.error.const"])
structure sparse {}

/// Indicates that the items in a list MUST be unique.
@trait(selector: "list")
@deprecated(message: "The uniqueItems trait has been deprecated in favor of using sets.", since: "2.0")
structure uniqueItems {}

/// Indicates that the shape is unstable and could change in the future.
@trait()
structure unstable {}

/// The paginated trait indicates that an operation intentionally limits the number
/// of results returned in a single response and that multiple invocations might be
/// necessary to retrieve all results.
@trait(selector: ":is(service, operation)")
@tags(["diff.error.remove", "diff.contents"])
structure paginated {
    /// The name of the operation input member that represents the continuation token.
    ///
    /// When this value is provided as operation input, the service returns results
    /// from where the previous response left off. This input member MUST NOT be
    /// required and MUST target a string shape.
    @tags(["diff.error.update"])
    inputToken: NonEmptyString,

    /// The name of the operation output member that represents the continuation token.
    ///
    /// When this value is present in operation output, it indicates that there are more
    /// results to retrieve. To get the next page of results, the client uses the output
    /// token as the input token of the next request. This output member MUST NOT be
    /// required and MUST target a string shape.
    @tags(["diff.error.update"])
    outputToken: NonEmptyString,

    /// The name of a top-level output member of the operation that is the data
    /// that is being paginated across many responses.
    ///
    /// The named output member, if specified, MUST target a list or map.
    @tags(["diff.error.const"])
    items: NonEmptyString,

    /// The name of an operation input member that limits the maximum number of
    /// results to include in the operation output. This input member MUST NOT be
    /// required and MUST target an integer shape.
    @tags(["diff.error.update", "diff.error.remove"])
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
    @range(min: 100, max: 999)
    code: Integer,
}

/// Binds an operation input structure member to an HTTP label.
@trait(selector: "structure > member[trait|required] :test(> :test(string, number, boolean, timestamp))",
        conflicts: [httpHeader, httpQuery, httpPrefixHeaders, httpPayload, httpResponseCode, httpQueryParams])
@tags(["diff.error.const"])
structure httpLabel {}

/// Binds an operation input structure member to a query string parameter.
@trait(selector: """
        structure > member
        :test(> :test(string, number, boolean, timestamp),
              > collection > member > :test(string, number, boolean, timestamp))""",
        conflicts: [httpLabel, httpHeader, httpPrefixHeaders, httpPayload, httpResponseCode, httpQueryParams])
@length(min: 1)
@tags(["diff.error.const"])
string httpQuery

/// Binds an operation input structure member to the HTTP query string.
@trait(selector: """
        structure > member
        :test(> map > member[id|member=value] > :test(string, collection > member > string))""",
        structurallyExclusive: "member",
        conflicts: [httpLabel, httpQuery, httpHeader, httpPayload, httpResponseCode, httpPrefixHeaders])
@tags(["diff.error.const"])
structure httpQueryParams {}

/// Binds a structure member to an HTTP header.
@trait(selector: """
        structure > :test(member > :test(boolean, number, string, timestamp,
                collection > member > :test(boolean, number, string, timestamp)))""",
        conflicts: [httpLabel, httpQuery, httpPrefixHeaders, httpPayload, httpResponseCode, httpQueryParams])
@length(min: 1)
@tags(["diff.error.const"])
string httpHeader

/// Binds a map of key-value pairs to prefixed HTTP headers.
@trait(selector: """
        structure > member
        :test(> map > member[id|member=value] > string)""",
        structurallyExclusive: "member",
        conflicts: [httpLabel, httpQuery, httpHeader, httpPayload, httpResponseCode, httpQueryParams])
@tags(["diff.error.const"])
string httpPrefixHeaders

/// Binds a single structure member to the body of an HTTP request.
@trait(selector: "structure > :test(member > :test(string, blob, structure, union, document, list, set, map))",
        conflicts: [httpLabel, httpQuery, httpHeader, httpPrefixHeaders, httpResponseCode, httpQueryParams],
        structurallyExclusive: "member")
@tags(["diff.error.const"])
structure httpPayload {}

/// Defines an HTTP response code for an operation error.
@trait(selector: "structure[trait|error]")
@tags(["diff.error.const"])
integer httpError

/// Indicates that the structure member represents the HTTP response
/// status code. The value MAY differ from the HTTP status code provided
/// on the response.
@trait(selector: "structure > member :test(> integer)",
        structurallyExclusive: "member",
        conflicts: [httpLabel, httpQuery, httpHeader, httpPrefixHeaders, httpPayload, httpQueryParams])
@tags(["diff.error.const"])
structure httpResponseCode {}

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
@trait(selector: "structure > :test(member > :test(blob, string, structure, union))",
        conflicts: [eventHeader],
        structurallyExclusive: "member")
@tags(["diff.error.const"])
structure eventPayload {}

/// Marks a member as a header of an event.
@trait(selector: """
        structure >
        :test(member > :test(boolean, byte, short, integer, long, blob, string, timestamp))""",
        conflicts: [eventPayload])
@tags(["diff.error.const"])
structure eventHeader {}

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
    failWhenMissing: Boolean,

    /// Defines a custom error message to use when the shape ID cannot be
    /// found or does not match the selector.
    ///
    /// A default message is generated when errorMessage is not defined.
    errorMessage: String,
}

@trait(selector: ":test(timestamp, member > timestamp)")
@tags(["diff.error.const"])
enum timestampFormat {

    /// Date time as defined by the date-time production in RFC3339 section 5.6
    /// with no UTC offset (for example, 1985-04-12T23:20:50.52Z).
    @enumValue("date-time")
    DATE_TIME

    /// Also known as Unix time, the number of seconds that have elapsed since
    /// 00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970,
    /// with decimal precision (for example, 1515531081.1234).
    @enumValue("epoch-seconds")
    EPOCH_SECONDS

    /// An HTTP date as defined by the IMF-fixdate production in
    /// RFC 7231#section-7.1.1.1 (for example, Tue, 29 Apr 2014 18:30:38 GMT).
    @enumValue("http-date")
    HTTP_DATE
}

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
@trait(selector: "structure > :test(member[trait|required] > string)")
@tags(["diff.error.const"])
structure hostLabel {}

/// Suppresses validation events by ID for a given shape.
@trait
list suppress {
    @pattern("^[_a-zA-Z][A-Za-z0-9]*$")
    @length(min: 1)
    member: String
}

/// Marks an operation as requiring checksum in its HTTP request.
/// By default, the checksum used for a service is a MD5 checksum
/// passed in the Content-MD5 header.
@unstable
@trait(selector: "operation")
structure httpChecksumRequired {}

/// Specializes a structure for use only as the input of a single operation.
@trait(selector: "structure", conflicts: [output, error])
@tags(["diff.error.const"])
structure input {}

/// Specializes a structure for use only as the output of a single operation.
@trait(selector: "structure", conflicts: [input, error])
@tags(["diff.error.const"])
structure output {}

/// Specializes a structure as a unit type that has no meaningful value.
/// This trait can only be applied to smithy.api#Unit, which ensures that
/// only a single Unit shape can be created.
@trait(selector: "[id=smithy.api#Unit]")
structure unitType {}

/// Makes a structure or union a mixin.
@trait(selector: ":not(member)")
structure mixin {
    localTraits: LocalMixinTraitList
}

@private
list LocalMixinTraitList {
    member: LocalMixinTrait
}

@idRef(
    selector: "[trait|trait]",
    failWhenMissing: true,
    errorMessage: """
            Strings provided to the localTraits property of a mixin trait
            must target a valid trait.""")
@private
string LocalMixinTrait
