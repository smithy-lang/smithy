$version: "2.0"

namespace smithy.tests.endpointrules.endpointbindings

use smithy.rules#clientContextParams
use smithy.rules#contextParam
use smithy.rules#endpointBdd
use smithy.rules#endpointTests
use smithy.rules#operationContextParams
use smithy.rules#staticContextParams

@suppress([
    "UnstableTrait"
])
@clientContextParams(
    bar: {
        type: "string"
        documentation: "a client string parameter"
    }
    baz: {
        type: "string"
        documentation: "another client string parameter"
    }
    booleanParam: {
        type: "boolean"
        documentation: "a client boolean parameter"
    }
)
@endpointBdd(
    version: "1.1"
    parameters: {
        bar: {
            required: false
            documentation: "String parameter with no default value and client binding"
            type: "string"
        }
        baz: {
            required: true
            default: "baz"
            documentation: "String parameter with default value and client binding"
            type: "string"
        }
        booleanParam: {
            required: true
            default: true
            documentation: "Boolean parameter with default value and client binding"
            type: "boolean"
        }
        Endpoint: {
            builtIn: "SDK::Endpoint"
            required: false
            documentation: "Override the endpoint used to send this request"
            type: "string"
        }
    }
    conditions: [
        {
            fn: "isSet"
            argv: [
                {
                    ref: "Endpoint"
                }
            ]
        }
        {
            fn: "booleanEquals"
            argv: [
                {
                    ref: "booleanParam"
                }
                true
            ]
        }
        {
            fn: "isSet"
            argv: [
                {
                    ref: "bar"
                }
            ]
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: {
                    ref: "Endpoint"
                }
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "https://{bar}.{baz}/set"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "https://{baz}/set"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "https://{bar}.{baz}/unset"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "https://{baz}/unset"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
    ]
    root: 2
    nodeCount: 5
    nodes: "/////wAAAAH/////AAAAAAX14QEAAAADAAAAAQAAAAUAAAAEAAAAAgX14QQF9eEFAAAAAgX14QIF9eED"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "Custom Endpoint used"
            params: {
                Endpoint: "https://custom-endpoint.com"
            }
            expect: {
                endpoint: {
                    url: "https://custom-endpoint.com"
                }
            }
            operationInputs: [
                {
                    operationName: "NoBindingsOperation"
                    builtInParams: {
                        "SDK::Endpoint": "https://custom-endpoint.com"
                    }
                }
            ]
        }
        {
            documentation: "Default values used"
            params: {}
            expect: {
                endpoint: {
                    url: "https://baz/set"
                }
            }
            operationInputs: [
                {
                    operationName: "NoBindingsOperation"
                }
            ]
        }
        {
            documentation: "Client config used over default"
            params: {
                baz: "client-config"
            }
            expect: {
                endpoint: {
                    url: "https://client-config/set"
                }
            }
            operationInputs: [
                {
                    operationName: "NoBindingsOperation"
                    clientParams: {
                        baz: "client-config"
                    }
                }
            ]
        }
        {
            documentation: "StaticContextParam values used"
            params: {
                bar: "static-context"
                booleanParam: false
            }
            expect: {
                endpoint: {
                    url: "https://static-context.baz/unset"
                }
            }
            operationInputs: [
                {
                    operationName: "StaticContextOperation"
                }
            ]
        }
        {
            documentation: "ContextParam values used over config and defaults"
            params: {
                bar: "context-bar"
                baz: "context-baz"
                booleanParam: false
            }
            expect: {
                endpoint: {
                    url: "https://context-bar.context-baz/unset"
                }
            }
            operationInputs: [
                {
                    operationName: "ContextParamsOperation"
                    operationParams: {
                        bar: "context-bar"
                        baz: "context-baz"
                        booleanParam: false
                    }
                    clientParams: {
                        bar: "client-config"
                    }
                }
            ]
        }
        {
            documentation: "OperationContextParam values used over config and defaults"
            params: {
                bar: "operation-context-bar"
                baz: "operation-context-baz"
                booleanParam: false
            }
            expect: {
                endpoint: {
                    url: "https://operation-context-bar.operation-context-baz/unset"
                }
            }
            operationInputs: [
                {
                    operationName: "OperationContextParamsOperation"
                    operationParams: {
                        nested: {
                            bar: "operation-context-bar"
                            baz: "operation-context-baz"
                        }
                        booleanParam: false
                    }
                    clientParams: {
                        bar: "client-config"
                    }
                }
            ]
        }
    ]
)
service EndpointBindingService {
    version: "2022-01-01"
    operations: [
        ContextParamsOperation
        NoBindingsOperation
        OperationContextParamsOperation
        StaticContextOperation
    ]
}

operation ContextParamsOperation {
    input := {
        @contextParam(
            name: "bar"
        )
        bar: String
        @contextParam(
            name: "baz"
        )
        baz: String
        @contextParam(
            name: "booleanParam"
        )
        booleanParam: Boolean
    }
    output: Unit
}

operation NoBindingsOperation {
    input := {}
    output: Unit
}

@operationContextParams(
    bar: {
        path: "nested.bar"
    }
    baz: {
        path: "nested.baz"
    }
    booleanParam: {
        path: "booleanParam"
    }
)
operation OperationContextParamsOperation {
    input := {
        nested: Nested
        booleanParam: Boolean
    }
    output: Unit
}

@suppress([
    "UnstableTrait"
])
@staticContextParams(
    bar: {
        value: "static-context"
    }
    booleanParam: {
        value: false
    }
)
operation StaticContextOperation {
    input := {}
    output: Unit
}

structure Nested {
    bar: String
    baz: String
}

structure ObjectMember {
    key: String
}

list ListOfObjects {
    member: ObjectMember
}

map Map {
    key: String
    value: String
}
