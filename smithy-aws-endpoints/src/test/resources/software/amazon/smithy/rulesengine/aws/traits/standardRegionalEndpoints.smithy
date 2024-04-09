$version: "2.0"

namespace ns.foo

use aws.endpoints#standardRegionalEndpoints

@standardRegionalEndpoints
service Service1 {
    version: "2021-06-29"
}

@standardRegionalEndpoints(partitionSpecialCases: {}, regionSpecialCases: {})
service Service2 {
    version: "2021-06-29"
}

@standardRegionalEndpoints(
    partitionSpecialCases: {
        "aws-us-gov": [
            {
                endpoint: "https://myservice.{region}.{dnsSuffix}",
                fips: true
            },
            {
                endpoint: "https://myservice.global.amazonaws.com",
                dualStack: true
            }
        ]
    },
    regionSpecialCases: {
        "us-east-1": [
        ]
    }
)
service Service3 {
    version: "2021-06-29"
}
