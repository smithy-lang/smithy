$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "missing_vendor_params",
        vendorParamsShape: VendorParams,
        expect: {
            success: {}
        }
    },
    {
        id: "extra_vendor_params_member",
        vendorParams: {
            foo: "bar",
            bar: "baz"
        },
        vendorParamsShape: VendorParams,
        expect: {
            success: {}
        }
    },
    {
        id: "missing_vendor_params_member",
        vendorParams: {},
        vendorParamsShape: VendorParams,
        expect: {
            success: {}
        }
    },
    {
        id: "wrong_vendor_params_member_type",
        vendorParams: {
            foo: 1
        },
        vendorParamsShape: VendorParams,
        expect: {
            success: {}
        }
    },
    {
        id: "missing_vendor_params_shape",
        vendorParams: {
            foo: "bar"
        },
        expect: {
            success: {}
        }
    },
])
operation SayHello {
    input := {}
    output := {}
}

structure VendorParams {
    @required
    foo: String
}
