$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

service HelloService {
    version: "2024-01-17"
}

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
operation GetFoo {}

structure VendorParams {
    foo: String
}
