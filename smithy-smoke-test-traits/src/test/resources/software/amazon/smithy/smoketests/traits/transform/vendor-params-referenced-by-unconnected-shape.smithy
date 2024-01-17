$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

service HelloService {
    version: "2024-01-17"
    operations: [SayHello]
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
operation SayHello {}

structure VendorParams {
    foo: String
}

structure Unconnected {
    vendorParams: VendorParams
}
