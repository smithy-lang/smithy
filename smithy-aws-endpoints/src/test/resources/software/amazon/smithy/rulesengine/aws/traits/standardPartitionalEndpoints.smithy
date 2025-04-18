$version: "2.0"

namespace ns.foo

use aws.endpoints#standardPartitionalEndpoints

@standardPartitionalEndpoints(endpointPatternType: "service_dnsSuffix")
service Service1 {
    version: "2021-06-29"
}

@standardPartitionalEndpoints(
    endpointPatternType: "service_region_dnsSuffix",
    partitionEndpointSpecialCases: {
        "aws-us-gov": [
            {
                endpoint: "https://myservice.{region}.{dnsSuffix}",
                region: "us-east-1"
                fips: true
            },
            {
                endpoint: "https://myservice.global.amazonaws.com",
                region: "us-west-2"
                dualStack: true
            }
        ]
    }
)
service Service2 {
    version: "2021-06-29"
}

@standardPartitionalEndpoints(
    endpointPatternType: "aws_recommended",
    partitionEndpointSpecialCases: {
        "aws": [
            {
                endpoint: "https://myservice.{dnsSuffix}",
                region: "us-west-2"
            },
        ]
    }
)
service Service3 {
    version: "2025-01-01"
}
