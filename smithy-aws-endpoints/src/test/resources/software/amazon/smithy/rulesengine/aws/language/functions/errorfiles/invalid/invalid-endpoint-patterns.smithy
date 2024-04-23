$version: "1.0"

namespace example

use aws.endpoints#standardPartitionalEndpoints
use aws.endpoints#standardRegionalEndpoints

@standardRegionalEndpoints(
    regionSpecialCases: {
        "us-east-1": [
            {
                endpoint: "https://myservice.{invalid}.{dnsSuffix}"
            }
        ]
    }
    partitionSpecialCases: {
        aws: [
            {
                endpoint: "{invalid}-fips.{region}.{badSuffix}"
                fips: true
            }
            {
                endpoint: "https://{region}.   invalidurl   {dnsSuffix}"
                dualStack: true
            }
        ]
    }
)
service Service1 {}

@standardPartitionalEndpoints(
    endpointPatternType: "service_dnsSuffix"
    partitionEndpointSpecialCases: {
        aws: [
            {
                endpoint: "myservice.{invalid}.{dnsSuffix}"
                region: "us-east-1"
            }
        ]
    }
)
service Service2 {}
