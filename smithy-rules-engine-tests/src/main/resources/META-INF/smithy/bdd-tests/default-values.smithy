$version: "2.0"

namespace smithy.tests.endpointrules.defaultvalues

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait"
])
@clientContextParams(
    bar: {
        type: "string"
        documentation: "a client string parameter"
    }
    baz: {
        type: "string"
        documentation: "another client string parameter"
    }
)
@endpointBdd(
    version: "1.1"
    parameters: {
        bar: {
            required: false
            documentation: "docs"
            type: "string"
        }
        baz: {
            required: true
            default: "baz"
            documentation: "docs"
            type: "string"
        }
    }
    conditions: [
        {
            fn: "isSet"
            argv: [
                {
                    ref: "bar"
                }
            ]
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: "https://example.com/{baz}"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            documentation: "error fallthrough"
            conditions: []
            error: "endpoint error"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 2
    nodes: "/////wAAAAH/////AAAAAAX14QEF9eEC"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "Default value is used when parameter is unset"
            params: {
                bar: "a b"
            }
            operationInputs: [
                {
                    operationName: "GetThing"
                    clientParams: {
                        bar: "a b"
                    }
                }
            ]
            expect: {
                endpoint: {
                    url: "https://example.com/baz"
                }
            }
        }
        {
            documentation: "Default value is not used when the parameter is set"
            params: {
                bar: "a b"
                baz: "BIG"
            }
            operationInputs: [
                {
                    operationName: "GetThing"
                    clientParams: {
                        bar: "a b"
                        baz: "BIG"
                    }
                }
            ]
            expect: {
                endpoint: {
                    url: "https://example.com/BIG"
                }
            }
        }
        {
            documentation: "a documentation string"
            expect: {
                error: "endpoint error"
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
