$version: "2"

namespace smithy.example

use aws.endpoints#standardPartitionalEndpoints

@standardPartitionalEndpoints(
    endpointPatternType: "service_dnsSuffix",
    partitionEndpointSpecialCases: {
        "aws-us": [{endpoint: "https://myservice.us-west-2.amazonaws.com", region: "us-west-2"}],
        "aws-cn": [{endpoint: "https://myservice.cn-north-1.amazonaws.com", region: "cn-north-1"}]
    }
)
service MyService {
    version: "2020-04-02"
}
