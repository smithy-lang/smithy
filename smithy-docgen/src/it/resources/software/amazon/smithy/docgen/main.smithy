$version: "2.0"

metadata suppressions = [
    {
        id: "UnstableTrait"
        namespace: "com.example"
        reason: "These are used for examples."
    }
    {
        id: "DeprecatedTrait"
        namespace: "com.example"
        reason: "These are used for examples."
    }
]

namespace com.example

use aws.protocols#awsJson1_0
use aws.protocols#restJson1
use aws.protocols#restXml

/// This is a sample service meant to exercise and demonstrate different kinds
/// of behavior that the documentation generator should handle. Click around to
/// different pages to see what it can do. Examples for various traits are
/// put into sections matching their sections in the Smithy docs.
///
/// This service uses many different auth and protocol traits to demonstrate
/// how that will look, though in practice most services will only use one
/// or two of each. One important thing to note is the ordering of the
/// documented auth traits. By default it uses the same ordering as the
/// [auth trait](https://smithy.io/2.0/spec/authentication-traits.html#auth-trait)
/// if present. Anything not in that list is added to the end. On this service,
/// the only auth trait not present in the list is `httpBasicAuth`.
///
/// For the most part, this doesn't go to any effort to pretend to be a real
/// service, operations and paramters will be named according to what they're
/// demonstrating rather than according to some kind of theme.
///
/// One feature the implementation <b>must</b> support, for example, is the
/// ability to handle HTML tags in the documentation trait. This is because the
/// documentation trait uses the [CommonMark](https://spec.commonmark.org/)
/// format, which supports inline HTML.
@title("Documented Service")
@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
@httpApiKeyAuth(name: "auth-bearing-header", in: "header", scheme: "Bearer")
@auth([httpApiKeyAuth, httpBearerAuth, httpDigestAuth])
@awsJson1_0
@restJson1
@restXml
service DocumentedService {
    version: "2023-10-13"
    operations: [
        ConstraintTraits
        TypeRefinementTraits
        DocumentationTraits
        BehaviorTraits
        DefaultAuth
        Unauthenticated
        OptionalAuth
        LimitedAuth
        LimitedOptionalAuth
        ProtocolTraits
        StreamingTraits
        HttpTraits
        EndpointTraits
        Misc
        ExternalReference
    ]
    resources: [
        DocumentationResource
    ]
    errors: [
        ClientError
        ServiceError
    ]
}

/// This operation showcases most of the various
/// [constraint traits](https://smithy.io/2.0/spec/constraint-traits.html).
///
/// Note that the `idref` trait isn't supported since it's only used for traits
/// and doesn't reflect the API. The `private` trait similarly doesn't have
/// any impact on the API.
@http(method: "POST", uri: "/ConstraintTraits")
operation ConstraintTraits with [AllAuth] {
    input := {
        lengthExamples: LengthTraitExamples

        /// This member has a pattern trait on it.
        @pattern("^[A-Za-z]+$")
        pattern: String

        rangeExamples: RangeTraitExamples

        uniqueItems: StringSet

        enum: StringEnum

        intEnum: IntEnum
    }
}

/// This shows how the length trait is applied to various types.
structure LengthTraitExamples {
    @length(min: 4)
    string: String

    @length(max: 255)
    blob: Blob

    @length(min: 2, max: 4)
    list: StringSet

    map: StringMap
}

/// A set of strings, using the
/// [uniqueItems](https://smithy.io/2.0/spec/constraint-traits.html#uniqueitems-trait)
/// trait to make the list effectively and ordered set.
@uniqueItems
list StringSet {
    member: String
}

/// A string map that allows null values.
@length(max: 16)
map StringMap {
    key: String
    value: String
}

/// This shows how the range trait is applied to various types.
structure RangeTraitExamples {
    @range(min: 0)
    positive: Integer

    @range(max: 0)
    negative: Long

    @range(min: 0, max: 255)
    unsignedByte: Short
}

/// This in an enum that can have one of the following values:
///
/// - `FOO`
/// - `BAR`
///
/// Like other shapes in the model, this doesn't actually mean anything.
enum StringEnum {
    /// One of the more common placeholders in the programming world.
    FOO

    /// Another very common placeholder, often seen with `foo`.
    BAR
}

/// This in an enum that can have one of the following values:
///
/// - `SPAM`: `1`
/// - `EGGS`: `2`
///
/// Like other shapes in the model, this doesn't actually mean anything.
intEnum IntEnum {
    /// The spam and eggs placeholders are really only common in Python code bases.
    SPAM = 1

    /// They're a reference to a famous Monty Python skit, which is fitting because the
    /// language itself is named after Monty Python.
    EGGS = 2
}

/// This operation showcases the various
/// [type refinement traits](https://smithy.io/2.0/spec/type-refinement-traits.html).
///
/// Note that `input` and `output` traits have the effect of moving the shape's
/// docs into the operation page with no dedicated shape page since it can't be
/// otherwise referenced.
///
/// The `mixin` trait has no effect on the API, so it's not documented. The generator
/// will skip generating anything for them.
@http(method: "POST", uri: "/TypeRefinementTraits")
operation TypeRefinementTraits with [AllAuth] {
    input := {
        /// A boolean with a default value.
        defaultBoolean: Boolean = true

        /// An enum with a default value.
        defaultEnum: StringEnum = "FOO"

        /// An integer with a default that was addded after initial publication
        /// and was annotated with the
        /// [addedDefault trait](https://smithy.io/2.0/spec/type-refinement-traits.html#addeddefault-trait).
        /// Currently that trait doesn't impact the docs.
        @addedDefault
        addedDefault: Integer = 5

        /// A member that's required. Note that many of the resource operations
        /// and http bindings will also have required traits since they need
        /// them.
        @required
        required: String

        /// This member is optional for clients. Since this is API
        /// documentation, that's not reflected here.
        @clientOptional
        @required
        clientOptional: String

        enumValue: NonMatchingEnum
    }

    output := {
        sparseList: SparseList
        sparseMap: SparseMap
    }

    errors: [
        SimpleError
    ]
}

/// This string enum has values that don't match the member names.
enum NonMatchingEnum {
    /// Note that the actual wire value is lower-case.
    FOO = "foo"

    /// Note that the actual wire value is completely different.
    BAR = "example"
}

/// A list that allows null values.
@sparse
list SparseList {
    member: String
}

/// A map that allows null values.
@sparse
map SparseMap {
    key: String
    value: String
}

/// This error is as basic as the protocols allow.
structure SimpleError with [ErrorMixin] {}

/// This operation showcases the various
/// [documentation traits](https://smithy.io/2.0/spec/documentation-traits.html).
///
/// Note that examples are only half-supported right now. An interface needs
/// to be created and implemented to allow both code generators and protocols
/// to provide examples that will be used in those sections. For now, it just
/// shows the raw input / output structures.
///
/// The `title` trait is applied to the service.
@examples([
    {
        title: "Basic Example"
        input: {
            deprecated: { deprecatedSince: "foo" }
        }
        output: {}
    }
    {
        title: "Error example"
        documentation: "This shows an error response example."
        input: {
            deprecated: { deprecatedMessage: "bar" }
        }
        error: {
            shapeId: SimpleError
            content: { message: "That's super deprecated, don't use it." }
        }
    }
])
@http(method: "POST", uri: "/DocumentationTraits")
operation DocumentationTraits with [AllAuth] {
    input := {
        noDocumentationTrait: Long

        deprecated: DeprecatedExamples

        /// While you can link things directly inside of the doc trait, you
        /// can also used the external docs trait to provide see-also type
        /// links.
        @externalDocumentation("trait docs": "https://smithy.io/2.0/spec/documentation-traits.html#externaldocumentation-trait")
        externalDocumentation: String

        /// This is distinct from required.
        @recommended
        recommended: String

        /// This has its own custom reason for why it's recommended.
        @recommended(reason: "Because you can.")
        reasonablyRecommended: String

        /// Sensitive data could be anything, but it shouldn't be logged.
        sensitive: SensitiveBlob

        /// This indicates when it was added.
        @since("2020-03-01")
        since: Integer

        /// This has a number of tags, though said tags aren't part of the docs
        /// and likely won't be. They're mostly useful for organizing the model
        /// itself.
        @tags(["foo", "bar"])
        tags: Short
    }

    output := {
        /// This integer being unstable could mean later it becomes an enum, or
        /// a different number type, or evern be removed.
        @unstable
        unstable: Integer
    }

    errors: [
        SimpleError
    ]
}

/// This showcases the different ways a shape can be marked as deprecated.
@deprecated
structure DeprecatedExamples {
    @deprecated
    deprecated: String

    @deprecated(since: "2020-03-01")
    deprecatedSince: String

    @deprecated(message: "This gets a custom  message.")
    deprecatedMessage: String

    @deprecated(since: "2020-03-01", message: "This gets a custom message too.")
    fullDeprecated: String
}

@sensitive
blob SensitiveBlob

/// This operation showcases the various
/// [behavior traits](https://smithy.io/2.0/spec/behavior-traits.html).
///
/// See the various read operations in the resource examples for an
/// example of the `readonly` trait. Similarly, see the list operations for
/// examples of pagination.
@requestCompression(
    encodings: ["gzip"]
)
@idempotent
@http(method: "POST", uri: "/BehaviorTraits")
operation BehaviorTraits with [AllAuth] {
    input := {
        /// This token is still required for services.
        @idempotencyToken
        idempotencyToken: String
    }

    errors: [
        RetryableError
    ]
}

/// An error that's explicitly retryable.
@retryable(throttling: true)
@error("server")
@httpError(500)
structure RetryableError with [ErrorMixin] {}

// This mixin provides all of the auth types available to the service, in the
// default expected priority. In effect, this hides the auth annotations that
// will otherwise appear since the service does not list all of its auth types
// in its auth trait list.
@mixin
@auth([httpApiKeyAuth, httpBearerAuth, httpDigestAuth, httpBasicAuth])
operation AllAuth {}

/// This operation uses the service's default auth list. Since that doesn't
/// include all of the possible auth types, this should display information
/// indicating what this operation supports.
///
/// You may wonder why it's like this, and why the auth information doesn't
/// just show up when the operation's list differs from what is on the
/// service's auth list. The answer is that auth documenation is inherently
/// a top level affair, so all of the possible auth needs to be documented at
/// the top level. So when an operation doesn't support all the possible auth
/// types, that has to be called out.
@http(method: "POST", uri: "/DefaultAuth")
operation DefaultAuth {}

/// This operation does not support any of the service's auth types.
@auth([])
@http(method: "POST", uri: "/Unauthenticated")
operation Unauthenticated {}

/// This operation supports all of the service's auth types, but optionally.
@optionalAuth
@http(method: "POST", uri: "/OptionalAuth")
operation OptionalAuth with [AllAuth] {}

/// This operation supports a limited set of the service's auth.
@auth([httpBasicAuth, httpApiKeyAuth])
@http(method: "POST", uri: "/LimitedAuth")
operation LimitedAuth {}

/// This operation supports a limited set of the service's auth, optionally.
@optionalAuth
@auth([httpBasicAuth, httpDigestAuth])
@http(method: "POST", uri: "/LimitedOptionalAuth")
operation LimitedOptionalAuth {}

/// This operation showcases the various
/// [serialization and protocol traits](https://smithy.io/2.0/spec/protocol-traits.html).
@http(method: "POST", uri: "/ProtocolTraits")
operation ProtocolTraits with [AllAuth] {
    input := {
        /// This has a name representation in JSON that differs from the member name.
        @jsonName("spam")
        jsonName: String

        /// This targets a shape with a media type.
        mediaType: VideoData

        /// This is a timestamp with a custom format.
        timestamp: DateTime

        /// This is a timestamp without a custom format.
        plainTimestamp: Timestamp

        xmlTraits: XmlTraits

        /// This member has different traits for the different protocols.
        @jsonName("foo")
        @xmlName("bar")
        differentProtocols: String
    }
}

@mediaType("video/quicktime")
blob VideoData

/// Timestamp in RFC3339 format
@timestampFormat("date-time")
timestamp DateTime

/// This structure showcases various XML traits
@xmlName("foo")
structure XmlTraits {
    /// This shows that the xml name isn't inherited from the target.
    nested: XmlTraits

    /// This shows an xml name targeting a normal shape.
    @xmlName("bar")
    xmlName: String

    /// This shows how xml attributes are displayed.
    @xmlAttribute
    xmlAttribute: String

    /// This list uses the default nesting behavior.
    nestedList: StringList

    /// This list uses the non-default flat list behavior.
    @xmlFlattened
    flatList: StringList

    /// This map uses the default nesting behavior.
    nestedMap: StringMap

    /// This map uses the non-default flat map behavior.
    @xmlFlattened
    flatMap: StringMap

    /// This string tag needs an xml namespace added to it.
    @xmlNamespace(prefix: "example", uri: "https://example.com")
    xmlNamespace: String
}

list StringList {
    member: String
}

/// This showcases the `streaming` trait with a data stream.
@http(method: "POST", uri: "/StreamingTraits")
operation StreamingTraits with [AllAuth] {
    input := {
        @httpPayload
        output: StreamingBlob = ""
    }
}

/// This is a streaming blob.
@streaming
@requiresLength
blob StreamingBlob

// TODO: add event stream traits
/// This operation showcases most of the various
/// [HTTP traits](https://smithy.io/2.0/spec/http-bindings.html).
@http(method: "POST", uri: "/HttpTraits/{label}/{greedyLabel+}?static", code: 200)
@httpChecksumRequired
operation HttpTraits with [AllAuth] {
    input := {
        /// This is a label member that's bound to a normal label.
        @httpLabel
        @required
        label: String

        /// This is a label member that's bound to a greedy label.
        @httpLabel
        @required
        greedyLabel: String

        /// This is a header member bound to a single static header.
        @httpHeader("x-custom-header")
        singletonHeader: String

        /// This is a header member that's bound to a list.
        @httpHeader("x-list-header")
        listHeader: StringList

        /// This is a header member that's bound to a map with a prefix.
        @httpPrefixHeaders("prefix-")
        prefixHeaders: StringMap

        /// This is a query param that's bound to a single param.
        @httpQuery("singelton")
        singletonQuery: String

        /// This is a query param that's bound to a list.
        @httpQuery("list")
        listQuery: StringList

        /// This is an open listing of all query params.
        @httpQueryParams
        mapQuery: StringMap

        /// This is the operation's payload, only useable since everything
        /// else is bound to some other part of the HTTP request.
        @httpPayload
        payload: Blob
    }

    output := {
        /// This allows people to more easily interact with the http response
        /// without having to leak the response object.
        @httpResponseCode
        responseCode: Integer
    }
}

/// The endpoint traits are currently not supported.
@endpoint(hostPrefix: "{foo}.data.")
@http(method: "POST", uri: "/EndpointTraits")
operation EndpointTraits with [AllAuth] {
    input := {
        /// This will be sent both as part of the endpoint prefix and in the
        /// message body.
        @required
        @hostLabel
        foo: String
    }
}

/// This operation showcases anything that doesn't fit cleanly into one of the
/// other showcase operations.
@http(method: "POST", uri: "/Misc")
operation Misc with [AllAuth] {
    input := {
        union: DocumentedUnion
    }
}

/// Unions can only have one member set. The member name is used as a tag to
/// determine which member is intended at runtime.
union DocumentedUnion {
    /// Union members for the most part look like structure members, with the exception
    /// that exactly one must be set.
    string: String

    /// It doesn't matter that multiple members target the same type, since the type
    /// isn't the discriminator, the tag (member name) is.
    otherString: String
}

@mixin
@error("client")
@httpError(400)
structure ErrorMixin {
    /// The wire-level error identifier.
    code: String

    /// A message with details about why the error happened.
    message: String
}

/// This is an error that is the fault of the calling client.
structure ClientError with [ErrorMixin] {}

/// This is an error that is the fault of the service.
@error("server")
@httpError(500)
structure ServiceError with [ErrorMixin] {}

/// This operation references a resource shape that isn't contained within this
/// model, and so generating a reference link to it requires configuring the
/// `references` setting of the generator.
@http(method: "POST", uri: "/ExternalReference")
operation ExternalReference {
    input := {
        /// A structure that contains the identifiers for the external resource.
        externalReference: ExternalResourceReference
    }
}

/// This is a non-input, non-output structure that contains references, in this
/// case to an external resource.
@references([
    {
        resource: "com.example#ExternalResource"
        rel: "help"
    }
])
structure ExternalResourceReference {
    /// This is the actual identifier for the external resource.
    externalResourceId: String
}

/// A resource shape. To have some sense of readability this will represent the concept
/// of documentation itself as a resource, presenting the image of a service which
/// stores such things.
@noReplace
resource DocumentationResource {
    identifiers: {
        id: DocumentationId
    }
    properties: {
        contents: DocumentationContents
        archived: DocumentationArchived
    }
    put: PutDocs
    create: CreateDocs
    read: GetDocs
    update: UpdateDocs
    delete: DeleteDocs
    list: ListDocs
    operations: [
        ArchiveDocs
    ]
    collectionOperations: [
        DeleteArchivedDocs
    ]
    resources: [
        DocumentationArtifact
    ]
}

/// The identifier for the documentation resoruce.
///
/// These properites and identifiers all have their own shapes to enable documentation
/// sharing, not necessarily because they have meaningful collections of constraints
/// or other wire-level traits.
@references([
    {
        resource: DocumentationResource
    }
])
string DocumentationId

/// The actual body of the documentation.
string DocumentationContents

/// Whether or not the documentation has been archived. This could mean that changes
/// are rejected, for example.
boolean DocumentationArchived

/// Put operations create a resource with a user-specified identifier.
@idempotent
@http(method: "PUT", uri: "/DocumentationResource/{id}")
operation PutDocs with [AllAuth] {
    input := for DocumentationResource {
        @required
        @httpLabel
        $id

        @required
        $contents
    }
}

/// Create operations instead have the service create the identifier.
@http(method: "POST", uri: "/DocumentationResource")
operation CreateDocs with [AllAuth] {
    input := for DocumentationResource {
        @required
        $contents
    }
}

/// Gets the contents of a documentation resource.
@readonly
@http(method: "GET", uri: "/DocumentationResource/{id}")
operation GetDocs with [AllAuth] {
    input := for DocumentationResource {
        @required
        @httpLabel
        $id
    }

    output := for DocumentationResource {
        @required
        $id

        @required
        $contents

        @required
        $archived
    }
}

/// Does an update on the documentation resource. These can often also be the put
/// lifecycle operation.
@idempotent
@http(method: "PUT", uri: "/DocumentationResource")
operation UpdateDocs with [AllAuth] {
    input := for DocumentationResource {
        @required
        @httpQuery("id")
        $id

        @required
        $contents
    }
}

/// Deletes documentation.
@idempotent
@http(method: "DELETE", uri: "/DocumentationResource/{id}")
operation DeleteDocs with [AllAuth] {
    input := for DocumentationResource {
        @required
        @httpLabel
        $id
    }
}

/// Archives documentation. This is here to be a non-lifecycle instance operation.
/// We need both instance operations and collection operations that aren't lifecycle
/// operations to make sure both cases are being documented.
@idempotent
@http(method: "PUT", uri: "/DocumentationResource/{id}/archive")
operation ArchiveDocs with [AllAuth] {
    input := for DocumentationResource {
        @required
        @httpLabel
        $id
    }
}

/// Deletes all documentation that's been archived. This is a collection operation that
/// isn't part of a lifecycle operation, which is again needed to make sure everything
/// is being documented as expected.
@http(method: "DELETE", uri: "/DocumentationResource?delete-archived")
@idempotent
operation DeleteArchivedDocs with [AllAuth] {}

/// Lists the avialable documentation resources.
@readonly
@http(method: "GET", uri: "/DocumentationResource")
@paginated(inputToken: "paginationToken", outputToken: "paginationToken", items: "documentation", pageSize: "pageSize")
operation ListDocs with [AllAuth] {
    input := {
        /// Whether to list documentation that has been archived.
        @httpQuery("showArchived")
        showArchived: Boolean = false

        @httpHeader("x-example-pagination-token")
        paginationToken: String

        @httpHeader("x-example-page-size")
        pageSize: Integer
    }

    output := {
        @required
        documentation: DocumentationList

        paginationToken: String
    }
}

list DocumentationList {
    member: Documentation
}

/// A concrete documentation resource instance.
structure Documentation for DocumentationResource {
    @required
    $id

    @required
    $contents

    @required
    $archived
}

/// This would be something like a built PDF.
resource DocumentationArtifact {
    identifiers: {
        id: DocumentationId
        artifactId: DocumentationArtifactId
    }
    properties: {
        data: DocumentationArtifactData
    }
    put: PutDocArtifact
    read: GetDocArtifact
    delete: DeleteDocArtifact
}

/// Sub-resources need distinct identifiers.
string DocumentationArtifactId

@mixin
@references([
    {
        resource: DocumentationArtifact
    }
])
structure DocArtifactRef for DocumentationArtifact {
    $id
    $artifactId
}

/// This would be the bytes containing the artifact
blob DocumentationArtifactData

@idempotent
@http(method: "PUT", uri: "/DocumentationResource/{id}/artifact/{artifactId}")
operation PutDocArtifact with [AllAuth] {
    input := for DocumentationArtifact with [DocArtifactRef] {
        @required
        @httpLabel
        $id

        @required
        @httpLabel
        $artifactId

        @required
        $data
    }
}

@readonly
@http(method: "GET", uri: "/DocumentationResource/{id}/artifact/{artifactId}")
operation GetDocArtifact with [AllAuth] {
    input := for DocumentationArtifact with [DocArtifactRef] {
        @required
        @httpLabel
        $id

        @required
        @httpLabel
        $artifactId
    }

    output := for DocumentationArtifact with [DocArtifactRef] {
        @required
        $id

        @required
        $artifactId

        @required
        $data
    }
}

@idempotent
@http(method: "DELETE", uri: "/DocumentationResource/{id}/artifact/{artifactId}")
operation DeleteDocArtifact with [AllAuth] {
    input := for DocumentationArtifact with [DocArtifactRef] {
        @required
        @httpLabel
        $id

        @required
        @httpLabel
        $artifactId
    }
}
