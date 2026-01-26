$version: "2.0"

namespace smithy.tests.endpointrules.split

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait.smithy"
])
@clientContextParams(
    Input: {
        type: "string"
        documentation: "Input string to split"
    }
    Delimiter: {
        type: "string"
        documentation: "Delimiter to split by"
    }
    Limit: {
        type: "string"
        documentation: "Split limit"
    }
)
@endpointBdd(
    version: "1.3"
    parameters: {
        Input: {
            required: true
            documentation: "The input string to split"
            type: "string"
        }
        Delimiter: {
            required: true
            documentation: "The delimiter to split by"
            type: "string"
        }
        Limit: {
            required: true
            documentation: "The split limit as a string"
            type: "string"
        }
    }
    conditions: [
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "Limit"
                }
                "0"
            ]
        }
        {
            fn: "split"
            argv: [
                "{Input}"
                "{Delimiter}"
                0
            ]
            assign: "parts_ssa_1"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_1"
                        }
                        "[0]"
                    ]
                }
                "<null>"
            ]
            assign: "part1"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_1"
                        }
                        "[1]"
                    ]
                }
                "<null>"
            ]
            assign: "part2"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_1"
                        }
                        "[2]"
                    ]
                }
                "<null>"
            ]
            assign: "part3"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_1"
                        }
                        "[3]"
                    ]
                }
                "<null>"
            ]
            assign: "part4"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_1"
                        }
                        "[4]"
                    ]
                }
                "<null>"
            ]
            assign: "part5"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "Limit"
                }
                "1"
            ]
        }
        {
            fn: "split"
            argv: [
                "{Input}"
                "{Delimiter}"
                1
            ]
            assign: "parts_ssa_2"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_2"
                        }
                        "[0]"
                    ]
                }
                "<null>"
            ]
            assign: "part1"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_2"
                        }
                        "[1]"
                    ]
                }
                "<null>"
            ]
            assign: "part2"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_2"
                        }
                        "[2]"
                    ]
                }
                "<null>"
            ]
            assign: "part3"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_2"
                        }
                        "[3]"
                    ]
                }
                "<null>"
            ]
            assign: "part4"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_2"
                        }
                        "[4]"
                    ]
                }
                "<null>"
            ]
            assign: "part5"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "Limit"
                }
                "2"
            ]
        }
        {
            fn: "split"
            argv: [
                "{Input}"
                "{Delimiter}"
                2
            ]
            assign: "parts_ssa_3"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_3"
                        }
                        "[0]"
                    ]
                }
                "<null>"
            ]
            assign: "part1"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_3"
                        }
                        "[1]"
                    ]
                }
                "<null>"
            ]
            assign: "part2"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_3"
                        }
                        "[2]"
                    ]
                }
                "<null>"
            ]
            assign: "part3"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_3"
                        }
                        "[3]"
                    ]
                }
                "<null>"
            ]
            assign: "part4"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_3"
                        }
                        "[4]"
                    ]
                }
                "<null>"
            ]
            assign: "part5"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "Limit"
                }
                "3"
            ]
        }
        {
            fn: "split"
            argv: [
                "{Input}"
                "{Delimiter}"
                3
            ]
            assign: "parts_ssa_4"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_4"
                        }
                        "[0]"
                    ]
                }
                "<null>"
            ]
            assign: "part1"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_4"
                        }
                        "[1]"
                    ]
                }
                "<null>"
            ]
            assign: "part2"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_4"
                        }
                        "[2]"
                    ]
                }
                "<null>"
            ]
            assign: "part3"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_4"
                        }
                        "[3]"
                    ]
                }
                "<null>"
            ]
            assign: "part4"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_4"
                        }
                        "[4]"
                    ]
                }
                "<null>"
            ]
            assign: "part5"
        }
        {
            fn: "stringEquals"
            argv: [
                {
                    ref: "Limit"
                }
                "4"
            ]
        }
        {
            fn: "split"
            argv: [
                "{Input}"
                "{Delimiter}"
                4
            ]
            assign: "parts_ssa_5"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_5"
                        }
                        "[0]"
                    ]
                }
                "<null>"
            ]
            assign: "part1"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_5"
                        }
                        "[1]"
                    ]
                }
                "<null>"
            ]
            assign: "part2"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_5"
                        }
                        "[2]"
                    ]
                }
                "<null>"
            ]
            assign: "part3"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_5"
                        }
                        "[3]"
                    ]
                }
                "<null>"
            ]
            assign: "part4"
        }
        {
            fn: "coalesce"
            argv: [
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "parts_ssa_5"
                        }
                        "[4]"
                    ]
                }
                "<null>"
            ]
            assign: "part5"
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: "https://example.com"
                properties: {
                    splitResult: "p1={part1}; p2={part2}; p3={part3}; p4={part4}; p5={part5}"
                }
                headers: {}
            }
            type: "endpoint"
        }
        {
            documentation: "error fallthrough"
            conditions: []
            error: "endpoint error"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 36
    nodes: "/////wAAAAH/////AAAAAAAAAAMAAAAJAAAAAQAAAAQAAAAJAAAAAgAAAAUAAAAJAAAAAwAAAAYAAAAJAAAABAAAAAcAAAAJAAAABQAAAAgAAAAJAAAABgX14QEAAAAJAAAABwAAAAoAAAAQAAAACAAAAAsAAAAQAAAACQAAAAwAAAAQAAAACgAAAA0AAAAQAAAACwAAAA4AAAAQAAAADAAAAA8AAAAQAAAADQX14QEAAAAQAAAADgAAABEAAAAXAAAADwAAABIAAAAXAAAAEAAAABMAAAAXAAAAEQAAABQAAAAXAAAAEgAAABUAAAAXAAAAEwAAABYAAAAXAAAAFAX14QEAAAAXAAAAFQAAABgAAAAeAAAAFgAAABkAAAAeAAAAFwAAABoAAAAeAAAAGAAAABsAAAAeAAAAGQAAABwAAAAeAAAAGgAAAB0AAAAeAAAAGwX14QEAAAAeAAAAHAAAAB8F9eECAAAAHQAAACAF9eECAAAAHgAAACEF9eECAAAAHwAAACIF9eECAAAAIAAAACMF9eECAAAAIQAAACQF9eECAAAAIgX14QEF9eEC"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "basic three-part split"
            params: {
                Input: "a--b--c"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "empty string returns single empty element"
            params: {
                Input: ""
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "delimiter not found returns original string"
            params: {
                Input: "no-delimiter"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=no-delimiter; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "leading delimiter creates empty first element"
            params: {
                Input: "--leading"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=leading; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "trailing delimiter creates empty last element"
            params: {
                Input: "trailing--"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=trailing; p2=; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "adjacent delimiters create empty element"
            params: {
                Input: "----"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=; p3=; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "delimiter equals input creates two empty strings"
            params: {
                Input: "--"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "overlapping delimiter pattern"
            params: {
                Input: "aaaa"
                Delimiter: "aa"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=; p3=; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "overlapping delimiter with odd remainder"
            params: {
                Input: "aaa"
                Delimiter: "aa"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=a; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "multi-character delimiter"
            params: {
                Input: "foo<=>bar<=>baz"
                Delimiter: "<=>"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=foo; p2=bar; p3=baz; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "multi-character delimiter with limit"
            params: {
                Input: "foo<=>bar<=>baz"
                Delimiter: "<=>"
                Limit: "2"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=foo; p2=bar<=>baz; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "both leading and trailing delimiters"
            params: {
                Input: "--both--"
                Delimiter: "--"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=both; p3=; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "both leading and trailing with limit"
            params: {
                Input: "--both--"
                Delimiter: "--"
                Limit: "2"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=both--; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 1 returns original string"
            params: {
                Input: "a--b--c"
                Delimiter: "--"
                Limit: "1"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a--b--c; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 1 with no delimiter"
            params: {
                Input: "no-delimiter"
                Delimiter: "--"
                Limit: "1"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=no-delimiter; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 2 splits once"
            params: {
                Input: "a--b--c"
                Delimiter: "--"
                Limit: "2"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b--c; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 2 with leading delimiter"
            params: {
                Input: "--x-s3--azid"
                Delimiter: "--"
                Limit: "2"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=x-s3--azid; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 3 exact match"
            params: {
                Input: "a--b--c"
                Delimiter: "--"
                Limit: "3"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 3 with remainder"
            params: {
                Input: "a--b--c--d"
                Delimiter: "--"
                Limit: "3"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c--d; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "S3 Express bucket pattern"
            params: {
                Input: "--x-s3--azid--suffix"
                Delimiter: "--"
                Limit: "4"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=x-s3; p3=azid; p4=suffix; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "limit 4 stops at 4 parts"
            params: {
                Input: "a--b--c--d--e"
                Delimiter: "--"
                Limit: "4"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c; p4=d--e; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "unicode emoji delimiter"
            params: {
                Input: "a🌟b🌟c"
                Delimiter: "🌟"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "regex-like pattern treated literally"
            params: {
                Input: "a.*b.*c"
                Delimiter: ".*"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "pipe delimiter"
            params: {
                Input: "a|b|c|d|e"
                Delimiter: "|"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=a; p2=b; p3=c; p4=d; p5=e"
                    }
                }
            }
        }
        {
            documentation: "delimiter longer than input"
            params: {
                Input: "ab"
                Delimiter: "abcd"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=ab; p2=<null>; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
        {
            documentation: "delimiter equals entire input"
            params: {
                Input: "abc"
                Delimiter: "abc"
                Limit: "0"
            }
            expect: {
                endpoint: {
                    url: "https://example.com"
                    properties: {
                        splitResult: "p1=; p2=; p3=<null>; p4=<null>; p5=<null>"
                    }
                }
            }
        }
    ]
)
service FizzBuzz {
}
