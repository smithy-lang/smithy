$version: "2.0"

namespace smithy.tests.endpointrules.urlencode

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait"
])
@clientContextParams(
    TestCaseId: {
        type: "string"
        documentation: "Test case id used to select the test case to use"
    }
    Input: {
        type: "string"
        documentation: "The input used to test uriEncoder"
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
            documentation: "The input used to test uriEncode"
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
            fn: "uriEncode"
            argv: [
                "{Input}"
            ]
            assign: "output"
        }
    ]
    results: [
        {
            conditions: []
            error: "The value is: `{output}`"
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
    nodeCount: 3
    nodes: "/////wAAAAH/////AAAAAAAAAAMF9eECAAAAAQX14QEF9eEC"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "uriEncode when the string has nothing to encode returns the input"
            params: {
                TestCaseId: "1"
                Input: "abcdefg"
            }
            expect: {
                error: "The value is: `abcdefg`"
            }
        }
        {
            documentation: "uriEncode with single character to encode encodes only that character"
            params: {
                TestCaseId: "1"
                Input: "abc:defg"
            }
            expect: {
                error: "The value is: `abc%3Adefg`"
            }
        }
        {
            documentation: "uriEncode with all ASCII characters to encode encodes all characters"
            params: {
                TestCaseId: "1"
                Input: "/:,?#[]{}|@! $&'()*+;=%<>\"^`\\"
            }
            expect: {
                error: "The value is: `%2F%3A%2C%3F%23%5B%5D%7B%7D%7C%40%21%20%24%26%27%28%29%2A%2B%3B%3D%25%3C%3E%22%5E%60%5C`"
            }
        }
        {
            documentation: "uriEncode with ASCII characters that should not be encoded returns the input"
            params: {
                TestCaseId: "1"
                Input: "0123456789.underscore_dash-Tilda~"
            }
            expect: {
                error: "The value is: `0123456789.underscore_dash-Tilda~`"
            }
        }
        {
            documentation: "uriEncode encodes unicode characters"
            params: {
                TestCaseId: "1"
                Input: "😹"
            }
            expect: {
                error: "The value is: `%F0%9F%98%B9`"
            }
        }
        {
            documentation: "uriEncode on all printable ASCII characters"
            params: {
                TestCaseId: "1"
                Input: " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
            }
            expect: {
                error: "The value is: `%20%21%22%23%24%25%26%27%28%29%2A%2B%2C-.%2F0123456789%3A%3B%3C%3D%3E%3F%40ABCDEFGHIJKLMNOPQRSTUVWXYZ%5B%5C%5D%5E_%60abcdefghijklmnopqrstuvwxyz%7B%7C%7D~`"
            }
        }
        {
            documentation: "uriEncode on an empty string"
            params: {
                TestCaseId: "1"
                Input: ""
            }
            expect: {
                error: "The value is: ``"
            }
        }
    ]
)
service FizzBuzz {
}
