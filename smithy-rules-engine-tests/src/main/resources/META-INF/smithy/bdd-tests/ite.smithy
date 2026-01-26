$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait.smithy"
])
@clientContextParams(
    useFips: {
        type: "boolean"
        documentation: "Use FIPS endpoints"
    }
)
@endpointBdd(
    version: "1.1"
    parameters: {
        useFips: {
            required: true
            default: false
            documentation: "Use FIPS endpoints"
            type: "boolean"
        }
    }
    conditions: [
        {
            fn: "ite"
            argv: [
                {
                    ref: "useFips"
                }
                "-fips"
                ""
            ]
            assign: "suffix"
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: "https://example{suffix}.com"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
    ]
    root: 2
    nodeCount: 2
    nodes: "/////wAAAAH/////AAAAAAX14QEF9eEA"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "When useFips is true, returns trueValue"
            params: {
                useFips: true
            }
            operationInputs: [
                {
                    operationName: "GetThing"
                }
            ]
            expect: {
                endpoint: {
                    url: "https://example-fips.com"
                }
            }
        }
        {
            documentation: "When useFips is false, returns falseValue"
            params: {
                useFips: false
            }
            operationInputs: [
                {
                    operationName: "GetThing"
                }
            ]
            expect: {
                endpoint: {
                    url: "https://example.com"
                }
            }
        }
    ]
)
service FizzBuzz {
    version: "2022-01-01"
    operations: [
        GetThing
    ]
}

operation GetThing {
    input := {}
    output: Unit
}
