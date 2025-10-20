$version: "2.0"

namespace smithy.example

use smithy.test#eventStreamTests

@trait
@protocolDefinition
structure testProtocol {}

@eventStreamTests([
    {
        id: "duplicate"
        protocol: testProtocol
        events: [{
            type: "request"
            params: {
                message: {
                    message: "foo"
                }
            }
        }]
    }
    {
        id: "duplicate"
        protocol: testProtocol
        events: [{
            type: "request"
            params: {
                message: {
                    message: "bar"
                }
            }
        }]
    }
])
operation PublishMessages {
    input := {
        stream: MessageStream
    }
}

@streaming
union MessageStream {
    message: MessageEvent
}

structure MessageEvent {
    @required
    message: String
}
