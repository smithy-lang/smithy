$version: "2.0"

namespace smithy.example

// The severity of HttpPayload.UnboundTrailer depends on the protocols of the
// services that bind the operation: it's a WARNING only if there is at least
// one protocol and every protocol lists the trailer trait.
@trait(selector: "service")
@protocolDefinition(traits: [trailer, http, httpHeader, httpPayload])
structure supportsTrailers {}

@trait(selector: "service")
@protocolDefinition(traits: [http, httpHeader, httpPayload])
structure noTrailers {}

// The only protocol lists the trailer trait, so Supported and the
// resource-bound GetFile get a WARNING.
@supportsTrailers
service SupportingService {
    version: "2024-01-01"
    operations: [Supported, Shared, CommonA, WithoutProtocolToo]
    resources: [File]
}

// One of the two protocols doesn't list the trailer trait, so TwoProtocols
// gets a DANGER, which can still be suppressed.
@supportsTrailers
@noTrailers
service TwoProtocolsService {
    version: "2024-01-01"
    operations: [TwoProtocols, Suppressed]
}

// Shared is also bound here, by a protocol that doesn't list the trailer
// trait, so Shared gets a DANGER.
@noTrailers
service NonSupportingService {
    version: "2024-01-01"
    operations: [Shared, CommonB]
}

// A service with no protocol, so NoProtocol gets a DANGER. A service with no
// protocol adds no protocols, so WithoutProtocolToo, which is also bound by
// SupportingService, still gets a WARNING.
service NoProtocolService {
    version: "2024-01-01"
    operations: [NoProtocol, WithoutProtocolToo]
}

resource File {
    identifiers: {fileId: String}
    read: GetFile
}

@readonly
@http(method: "GET", uri: "/files/{fileId}")
operation GetFile {
    input := {
        @required
        @httpLabel
        fileId: String
    }
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

// CommonOutput is the output of operations in SupportingService and
// NonSupportingService, so its trailer member gets a single DANGER.
@readonly
@http(method: "GET", uri: "/common-a")
operation CommonA {
    output: CommonOutput
}

@readonly
@http(method: "GET", uri: "/common-b")
operation CommonB {
    output: CommonOutput
}

structure CommonOutput {
    @required
    @httpPayload
    body: StreamingBlob

    @trailer
    checksum: String
}

@readonly
@http(method: "GET", uri: "/without-protocol-too")
operation WithoutProtocolToo {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

// No service contains this error, so its trailer member gets a DANGER.
@error("server")
structure UnreferencedError {
    @required
    @httpPayload
    body: StreamingBlob

    @trailer
    checksum: String
}

@readonly
@http(method: "GET", uri: "/supported")
operation Supported {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

@readonly
@http(method: "GET", uri: "/two-protocols")
operation TwoProtocols {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

@readonly
@http(method: "GET", uri: "/suppressed")
operation Suppressed {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        @suppress(["HttpPayload.UnboundTrailer"])
        checksum: String
    }
}

@readonly
@http(method: "GET", uri: "/shared")
operation Shared {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

@readonly
@http(method: "GET", uri: "/no-protocol")
operation NoProtocol {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

@streaming
blob StreamingBlob
