$version: "2.0"

namespace smithy.example

use smithy.rules#endpointBdd

@endpointBdd({
    version: "1.1"
    parameters: {
        Region: {
            type: "string"
            required: true
            documentation: "The AWS region"
        }
    }
    conditions: [
        {
            fn: "isSet"
            argv: [{ref: "Region"}]
        }
    ]
    results: [
        {
            type: "endpoint"
            endpoint: {
                url: "https://service.{Region}.amazonaws.com"
            }
        }
    ]
    nodes: "ABCD=" // invalid base64
    nodeCount: 3
    root: 1
})
service ValidBddService {
    version: "2022-01-01"
}
