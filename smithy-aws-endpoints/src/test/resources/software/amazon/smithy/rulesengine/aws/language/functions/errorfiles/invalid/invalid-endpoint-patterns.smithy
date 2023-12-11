$version: "1.0"

namespace example

use aws.endpoints#standardRegionalEndpoints

@standardRegionalEndpoints(
    regionSpecialCases: {
        "us-east-1": [
            {
                endpoint: "myservice.{invalid}.{dnsSuffix}",
            }
        ]
    },
    partitionSpecialCases: {
        "aws": [
            {
                endpoint: "{invalid}-fips.{region}.{dnsSuffix}",
                fips: true
            }
        ]
    }
)
service FizzBuzz {}
