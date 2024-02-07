$version: "2"

namespace smithy.example

use aws.endpoints#standardRegionalEndpoints

@standardRegionalEndpoints(
    partitionSpecialCases: {
        "aws-us-gov": [
            {
                endpoint: "https://myservice.{region}.{dnsSuffix}",
                fips: true
            }
        ]
    },
    regionSpecialCases: {
        "us-east-1": [
        ]
    }
)
service MyService {
    version: "2020-04-02"
}
