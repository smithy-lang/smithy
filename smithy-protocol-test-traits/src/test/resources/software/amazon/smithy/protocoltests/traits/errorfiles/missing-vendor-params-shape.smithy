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
    input: SayHelloInput
}

structure SayHelloInput {
    type: Boolean
}
