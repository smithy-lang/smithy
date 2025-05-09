$version: "2.0"

namespace smithy.example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    version: "1.0"
    parameters: {
        endpoint: {
            type: "string"
            builtIn: "SDK::Endpoint"
            documentation: "docs"
            default: "https://example.com"
            required: true
        }
    }
    rules: [
        {
            conditions: []
            documentation: "Passthrough"
            endpoint: {
                url: "{endpoint}"
            }
            type: "endpoint"
        }
    ]
})
@endpointTests({
    version: "1.0"
    testCases: [
        {
            params: {}
            operationInputs: [
                {
                    operationName: "GetThing"
                    operationParams: {
                        fizz: "something"
                        buzz: "a buzz value"
                    }
                }
            ]
            expect: {
                endpoint: {
                    url: "https://example.com"
                }
            }
        }
    ]
})
@suppress(["RuleSetParameter.TestCase.Unused"])
service InvalidService {
    version: "2022-01-01"
    operations: [
        GetThing
    ]
}

@readonly
operation GetThing {
    input := {
        buzz: String
        fuzz: String
    }
}
