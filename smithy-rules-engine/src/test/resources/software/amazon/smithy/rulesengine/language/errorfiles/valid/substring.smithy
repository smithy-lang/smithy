$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "parameters": {
    "TestCaseId": {
      "type": "string",
      "required": true,
      "documentation": "Test case id used to select the test case to use"
    },
    "Input": {
      "type": "string",
      "required": true,
      "documentation": "the input used to test substring"
    }
  },
  "rules": [
    {
      "documentation": "Substring from beginning of input",
      "conditions": [
        {
          "fn": "stringEquals",
          "argv": [
            "{TestCaseId}",
            "1"
          ]
        },
        {
          "fn": "substring",
          "argv": [
            "{Input}",
            0,
            4,
            false
          ],
          "assign": "output"
        }
      ],
      "error": "The value is: `{output}`",
      "type": "error"
    },
    {
      "documentation": "Substring from end of input",
      "conditions": [
        {
          "fn": "stringEquals",
          "argv": [
            "{TestCaseId}",
            "2"
          ]
        },
        {
          "fn": "substring",
          "argv": [
            "{Input}",
            0,
            4,
            true
          ],
          "assign": "output"
        }
      ],
      "error": "The value is: `{output}`",
      "type": "error"
    },
    {
      "documentation": "Substring the middle of the string",
      "conditions": [
        {
          "fn": "stringEquals",
          "argv": [
            "{TestCaseId}",
            "3"
          ]
        },
        {
          "fn": "substring",
          "argv": [
            "{Input}",
            1,
            3,
            false
          ],
          "assign": "output"
        }
      ],
      "error": "The value is: `{output}`",
      "type": "error"
    },
    {
      "documentation": "fallback when no tests match",
      "conditions": [],
      "error": "No tests matched",
      "type": "error"
    }
  ],
  "version": "1.3"
})
@endpointTests(
  version: "1.0",
  testCases: [
    {
      "documentation": "substring when string is long enough",
      "params": {
        "TestCaseId": "1",
        "Input": "abcdefg"
      },
      "expect": {
        "error": "The value is: `abcd`"
      }
    },
    {
      "documentation": "substring when string is exactly the right length",
      "params": {
        "TestCaseId": "1",
        "Input": "abcd"
      },
      "expect": {
        "error": "The value is: `abcd`"
      }
    },
    {
      "documentation": "substring when string is too short",
      "params": {
        "TestCaseId": "1",
        "Input": "abc"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring when string is too short",
      "params": {
        "TestCaseId": "1",
        "Input": ""
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring on wide characters (ensure that unicode code points are properly counted)",
      "params": {
        "TestCaseId": "1",
        "Input": "\ufdfd"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "unicode characters always return `None`",
      "params": {
        "TestCaseId": "1",
        "Input": "abcdef\uD83D\uDC31"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "non-ascii cause substring to always return `None`",
      "params": {
        "TestCaseId": "1",
        "Input": "abcdef\u0080"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "the full set of ascii is supported, including non-printable characters",
      "params": {
        "TestCaseId": "1",
        "Input": "\u007Fabcdef"
      },
      "expect": {
        "error": "The value is: `\u007Fabc`"
      }
    },
    {
      "documentation": "substring when string is long enough",
      "params": {
        "TestCaseId": "2",
        "Input": "abcdefg"
      },
      "expect": {
        "error": "The value is: `defg`"
      }
    },
    {
      "documentation": "substring when string is exactly the right length",
      "params": {
        "TestCaseId": "2",
        "Input": "defg"
      },
      "expect": {
        "error": "The value is: `defg`"
      }
    },
    {
      "documentation": "substring when string is too short",
      "params": {
        "TestCaseId": "2",
        "Input": "abc"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring when string is too short",
      "params": {
        "TestCaseId": "2",
        "Input": ""
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring on wide characters (ensure that unicode code points are properly counted)",
      "params": {
        "TestCaseId": "2",
        "Input": "\ufdfd"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring when string is longer",
      "params": {
        "TestCaseId": "3",
        "Input": "defg"
      },
      "expect": {
        "error": "The value is: `ef`"
      }
    },
    {
      "documentation": "substring when string is exact length",
      "params": {
        "TestCaseId": "3",
        "Input": "def"
      },
      "expect": {
        "error": "The value is: `ef`"
      }
    },
    {
      "documentation": "substring when string is too short",
      "params": {
        "TestCaseId": "3",
        "Input": "ab"
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring when string is too short",
      "params": {
        "TestCaseId": "3",
        "Input": ""
      },
      "expect": {
        "error": "No tests matched"
      }
    },
    {
      "documentation": "substring on wide characters (ensure that unicode code points are properly counted)",
      "params": {
        "TestCaseId": "3",
        "Input": "\ufdfd"
      },
      "expect": {
        "error": "No tests matched"
      }
    }
  ]
)
@clientContextParams(
  TestCaseId: {type: "string", documentation: "docs"}
  Input: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
