$version: "2.0"

namespace smithy.example

use smithy.test#eventStreamTests

@trait
@protocolDefinition
structure testProtocol {}

@eventStreamTests([
    {
        id: "hasEvent"
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
        id: "hasInitialRequest"
        protocol: testProtocol
        initialRequestParams: {
            room: "smithy"
        }
    }
    {
        id: "hasInitialResponse"
        protocol: testProtocol
        initialResponseParams: {
            room: "smithy"
        }
    }
    {
        id: "invalid"
        protocol: testProtocol
    }
])
operation PublishMessages {
    input := {
        @httpHeader("x-chat-room")
        room: String
        stream: MessageStream
    }
    output := {
        @httpHeader("x-chat-room")
        room: String
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
