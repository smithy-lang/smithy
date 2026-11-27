$version: "2.0"

namespace smithy.example

// Trailer members still get a DANGER when the httpPayload trait isn't used
// anywhere in the model.
@readonly
@http(method: "GET", uri: "/document")
operation DownloadDocument {
    output := {
        @required
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

@streaming
blob StreamingBlob
