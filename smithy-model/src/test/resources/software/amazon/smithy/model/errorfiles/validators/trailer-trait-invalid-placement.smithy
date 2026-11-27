$version: "2.0"

namespace smithy.example

// Trailer members are not allowed in operation input.
operation UploadFile {
    input := {
        @required
        body: StreamingBlob

        @trailer
        checksum: String
    }
}

// Trailer members are not allowed on the streaming member itself.
operation DownloadTrailingBody {
    output := {
        @required
        @trailer
        body: StreamingBlob
    }
}

// Trailer members are not allowed in structures without a streaming blob.
structure NotStreaming {
    @trailer
    checksum: String
}

// Trailer members are not allowed alongside event streams.
operation Subscribe {
    output := {
        events: Events

        @trailer
        checksum: String
    }
}

@streaming
union Events {
    event: Event
}

structure Event {}

// Trailer members are allowed in mixins, but not when mixed into operation input.
@mixin
structure StreamingMixin {
    @required
    body: StreamingBlob

    @trailer
    checksum: String
}

operation UploadViaMixin {
    input := with [StreamingMixin] {}
}

@streaming
blob StreamingBlob
