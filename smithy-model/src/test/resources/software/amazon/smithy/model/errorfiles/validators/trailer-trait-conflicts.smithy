$version: "2.0"

namespace smithy.example

// Trailer members can't also be bound with an HTTP binding trait.
@readonly
@http(method: "GET", uri: "/conflict")
operation DownloadConflict {
    output := {
        @required
        @httpPayload
        body: StreamingBlob

        @trailer
        @httpHeader("x-checksum")
        checksum: String

        @trailer
        @httpPrefixHeaders("x-meta-")
        prefixHeaders: StringMap

        @trailer
        @httpResponseCode
        responseCode: Integer

        @trailer
        @httpQuery("query")
        query: String

        @trailer
        @httpQueryParams
        queryParams: StringMap

        @trailer
        @httpLabel
        @required
        label: String
    }
}

@readonly
@http(method: "GET", uri: "/conflict-payload")
operation DownloadConflictPayload {
    output := {
        @required
        body: StreamingBlob

        @trailer
        @httpPayload
        checksum: String
    }
}

map StringMap {
    key: String
    value: String
}

@streaming
blob StreamingBlob
