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
}

@streaming
union StreamUnion {
    first: Unit
    second: Unit
}
