$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "with_vendor_params_shape",
        expect: {
            success: {}
        },
        vendorParams: {
            foo: "Bar"
        },
        vendorParamsShape: VendorParams
    }
])
operation SayHello {}

structure VendorParams {
    foo: String
}
