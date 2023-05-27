$version: "1.0"

namespace smithy.example

use smithy.rules#endpointTests

service InvalidService {
    version: "2022-01-01",
    operations: [GetThing]
}

apply InvalidService @endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "params": {
                "stringFoo": "c d",
                "boolFoo": true
            },
            "operationInputs": [{
                "operationName": "GetThing",
                "operationParams": {
                    "buzz": "a buzz value",
                },
            }],
            "expect": {
                "error": "failed to resolve"
            }
        }
    ]
})

@readonly
operation GetThing {
    input: GetThingInput
}

@input
structure GetThingInput {
    @required
    fizz: String,

    buzz: String,

    fuzz: String,
}
