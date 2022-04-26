$version: "2.0"

namespace aws.protocols

/// An RPC-based protocol that sends JSON payloads. This protocol does not use
/// HTTP binding traits.
@protocolDefinition(
    traits: [
        jsonName
        timestampFormat
        cors
        endpoint
        hostLabel
    ]
)
@trait(selector: "service")
structure awsJson1_0 {
    http: StringList

    eventStreamHttp: StringList
}

/// An RPC-based protocol that sends JSON payloads. This protocol does not use
/// HTTP binding traits.
@protocolDefinition(
    traits: [
        jsonName
        timestampFormat
        cors
        endpoint
        hostLabel
    ]
)
@trait(
    selector: "service"
)
structure awsJson1_1 {
    http: StringList
    eventStreamHttp: StringList
}

/// An RPC-based protocol that sends 'POST' requests in the body as
/// `x-www-form-urlencoded` strings and responses in XML documents. This
/// protocol does not use HTTP binding traits.
@deprecated
@protocolDefinition(
    noInlineDocumentSupport: true
    traits: [
        awsQueryError
        xmlAttribute
        xmlFlattened
        xmlName
        xmlNamespace
        timestampFormat
        cors
        endpoint
        hostLabel
    ]
)
@trait(selector: "service [trait|xmlNamespace]")
structure awsQuery {}

/// Provides the value in the 'Code' distinguishing field and HTTP response
/// code for an operation error.
@trait(
    selector: "structure [trait|error]",
    breakingChanges: [{change: "any"}]
)
structure awsQueryError {
    /// The value used to distinguish this error shape during serialization.
    @required
    code: String

    /// The HTTP response code used on a response containing this error shape.
    @required
    httpResponseCode: Integer
}

/// An RPC-based protocol that sends 'POST' requests in the body as Amazon EC2
/// formatted `x-www-form-urlencoded` strings and responses in XML documents.
/// This protocol does not use HTTP binding traits.
@deprecated
@protocolDefinition(
    noInlineDocumentSupport: true
    traits: [
        ec2QueryName
        xmlAttribute
        xmlFlattened
        xmlName
        xmlNamespace
        timestampFormat
        cors
        endpoint
        hostLabel
    ]
)
@trait(selector: "service [trait|xmlNamespace]")
structure ec2Query {}

/// Indicates the serialized name of a structure member when that structure is
/// serialized for the input of an EC2 operation.
@pattern("^[a-zA-Z_][a-zA-Z_0-9-]*$")
@trait(selector: "structure > member")
string ec2QueryName

/// Indicates that an operation supports checksum validation.
@trait(
    selector: "operation"
)
@unstable
structure httpChecksum {
    /// Defines a top-level operation input member that is used to configure
    /// request checksum behavior.
    requestAlgorithmMember: String

    /// Indicates an operation requires a checksum in its HTTP request.
    requestChecksumRequired: Boolean

    /// Defines a top-level operation input member used to opt-in to response
    /// checksum validation.
    requestValidationModeMember: String

    /// Defines the checksum algorithms clients should look for when performing
    /// HTTP response checksum validation.
    responseAlgorithms: ChecksumAlgorithmSet
}

/// A RESTful protocol that sends JSON in structured payloads.
@protocolDefinition(
    traits: [
        cors
        endpoint
        hostLabel
        http
        httpError
        httpHeader
        httpLabel
        httpPayload
        httpPrefixHeaders
        httpQuery
        httpQueryParams
        httpResponseCode
        jsonName
        timestampFormat
    ]
)
@trait(selector: "service")
structure restJson1 {
    http: StringList

    eventStreamHttp: StringList
}

/// A RESTful protocol that sends XML in structured payloads.
@deprecated
@protocolDefinition(
    noInlineDocumentSupport: true
    traits: [
        cors
        endpoint
        hostLabel
        http
        httpError
        httpHeader
        httpLabel
        httpPayload
        httpPrefixHeaders
        httpQuery
        httpQueryParams
        httpResponseCode
        xmlAttribute
        xmlFlattened
        xmlName
        xmlNamespace
    ]
)
@trait(selector: "service")
structure restXml {
    http: StringList

    eventStreamHttp: StringList

    /// Disables the serialization wrapping of error properties in an 'Error'
    /// XML element.
    @deprecated
    noErrorWrapping: Boolean
}

@private
list StringList {
    member: String
}

@length(min: 1)
@private
set ChecksumAlgorithmSet {
    member: ChecksumAlgorithm
}

@private
enum ChecksumAlgorithm {
    CRC32C

    CRC32

    SHA1

    SHA256
}
