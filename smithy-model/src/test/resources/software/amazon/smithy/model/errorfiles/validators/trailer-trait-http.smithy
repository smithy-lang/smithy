$version: "2.0"

namespace smithy.example

// Each trailer member gets its own DANGER, including required members and
// members with a default.
@readonly
@http(method: "GET", uri: "/download")
operation DownloadFile {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String

        @trailer
        @required
        size: Long

        @trailer
        @default("none")
        status: String
    }
}

// The DANGER can be suppressed on the member.
@readonly
@http(method: "GET", uri: "/suppressed")
operation DownloadSuppressed {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        @suppress(["HttpPayload.UnboundTrailer"])
        checksum: String
    }
}

// Other unbound members still get an ERROR that doesn't list trailer members.
@readonly
@http(method: "GET", uri: "/mixed")
operation DownloadMixed {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String

        name: String
    }
}

@streaming
blob StreamingBlob
