$version: "2.0"

namespace smithy.example

use smithy.test#eventStreamTests

@trait
@protocolDefinition
structure testProtocol {}

@eventStreamTests([
    {
        id: "validJson"
        protocol: testProtocol
        events: [{
            type: "request"
            body: "{}"
            bodyMediaType: "application/json"
        }]
    }
    {
        id: "invalidJson"
        protocol: testProtocol
        events: [{
            type: "request"
            body: "{\"keyWithoutValue\"}"
            bodyMediaType: "application/json"
        }]
    }
    {
        id: "validXml"
        protocol: testProtocol
        events: [{
            type: "request"
            body: "<foo/>"
            bodyMediaType: "application/xml"
        }]
    }
    {
        id: "invalidXml"
        protocol: testProtocol
        events: [{
            type: "request"
            body: "{}"
            bodyMediaType: "application/xml"
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
