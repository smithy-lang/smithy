$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "say_hello",
        params: {},
        vendorParams: {
            foo: "bar"
        },
        vendorParamsShape: MissingVendorParamsShape,
        expect: {
            success: {}
        }
    }
])
operation SayHello {
    input := {}
    output := {}
}
