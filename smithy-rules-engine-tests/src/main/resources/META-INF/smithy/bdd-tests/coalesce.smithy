$version: "2.0"

namespace smithy.tests.endpointrules.coalesce

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait"
    "RuleSetParameter.TestCase.Unused"
])
@clientContextParams(
    TestCaseId: {
        type: "string"
        required: true
        documentation: "Test case id used to select the test case to use"
    }
    req1: {
        type: "string"
        documentation: "docs"
    }
    req2: {
        type: "string"
        documentation: "docs"
    }
    opt1: {
        type: "string"
        documentation: "always Some"
    }
    opt2: {
        type: "string"
        documentation: "always Some"
    }
)
@endpointBdd(
    version: "1.1"
    parameters: {
        TestCaseId: {
            required: true
            documentation: "Test case id used to select the test case to use"
            type: "string"
        }
        req1: {
            required: true
            default: "req1Value"
            documentation: "req1"
            type: "string"
        }
        req2: {
            required: true
            default: "req2Value"
            documentation: "req2"
            type: "string"
        }
        opt1: {
            required: false
            documentation: "opt1"
            type: "string"
        }
        opt2: {
            required: false
            documentation: "opt2"
            type: "string"
        }
    }
    conditions: [
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "TestCaseId"
                }
                "0"
            ]
        }
        {
            fn: "coalesce"
            argv: [
                {
                    ref: "req1"
                }
                {
                    ref: "req2"
                }
            ]
            assign: "req1req2"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "TestCaseId"
                }
                "1"
            ]
        }
        {
            fn: "coalesce"
            argv: [
                {
                    ref: "opt1"
                }
                {
                    ref: "opt2"
                }
            ]
            assign: "opt1opt2"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "TestCaseId"
                }
                "2"
            ]
        }
        {
            fn: "coalesce"
            argv: [
                {
                    ref: "req1"
                }
                {
                    ref: "opt1"
                }
            ]
            assign: "req1opt1"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "TestCaseId"
                }
                "3"
            ]
        }
        {
            fn: "coalesce"
            argv: [
                {
                    ref: "opt1"
                }
                {
                    ref: "req1"
                }
            ]
            assign: "opt1req1"
        }
    ]
    results: [
        {
            conditions: []
            error: "The value is: {req1req2}"
            type: "error"
        }
        {
            conditions: []
            error: "The value is: {opt1opt2}"
            type: "error"
        }
        {
            conditions: []
            error: "The value is: {req1opt1}"
            type: "error"
        }
        {
            conditions: []
            error: "The value is: {opt1req1}"
            type: "error"
        }
        {
            documentation: "error fallthrough"
            conditions: []
            error: "endpoint error"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 9
    nodes: "/////wAAAAH/////AAAAAAAAAAMAAAAEAAAAAQX14QEAAAAEAAAAAgAAAAUAAAAGAAAAAwX14QIAAAAGAAAABAAAAAcAAAAIAAAABQX14QMAAAAIAAAABgAAAAkF9eEFAAAABwX14QQF9eEF"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "Two required, first val returned"
            params: {
                TestCaseId: "0"
            }
            expect: {
                error: "The value is: req1Value"
            }
        }
        {
            documentation: "Two optional, Some(opt1Value), Some(opt2Value), opt1Value returned"
            params: {
                TestCaseId: "1"
                opt1: "opt1Value"
                opt2: "opt2Value"
            }
            expect: {
                error: "The value is: opt1Value"
            }
        }
        {
            documentation: "Two optional, None, Some(opt2Value), opt2Value returned"
            params: {
                TestCaseId: "1"
                opt2: "opt2Value"
            }
            expect: {
                error: "The value is: opt2Value"
            }
        }
        {
            documentation: "Two optional, None, None, None returned"
            params: {
                TestCaseId: "1"
            }
            expect: {
                error: "endpoint error"
            }
        }
        {
            documentation: "Required then Optional, required returned"
            params: {
                TestCaseId: "2"
                opt1: "opt1Value"
            }
            expect: {
                error: "The value is: req1Value"
            }
        }
        {
            documentation: "Optional then Required, optional value returned"
            params: {
                TestCaseId: "3"
                opt1: "opt1Value"
            }
            expect: {
                error: "The value is: opt1Value"
            }
        }
        {
            documentation: "Optional then Required, optional is none so required value returned"
            params: {
                TestCaseId: "3"
            }
            expect: {
                error: "The value is: req1Value"
            }
        }
    ]
)
service FizzBuzz {
}
