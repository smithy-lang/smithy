$version: "2.0"

namespace smithy.tests.endpointrules.stringarray

use smithy.rules#endpointBdd
use smithy.rules#endpointTests
use smithy.rules#operationContextParams
use smithy.rules#staticContextParams

@suppress([
    "UnstableTrait"
])
@endpointBdd(
    version: "1.1"
    parameters: {
        stringArrayParam: {
            required: true
            default: [
                "defaultValue1"
                "defaultValue2"
            ]
            documentation: "docs"
            type: "stringArray"
        }
    }
    conditions: [
        {
            fn: "getAttr"
            argv: [
                {
                    ref: "stringArrayParam"
                }
                "[0]"
            ]
            assign: "arrayValue"
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: "https://example.com/{arrayValue}"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            documentation: "error fallthrough"
            conditions: []
            error: "no array values set"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 2
    nodes: "/////wAAAAH/////AAAAAAX14QEF9eEC"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "Default array values used"
            params: {}
            expect: {
                endpoint: {
                    url: "https://example.com/defaultValue1"
                }
            }
            operationInputs: [
                {
                    operationName: "NoBindingsOperation"
                }
            ]
        }
        {
            documentation: "Empty array"
            params: {
                stringArrayParam: []
            }
            expect: {
                error: "no array values set"
            }
            operationInputs: [
                {
                    operationName: "EmptyStaticContextOperation"
                }
            ]
        }
        {
            documentation: "Static value"
            params: {
                stringArrayParam: [
                    "staticValue1"
                ]
            }
            expect: {
                endpoint: {
                    url: "https://example.com/staticValue1"
                }
            }
            operationInputs: [
                {
                    operationName: "StaticContextOperation"
                }
            ]
        }
        {
            documentation: "bound value from input"
            params: {
                stringArrayParam: [
                    "key1"
                ]
            }
            expect: {
                endpoint: {
                    url: "https://example.com/key1"
                }
            }
            operationInputs: [
                {
                    operationName: "ListOfObjectsOperation"
                    operationParams: {
                        nested: {
                            listOfObjects: [
                                {
                                    key: "key1"
                                }
                            ]
                        }
                    }
                }
                {
                    operationName: "MapOperation"
                    operationParams: {
                        map: {
                            key1: "value1"
                        }
                    }
                }
                {
                    operationName: "ListOfUnionsOperation"
                    operationParams: {
                        listOfUnions: [
                            {
                                string: "key1"
                            }
                            {
                                object: {
                                    key: "key2"
                                }
                            }
                        ]
                    }
                }
            ]
        }
    ]
)
service EndpointStringArrayService {
    version: "2022-01-01"
    operations: [
        EmptyStaticContextOperation
        ListOfObjectsOperation
        ListOfUnionsOperation
        MapOperation
        NoBindingsOperation
        StaticContextOperation
    ]
}

@suppress([
    "UnstableTrait"
])
@staticContextParams(
    stringArrayParam: {
        value: []
    }
)
operation EmptyStaticContextOperation {
    input := {}
    output: Unit
}

@suppress([
    "UnstableTrait"
])
@operationContextParams(
    stringArrayParam: {
        path: "nested.listOfObjects[*].key"
    }
)
operation ListOfObjectsOperation {
    input := {
        nested: Nested
    }
    output: Unit
}

@suppress([
    "UnstableTrait"
])
@operationContextParams(
    stringArrayParam: {
        path: "listOfUnions[*][string, object.key][]"
    }
)
operation ListOfUnionsOperation {
    input := {
        listOfUnions: ListOfUnions
    }
    output: Unit
}

@suppress([
    "UnstableTrait"
])
@operationContextParams(
    stringArrayParam: {
        path: "keys(map)"
    }
)
operation MapOperation {
    input := {
        map: Map
    }
    output: Unit
}

operation NoBindingsOperation {
    input := {}
    output: Unit
}

@suppress([
    "UnstableTrait"
])
@staticContextParams(
    stringArrayParam: {
        value: [
            "staticValue1"
        ]
    }
)
operation StaticContextOperation {
    input := {}
    output: Unit
}

structure Nested {
    listOfObjects: ListOfObjects
}

structure ObjectMember {
    key: String
}

union UnionMember {
    string: String
    object: ObjectMember
}

list ListOfObjects {
    member: ObjectMember
}

list ListOfUnions {
    member: UnionMember
}

map Map {
    key: String
    value: String
}
