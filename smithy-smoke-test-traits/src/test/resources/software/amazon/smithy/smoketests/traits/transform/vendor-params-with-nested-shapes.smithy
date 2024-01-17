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
            nestedStruct: {
                nestedString: "foo"
            }
        },
        vendorParamsShape: VendorParams
    }
])
operation SayHello {}

structure VendorParams {
    nestedStruct: NestedStruct
}

structure NestedStruct {
    nestedString: NestedString
}

string NestedString
