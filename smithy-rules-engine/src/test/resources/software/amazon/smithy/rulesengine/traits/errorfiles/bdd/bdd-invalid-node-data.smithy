$version: "2.0"

namespace smithy.example

use smithy.rules#endpointBdd

@endpointBdd({
    parameters: {
        Region: {
            type: "string"
            required: true
            documentation: "The AWS region"
        }
        UseFips: {
            type: "boolean"
            required: true
            default: false
            documentation: "Use FIPS endpoints"
        }
    }
    conditions: [
        {
            fn: "isSet"
            argv: [{ref: "Region"}]
        }
        {
            fn: "booleanEquals"
            argv: [{ref: "UseFips"}, true]
        }
    ]
    results: [
        {
            type: "endpoint"
            endpoint: {
                url: "https://service.{Region}.amazonaws.com"
            }
        }
        {
            type: "endpoint"
            endpoint: {
                url: "https://service-fips.{Region}.amazonaws.com"
            }
        }
    ]
    nodes: "AQB" // bad data, valid base64
    nodeCount: 3
    root: 1
})
service ValidBddService {
    version: "2022-01-01"
}
