$version: "2.0"

namespace smithy.example

use smithy.test#eventStreamTests

@trait
@protocolDefinition
structure testProtocol {}

@eventStreamTests([
    {
        id: "missingOutputStream"
        protocol: testProtocol
        events: [{
            type: "response"
            params: {
                message: {
                    message: "foo"
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

@eventStreamTests([
    {
        id: "missingInputStream"
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
])
operation ReceiveMessages {
    output := {
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
