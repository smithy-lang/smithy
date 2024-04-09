$version: "2.0"

namespace com.foo

use smithy.test#smokeTests
use aws.test#AwsVendorParams
use aws.test#S3VendorParams

@smokeTests([
    {
        id: "AwsVendorParamsCase"
        params: {}
        vendorParams: {
            region: "us-east-1"
            sigv4aRegionSet: ["us-east-1"]
            uri: "foo"
            useFips: true
            useDualstack: true
            useAccountIdRouting: false
        }
        vendorParamsShape: AwsVendorParams
        expect: {
            success: {}
        }
    }
    {
        id: "S3VendorParamsCase"
        params: {}
        vendorParams: {
            region: "us-east-2"
            sigv4aRegionSet: ["us-east-2"]
            uri: "bar"
            useFips: true
            useDualstack: true
            useAccountIdRouting: false
            useAccelerate: true
            useGlobalEndpoint: true
            forcePathStyle: true
            useArnRegion: false
            useMultiRegionAccessPoints: false
        }
        vendorParamsShape: S3VendorParams
        expect: {
            success: {}
        }
    }
])
operation GetFoo {}
