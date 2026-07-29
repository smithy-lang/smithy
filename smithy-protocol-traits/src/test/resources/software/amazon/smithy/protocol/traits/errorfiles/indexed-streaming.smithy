$version: "2.0"

namespace smithy.example

use smithy.protocols#idx
use smithy.protocols#indexed

@indexed
service StreamingService {
    version: "2026-07-24"
    operations: [Stream]
}

operation Stream {
    input := {
        stream: StreamUnion

        @idx(1)
        value: String
    }
    output := {
        @required
        stream: StreamBlob

        @idx(1)
        value: String
    }
}

@streaming
union StreamUnion {
    @idx(1)
    event: Event
}

structure Event {
    @eventHeader
    header: String

    @eventPayload
    payload: Payload
}

structure Payload {
    @idx(1)
    value: String
}

@streaming
blob StreamBlob
