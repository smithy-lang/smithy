$version: "2.0"

namespace smithy.example

use smithy.test#eventStreamTests

@trait
@protocolDefinition
structure testProtocol {}

@eventStreamTests([
    {
        id: "nonMatchingInitialRequestParams"
        protocol: testProtocol
        initialRequestParams: {
            roomId: "smithy"
        }
    }
    {
        id: "nonMatchingInitialRequest"
        protocol: testProtocol
        initialRequestParams: {
            room: "smithy"
        }
        initialRequest: {
            room: "smithy"
        }
        initialRequestShape: RoomContext
    }
    {
        id: "nonMatchingInitialResponseParams"
        protocol: testProtocol
        initialResponseParams: {
            roomId: "smithy"
        }
    }
    {
        id: "nonMatchingInitialResponse"
        protocol: testProtocol
        initialResponseParams: {
            room: "smithy"
        }
        initialResponse: {
            room: "smithy"
        }
        initialResponseShape: RoomContext
    }
    {
        id: "nonMatchingVendorParams"
        protocol: testProtocol
        initialRequestParams: {
            room: "smithy"
        }
        vendorParams: {
            location: "central"
        }
        vendorParamsShape: VendorParams
    }
    {
        id: "nonMatchingRequestEventParams"
        protocol: testProtocol
        events: [{
            type: "request"
            params: {
                message: "smithy"
            }
        }]
    }
    {
        id: "nonMatchingResponseEventParams"
        protocol: testProtocol
        events: [{
            type: "response"
            params: {
                message: "smithy"
            }
        }]
    }
    {
        id: "nonMatchingEventVendorParams"
        protocol: testProtocol
        events: [{
            type: "request"
            params: {
                message: {
                    message: "foo"
                }
            }
            vendorParams: {
                dialect: "en_US"
            }
            vendorParamsShape: VendorParams
        }]
    }
])
operation ExchangeMessages {
    input := {
        @httpHeader("x-chat-room")
        room: String
        stream: MessageStream
    }
    output := {
        @httpHeader("x-chat-room")
        room: String
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

structure RoomContext {
    @required
    roomId: String
}

structure VendorParams {
    @required
    region: String
}
