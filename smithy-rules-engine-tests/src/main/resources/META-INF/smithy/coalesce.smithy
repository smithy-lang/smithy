$version: "2.0"

namespace smithy.rules.tests

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@suppress(["UnstableTrait", "RuleSetParameter.TestCase.Unused"])
@clientContextParams(
    "TestCaseId": {
            "type": "string",
            "required": true,
            "documentation": "Test case id used to select the test case to use"
        }
    req1: {
        type: "string",
        documentation: "docs",
    }
    req2: {
        type: "string",
        documentation: "docs",
    }
    opt1: {
        type: "string",
        documentation: "always Some"
        
    }
    opt2: {
        type: "string",
        documentation: "always Some"
    }
)
@endpointRuleSet({
    version: "1.1",
    parameters: {
        "TestCaseId": {
            "type": "string",
            "required": true,
            "documentation": "Test case id used to select the test case to use"
        }
        req1: {
            type: "string",
            documentation: "req1",
            required: true,
            default: "req1Value"
        }
        req2: {
            type: "string",
            documentation: "req2",
            required: true,
            default: "req2Value"
        }
        opt1: {
            type: "string",
            documentation: "opt1"
            
        }
        opt2: {
            type: "string",
            documentation: "opt2"
        }
    },
    rules: [
        {
            "documentation": "Two required",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": [
                        "{TestCaseId}",
                        "0"
                    ]
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {"ref": "req1"},
                        {"ref": "req2"},
                    ],
                    "assign": "req1req2"
                },
            ],
            "error": "The value is: {req1req2}",
            "type": "error"
        },
        {
            "documentation": "Two optional",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": [
                        "{TestCaseId}",
                        "1"
                    ]
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {"ref": "opt1"},
                        {"ref": "opt2"},
                    ],
                    "assign": "opt1opt2"
                },
            ],
            "error": "The value is: {opt1opt2}",
            "type": "error"
        },
        {
            "documentation": "Required then Optional",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": [
                        "{TestCaseId}",
                        "2"
                    ]
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {"ref": "req1"},
                        {"ref": "opt1"},
                    ],
                    "assign": "req1opt1"
                },
            ],
            "error": "The value is: {req1opt1}",
            "type": "error"
        },
        {
            "documentation": "Optional then Required",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": [
                        "{TestCaseId}",
                        "3"
                    ]
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {"ref": "opt1"},
                        {"ref": "req1"},
                    ],
                    "assign": "opt1req1"
                },
            ],
            "error": "The value is: {opt1req1}",
            "type": "error"
        },
        {
            "conditions": [],
            "documentation": "error fallthrough",
            "error": "endpoint error",
            "type": "error"
        }
    ]
})
@endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "documentation": "Two required, first val returned",
            "params": {
                "TestCaseId": "0"
            },
            "expect": {
                "error": "The value is: req1Value"
            }
        },
        {
            "documentation": "Two optional, Some(opt1Value), Some(opt2Value), opt1Value returned",
            "params": {
                "TestCaseId": "1",
                "opt1": "opt1Value",
                "opt2": "opt2Value",
            },
            "expect": {
                "error": "The value is: opt1Value"
            }
        },
        {
            "documentation": "Two optional, None, Some(opt2Value), opt2Value returned",
            "params": {
                "TestCaseId": "1",
                "opt2": "opt2Value",
            },
            "expect": {
                "error": "The value is: opt2Value"
            }
        },
        {
            "documentation": "Two optional, None, None, None returned",
            "params": {
                "TestCaseId": "1",
            },
            "expect": {
                "error": "endpoint error"
            }
        },
        {
            "documentation": "Required then Optional, required returned",
            "params": {
                "TestCaseId": "2",
                "opt1": "opt1Value",
            },
            "expect": {
                "error": "The value is: req1Value"
            }
        },
        {
            "documentation": "Optional then Required, optional value returned",
            "params": {
                "TestCaseId": "3",
                "opt1": "opt1Value",
            },
            "expect": {
                "error": "The value is: opt1Value"
            }
        },
        {
            "documentation": "Optional then Required, optional is none so required value returned",
            "params": {
                "TestCaseId": "3",
            },
            "expect": {
                "error": "The value is: req1Value"
            }
        },
    ]
})
service CoalesceTest {}
