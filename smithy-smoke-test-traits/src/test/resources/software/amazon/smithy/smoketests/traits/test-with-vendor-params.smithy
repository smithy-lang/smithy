$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "with_vendor_params",
        expect: {
            success: {}
        },
        vendorParams: {
            foo: "Bar"
        }
    }
])
operation SayHello {}
