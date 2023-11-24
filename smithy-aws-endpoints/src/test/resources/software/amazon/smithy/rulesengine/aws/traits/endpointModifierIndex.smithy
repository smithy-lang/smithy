$version: "2.0"

namespace ns.foo

use aws.endpoints#standardRegionalEndpoints
use aws.endpoints#standardPartitionalEndpoints
use aws.endpoints#dualStackOnlyEndpoints
use aws.endpoints#rulesBasedEndpoints

@standardRegionalEndpoints
service Service1 {
    version: "2021-06-29"
}

@standardPartitionalEndpoints(endpointPatternType: "service_dnsSuffix")
service Service2 {
    version: "2021-06-29"
}

@standardRegionalEndpoints(
    partitionSpecialCases: {
        "aws-us-gov": [
            {
                endpoint: "myservice.{region}.{dnsSuffix}",
                dualStack: true
            },
            {
                endpoint: "myservice.global.amazonaws.com",
                dualStack: true
            }
        ]
    },
    regionSpecialCases: {
        "us-east-1": [
        ]
    }
)
@dualStackOnlyEndpoints
service Service3 {
    version: "2021-06-29"
}

@standardPartitionalEndpoints(
    endpointPatternType: "service_region_dnsSuffix",
    partitionEndpointSpecialCases: {
        "aws-us-gov": [
            {
                endpoint: "myservice.{region}.{dnsSuffix}",
                region: "us-east-1"
                fips: true
            },
            {
                endpoint: "myservice.global.amazonaws.com",
                region: "us-west-2"
                dualStack: true
            }
        ]
    }
)
@rulesBasedEndpoints
service Service4 {
    version: "2021-06-29"
}
