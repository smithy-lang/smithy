$version: "2.0"

namespace smithy.example

use smithy.rules#endpointTests

@endpointTests({
    version: "1.0"
    testCases: [
        {
            operationInputs: [{
                operationName: "GetThing"
                operationParams: {
                    "buzz": "a buzz value"
                }
            }]
            expect: {
                error: "Failed to resolve."
            }
        }
    ]
})
service InvalidService {
    version: "2022-01-01"
    operations: [GetThing]
}

@readonly
operation GetThing {
    input := {
        @required
        fizz: String
        buzz: String
        fuzz: String
    }
}
