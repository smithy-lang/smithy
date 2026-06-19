$version: "2.0"

namespace smithy.calltest

use aws.protocols#restJson1

@restJson1
service CallTest {
    version: "2026-01-01"
    operations: [Echo, Download]
}

@http(method: "POST", uri: "/echo", code: 200)
operation Echo {
    input := {
        @required
        message: String
    }
    output := {
        message: String
    }
}

@readonly
@http(method: "GET", uri: "/download", code: 200)
operation Download {
    output: DownloadOutput
}

structure DownloadOutput {
    @required
    @httpPayload
    body: StreamingBlob
}

@streaming
blob StreamingBlob
