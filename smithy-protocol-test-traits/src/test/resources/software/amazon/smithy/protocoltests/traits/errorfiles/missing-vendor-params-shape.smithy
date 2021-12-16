$version: "2.0"

namespace smithy.example

use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo1",
        protocol: testProtocol,
        method: "POST",
        uri: "/",
        params: {
            type: true
        },
        vendorParamsShape: missingVendorParamsStructure,
        vendorParams: {
            integer: 1,
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
