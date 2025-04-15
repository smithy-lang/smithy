$version: "2.0"

namespace smithy.example

use smithy.rules#endpointTests

service InvalidService {
    version: "2022-01-01"
    operations: [
        GetThing
    ]
}

apply InvalidService @endpointTests({
    version: "1.0"
    testCases: [
        {
            params: { stringFoo: "c d", boolFoo: true }
            operationInputs: [
                {
                    operationName: "GetThing"
                    operationParams: { fizz: "something", buzz: "a buzz value" }
                }
            ]
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {}
                    headers: {
                        single: ["foo"]
                        multi: ["foo", "bar", "baz"]
                    }
                }
            }
        }
    ]
})

@readonly
operation GetThing {
    input := {
        buzz: String
        fuzz: String
    }
}
