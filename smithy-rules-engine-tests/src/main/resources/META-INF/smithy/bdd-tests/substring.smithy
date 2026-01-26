$version: "2.0"

namespace smithy.tests.endpointrules.substring

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait"
])
@clientContextParams(
    TestCaseId: {
        type: "string"
        documentation: "docs"
    }
    Input: {
        type: "string"
        documentation: "docs"
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
        Input: {
            required: true
            documentation: "the input used to test substring"
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
                "1"
            ]
        }
        {
            fn: "substring"
            argv: [
                "{Input}"
                0
                4
                false
            ]
            assign: "output_ssa_1"
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
            fn: "substring"
            argv: [
                "{Input}"
                0
                4
                true
            ]
            assign: "output_ssa_2"
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
            fn: "substring"
            argv: [
                "{Input}"
                1
                3
                false
            ]
            assign: "output_ssa_3"
        }
    ]
    results: [
        {
            conditions: []
            error: "The value is: `{output_ssa_1}`"
            type: "error"
        }
        {
            conditions: []
            error: "The value is: `{output_ssa_2}`"
            type: "error"
        }
        {
            conditions: []
            error: "The value is: `{output_ssa_3}`"
            type: "error"
        }
        {
            documentation: "fallback when no tests match"
            conditions: []
            error: "No tests matched"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 7
    nodes: "/////wAAAAH/////AAAAAAAAAAMAAAAEAAAAAQX14QEAAAAEAAAAAgAAAAUAAAAGAAAAAwX14QIAAAAGAAAABAAAAAcF9eEEAAAABQX14QMF9eEE"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "substring when string is long enough"
            params: {
                TestCaseId: "1"
                Input: "abcdefg"
            }
            expect: {
                error: "The value is: `abcd`"
            }
        }
        {
            documentation: "substring when string is exactly the right length"
            params: {
                TestCaseId: "1"
                Input: "abcd"
            }
            expect: {
                error: "The value is: `abcd`"
            }
        }
        {
            documentation: "substring when string is too short"
            params: {
                TestCaseId: "1"
                Input: "abc"
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring when string is too short"
            params: {
                TestCaseId: "1"
                Input: ""
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring on wide characters (ensure that unicode code points are properly counted)"
            params: {
                TestCaseId: "1"
                Input: "﷽"
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "the full set of ascii is supported, including non-printable characters"
            params: {
                TestCaseId: "1"
                Input: "\u007fabcdef"
            }
            expect: {
                error: "The value is: `\u007fabc`"
            }
        }
        {
            documentation: "substring when string is long enough"
            params: {
                TestCaseId: "2"
                Input: "abcdefg"
            }
            expect: {
                error: "The value is: `defg`"
            }
        }
        {
            documentation: "substring when string is exactly the right length"
            params: {
                TestCaseId: "2"
                Input: "defg"
            }
            expect: {
                error: "The value is: `defg`"
            }
        }
        {
            documentation: "substring when string is too short"
            params: {
                TestCaseId: "2"
                Input: "abc"
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring when string is too short"
            params: {
                TestCaseId: "2"
                Input: ""
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring on wide characters (ensure that unicode code points are properly counted)"
            params: {
                TestCaseId: "2"
                Input: "﷽"
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring when string is longer"
            params: {
                TestCaseId: "3"
                Input: "defg"
            }
            expect: {
                error: "The value is: `ef`"
            }
        }
        {
            documentation: "substring when string is exact length"
            params: {
                TestCaseId: "3"
                Input: "def"
            }
            expect: {
                error: "The value is: `ef`"
            }
        }
        {
            documentation: "substring when string is too short"
            params: {
                TestCaseId: "3"
                Input: "ab"
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring when string is too short"
            params: {
                TestCaseId: "3"
                Input: ""
            }
            expect: {
                error: "No tests matched"
            }
        }
        {
            documentation: "substring on wide characters (ensure that unicode code points are properly counted)"
            params: {
                TestCaseId: "3"
                Input: "﷽"
            }
            expect: {
                error: "No tests matched"
            }
        }
    ]
)
service FizzBuzz {
}
