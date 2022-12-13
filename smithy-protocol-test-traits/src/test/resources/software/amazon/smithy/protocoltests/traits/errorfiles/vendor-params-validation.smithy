$version: "2.0"

namespace smithy.example

use smithy.test#httpResponseTests
use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "foo1",
        protocol: testProtocol,
        code: 200,
        params: {},
        vendorParamsShape: emptyVendorParamsStructure,
    },
    {
        id: "foo2",
        protocol: testProtocol,
        code: 200,
        params: {},
        vendorParamsShape: emptyVendorParamsStructure,
        vendorParams: {
            additional: true,
        }
    }
])
operation SayGoodbye {
    input: SayGoodbyeInput,
    output: SayGoodbyeOutput
}

@input
structure SayGoodbyeInput {}

@output
structure SayGoodbyeOutput {}

@httpResponseTests([
    {
        id: "foo3",
        protocol: testProtocol,
        code: 200,
        params: {
            foo: 1
        },
        vendorParamsShape: simpleVendorParamsStructure,
        vendorParams: {
            integer: 1,
            float: "Hi"
        }
    }
])
@error("client")
structure MyError {
    foo: Integer,
}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo5",
        protocol: testProtocol,
        method: "POST",
        uri: "/",
        params: {
            type: true
        },
        vendorParamsShape: simpleVendorParamsStructure,
        vendorParams: {
            float: 1.2
        }
    },
    {
        id: "foo6",
        protocol: testProtocol,
        method: "POST",
        uri: "/",
        params: {
            type: true
        },
        vendorParamsShape: simpleVendorParamsStructure,
        vendorParams: {
            integer: 1,
            boolean: "Hi"
        }
    }
])
operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    type: Boolean
}

@output
structure SayHelloOutput {}

structure emptyVendorParamsStructure {}

structure simpleVendorParamsStructure {
    @required
    integer: Integer,

    boolean: Boolean,

    float: Float,
}
