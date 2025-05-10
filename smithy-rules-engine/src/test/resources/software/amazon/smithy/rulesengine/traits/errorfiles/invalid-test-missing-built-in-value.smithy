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
        }
    }
    rules: [
        {
            documentation: "Passthrough"
            conditions: []
            endpoint: {
                url: "https://example.com"
            }
            type: "endpoint"
        }
    ]
})
@endpointTests({
    version: "1.0"
    testCases: [
        {
            params: {
                endpoint: "https://example.com"
            }
            operationInputs: [{
                operationName: "GetThing"
            }]
            expect: {
                endpoint: {
                    url: "https://example.com"
                }
            }
        }
    ]
})
@suppress(["RuleSetParameter.TestCase.Unused"])
service ExampleService {
    version: "2020-07-02"
    operations: [GetThing]
}

operation GetThing {
    input := {
        fizz: String
    }
}

