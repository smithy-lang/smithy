$version: "2.0"

namespace smithy.example

// Trailer members are allowed alongside a streaming blob in output
// structures, including when they are required or have a default.
operation DownloadFile {
    output := {
        @required
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

// Trailer members are allowed in structures that aren't used by an operation.
structure Standalone {
    @required
    body: StreamingBlob

    @trailer
    checksum: String
}

@streaming
blob StreamingBlob
