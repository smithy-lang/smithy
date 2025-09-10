$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Input": {
            "type": "string",
            "required": true,
            "documentation": "The input string to split"
        },
        "Delimiter": {
            "type": "string",
            "required": true,
            "documentation": "The delimiter to split by"
        },
        "Limit": {
            "type": "string",
            "required": true,
            "documentation": "The split limit as a string"
        }
    },
    "rules": [
        {
            "documentation": "Split with limit 0 (unlimited)",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": ["{Limit}", "0"]
                },
                {
                    "fn": "split",
                    "argv": ["{Input}", "{Delimiter}", 0],
                    "assign": "parts"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[0]"]
                        },
                        "<null>"
                    ],
                    "assign": "part1"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[1]"]
                        },
                        "<null>"
                    ],
                    "assign": "part2"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[2]"]
                        },
                        "<null>"
                    ],
                    "assign": "part3"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[3]"]
                        },
                        "<null>"
                    ],
                    "assign": "part4"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[4]"]
                        },
                        "<null>"
                    ],
                    "assign": "part5"
                }
            ],
            "endpoint": {
                "url": "https://example.com",
                "properties": {
                    "splitResult": "p1={part1}; p2={part2}; p3={part3}; p4={part4}; p5={part5}"
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "Split with limit 1 (no split)",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": ["{Limit}", "1"]
                },
                {
                    "fn": "split",
                    "argv": ["{Input}", "{Delimiter}", 1],
                    "assign": "parts"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[0]"]
                        },
                        "<null>"
                    ],
                    "assign": "part1"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[1]"]
                        },
                        "<null>"
                    ],
                    "assign": "part2"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[2]"]
                        },
                        "<null>"
                    ],
                    "assign": "part3"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[3]"]
                        },
                        "<null>"
                    ],
                    "assign": "part4"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[4]"]
                        },
                        "<null>"
                    ],
                    "assign": "part5"
                }
            ],
            "endpoint": {
                "url": "https://example.com",
                "properties": {
                    "splitResult": "p1={part1}; p2={part2}; p3={part3}; p4={part4}; p5={part5}"
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "Split with limit 2",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": ["{Limit}", "2"]
                },
                {
                    "fn": "split",
                    "argv": ["{Input}", "{Delimiter}", 2],
                    "assign": "parts"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[0]"]
                        },
                        "<null>"
                    ],
                    "assign": "part1"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[1]"]
                        },
                        "<null>"
                    ],
                    "assign": "part2"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[2]"]
                        },
                        "<null>"
                    ],
                    "assign": "part3"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[3]"]
                        },
                        "<null>"
                    ],
                    "assign": "part4"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[4]"]
                        },
                        "<null>"
                    ],
                    "assign": "part5"
                }
            ],
            "endpoint": {
                "url": "https://example.com",
                "properties": {
                    "splitResult": "p1={part1}; p2={part2}; p3={part3}; p4={part4}; p5={part5}"
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "Split with limit 3",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": ["{Limit}", "3"]
                },
                {
                    "fn": "split",
                    "argv": ["{Input}", "{Delimiter}", 3],
                    "assign": "parts"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[0]"]
                        },
                        "<null>"
                    ],
                    "assign": "part1"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[1]"]
                        },
                        "<null>"
                    ],
                    "assign": "part2"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[2]"]
                        },
                        "<null>"
                    ],
                    "assign": "part3"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[3]"]
                        },
                        "<null>"
                    ],
                    "assign": "part4"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[4]"]
                        },
                        "<null>"
                    ],
                    "assign": "part5"
                }
            ],
            "endpoint": {
                "url": "https://example.com",
                "properties": {
                    "splitResult": "p1={part1}; p2={part2}; p3={part3}; p4={part4}; p5={part5}"
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "Split with limit 4",
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": ["{Limit}", "4"]
                },
                {
                    "fn": "split",
                    "argv": ["{Input}", "{Delimiter}", 4],
                    "assign": "parts"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[0]"]
                        },
                        "<null>"
                    ],
                    "assign": "part1"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[1]"]
                        },
                        "<null>"
                    ],
                    "assign": "part2"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[2]"]
                        },
                        "<null>"
                    ],
                    "assign": "part3"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[3]"]
                        },
                        "<null>"
                    ],
                    "assign": "part4"
                },
                {
                    "fn": "coalesce",
                    "argv": [
                        {
                            "fn": "getAttr",
                            "argv": [{"ref": "parts"}, "[4]"]
                        },
                        "<null>"
                    ],
                    "assign": "part5"
                }
            ],
            "endpoint": {
                "url": "https://example.com",
                "properties": {
                    "splitResult": "p1={part1}; p2={part2}; p3={part3}; p4={part4}; p5={part5}"
                }
            },
            "type": "endpoint"
        }
    ]
})
@endpointTests(
    version: "1.0",
    testCases: [
        // Limit 0 tests
        {
            "documentation": "basic three-part split",
            "params": {
                "Input": "a--b--c",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "empty string returns single empty element",
            "params": {
                "Input": "",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "delimiter not found returns original string",
            "params": {
                "Input": "no-delimiter",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=no-delimiter; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "leading delimiter creates empty first element",
            "params": {
                "Input": "--leading",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=leading; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "trailing delimiter creates empty last element",
            "params": {
                "Input": "trailing--",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=trailing; p2=; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "adjacent delimiters create empty element",
            "params": {
                "Input": "----",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=; p3=; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "delimiter equals input creates two empty strings",
            "params": {
                "Input": "--",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "overlapping delimiter pattern",
            "params": {
                "Input": "aaaa",
                "Delimiter": "aa",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=; p3=; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "overlapping delimiter with odd remainder",
            "params": {
                "Input": "aaa",
                "Delimiter": "aa",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=a; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "multi-character delimiter",
            "params": {
                "Input": "foo<=>bar<=>baz",
                "Delimiter": "<=>",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=foo; p2=bar; p3=baz; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "multi-character delimiter with limit",
            "params": {
                "Input": "foo<=>bar<=>baz",
                "Delimiter": "<=>",
                "Limit": "2"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=foo; p2=bar<=>baz; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "both leading and trailing delimiters",
            "params": {
                "Input": "--both--",
                "Delimiter": "--",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=both; p3=; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "both leading and trailing with limit",
            "params": {
                "Input": "--both--",
                "Delimiter": "--",
                "Limit": "2"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=both--; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },

        // Limit 1 tests (no split)
        {
            "documentation": "limit 1 returns original string",
            "params": {
                "Input": "a--b--c",
                "Delimiter": "--",
                "Limit": "1"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a--b--c; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "limit 1 with no delimiter",
            "params": {
                "Input": "no-delimiter",
                "Delimiter": "--",
                "Limit": "1"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=no-delimiter; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },

        // Limit 2 tests
        {
            "documentation": "limit 2 splits once",
            "params": {
                "Input": "a--b--c",
                "Delimiter": "--",
                "Limit": "2"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b--c; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "limit 2 with leading delimiter",
            "params": {
                "Input": "--x-s3--azid",
                "Delimiter": "--",
                "Limit": "2"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=x-s3--azid; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },

        // Limit 3 tests
        {
            "documentation": "limit 3 exact match",
            "params": {
                "Input": "a--b--c",
                "Delimiter": "--",
                "Limit": "3"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "limit 3 with remainder",
            "params": {
                "Input": "a--b--c--d",
                "Delimiter": "--",
                "Limit": "3"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c--d; p4=<null>; p5=<null>"
                    }
                }
            }
        },

        // Limit 4 tests
        {
            "documentation": "S3 Express bucket pattern",
            "params": {
                "Input": "--x-s3--azid--suffix",
                "Delimiter": "--",
                "Limit": "4"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=x-s3; p3=azid; p4=suffix; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "limit 4 stops at 4 parts",
            "params": {
                "Input": "a--b--c--d--e",
                "Delimiter": "--",
                "Limit": "4"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c; p4=d--e; p5=<null>"
                    }
                }
            }
        },

        // Unicode tests
        {
            "documentation": "unicode emoji delimiter",
            "params": {
                "Input": "a🌟b🌟c",
                "Delimiter": "🌟",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        },

        // Regex treated literally
        {
            "documentation": "regex-like pattern treated literally",
            "params": {
                "Input": "a.*b.*c",
                "Delimiter": ".*",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "pipe delimiter",
            "params": {
                "Input": "a|b|c|d|e",
                "Delimiter": "|",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=a; p2=b; p3=c; p4=d; p5=e"
                    }
                }
            }
        },

        // Edge cases
        {
            "documentation": "delimiter longer than input",
            "params": {
                "Input": "ab",
                "Delimiter": "abcd",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=ab; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        },
        {
            "documentation": "delimiter equals entire input",
            "params": {
                "Input": "abc",
                "Delimiter": "abc",
                "Limit": "0"
            },
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {
                        "splitResult": "p1=; p2=; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
    ]
)
@clientContextParams(
    Input: {type: "string", documentation: "Input string to split"}
    Delimiter: {type: "string", documentation: "Delimiter to split by"}
    Limit: {type: "string", documentation: "Split limit"}
)
@suppress(["UnstableTrait.smithy"])
service SplitTestService {}
