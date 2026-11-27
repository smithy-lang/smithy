$version: "2.0"

namespace smithy.example

@aws.protocols#restJson1
service Trailers {
    version: "2018-01-01"
    operations: [DownloadFile]
}

// Trailers on operations outside of the converted service are ignored.
@aws.protocols#restJson1
service NoTrailers {
    version: "2018-01-01"
    operations: [ListFiles]
}

@readonly
@http(method: "GET", uri: "/files")
operation ListFiles {
    output := {
        names: Names
    }
}

list Names {
    member: String
}

@readonly
@http(method: "GET", uri: "/files/{name}")
operation DownloadFile {
    input := {
        @required
        @httpLabel
        name: String
    }

    output := {
        @httpHeader("x-file-version")
        fileVersion: String

        @required
        @httpPayload
        body: FileStream

        @trailer
        @suppress(["HttpPayload.UnboundTrailer"])
        checksum: String
    }
}

@streaming
blob FileStream
