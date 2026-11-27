$version: "2.0"

namespace smithy.example

@readonly
@http(method: "GET", uri: "/with-payload")
operation DownloadWithPayload {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

@readonly
@http(method: "GET", uri: "/without-payload")
operation DownloadWithoutPayload {
    output := {
        @required
        body: StreamingBlob

        @trailer
        checksum: String

        name: String
    }
}

@streaming
blob StreamingBlob
