$version: "2.0"

metadata suppressions = [
    {
        id: "UnstableTrait.smithy.api#discriminated"
        namespace: "smithy.protocoltests.rpcv2Json"
        reason: "These protocol tests intentionally exercise the unstable discriminated trait."
    }
]

namespace smithy.protocoltests.rpcv2Json

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// Support for the discriminated trait is optional. Every test case is tagged
// "discriminated-unions" and uses the DiscriminatedUnionOperation operation, so
// implementations that do not support the trait can skip these tests by tag or
// by operation.
//
// Two rules of the trait are not covered here because these test traits cannot
// express them: a missing, null, or non-string discriminator SHOULD be reported
// as an error, and a discriminator value that names no member is handled like an
// unrecognized member of a standard union.
@httpRequestTests([
    {
        id: "RpcV2JsonRequestDiscriminatedUnion"
        protocol: rpcv2Json
        documentation: """
            Serializes a discriminated union as a flat object, with the discriminator
            field naming the member that is set."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shape": {
                    "type": "circle",
                    "radius": 5
                }
            }"""
        params: {
            shape: {
                circle: { radius: 5 }
            }
        }
    }
    {
        id: "RpcV2JsonRequestDiscriminatedUnionMultipleMembers"
        protocol: rpcv2Json
        documentation: """
            Serializes a discriminated union whose set member targets a structure with
            several members, all serialized alongside the discriminator field."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shape": {
                    "type": "rectangle",
                    "width": 3,
                    "height": 4
                }
            }"""
        params: {
            shape: {
                rectangle: { width: 3, height: 4 }
            }
        }
    }
    {
        id: "RpcV2JsonRequestDiscriminatedUnionDefaultDiscriminator"
        protocol: rpcv2Json
        documentation: """
            Serializes a discriminated union whose trait has no value, which uses the
            default discriminator field name, __type."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "defaultShape": {
                    "__type": "circle",
                    "radius": 5
                }
            }"""
        params: {
            defaultShape: {
                circle: { radius: 5 }
            }
        }
    }
    {
        id: "RpcV2JsonRequestNestedDiscriminatedUnion"
        protocol: rpcv2Json
        documentation: """
            Serializes a discriminated union whose set member targets a structure that
            contains another discriminated union, which is serialized as a nested
            object with its own discriminator field."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shape": {
                    "type": "frame",
                    "content": {
                        "type": "circle",
                        "radius": 5
                    }
                }
            }"""
        params: {
            shape: {
                frame: {
                    content: {
                        circle: { radius: 5 }
                    }
                }
            }
        }
    }
    {
        id: "RpcV2JsonRequestDiscriminatedUnionDiscriminatorLast"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union whose discriminator field is not the
            first field of the object. Servers MUST accept the discriminator at any
            position."""
        tags: ["discriminated-unions"]
        appliesTo: "server"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shape": {
                    "width": 3,
                    "height": 4,
                    "type": "rectangle"
                }
            }"""
        params: {
            shape: {
                rectangle: { width: 3, height: 4 }
            }
        }
    }
    {
        id: "RpcV2JsonRequestNestedDiscriminatedUnionDiscriminatorsLast"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union that contains another discriminated
            union, where the discriminator field of each object is its last field."""
        tags: ["discriminated-unions"]
        appliesTo: "server"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shape": {
                    "content": {
                        "radius": 5,
                        "type": "circle"
                    },
                    "type": "frame"
                }
            }"""
        params: {
            shape: {
                frame: {
                    content: {
                        circle: { radius: 5 }
                    }
                }
            }
        }
    }
    {
        id: "RpcV2JsonRequestDiscriminatedUnionList"
        protocol: rpcv2Json
        documentation: """
            Serializes a list of discriminated unions, where each element is a flat
            object with its own discriminator field."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shapes": [
                    {
                        "type": "circle",
                        "radius": 5
                    },
                    {
                        "type": "rectangle",
                        "width": 3,
                        "height": 4
                    }
                ]
            }"""
        params: {
            shapes: [
                {
                    circle: { radius: 5 }
                }
                {
                    rectangle: { width: 3, height: 4 }
                }
            ]
        }
    }
    {
        id: "RpcV2JsonRequestStandardUnionInDiscriminatedUnion"
        protocol: rpcv2Json
        documentation: """
            Serializes a discriminated union whose set member targets a structure that
            contains a standard union, which uses the standard union encoding."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/DiscriminatedUnionOperation"
        body: """
            {
                "shape": {
                    "type": "labeled",
                    "label": {
                        "text": "hello"
                    }
                }
            }"""
        params: {
            shape: {
                labeled: {
                    label: { text: "hello" }
                }
            }
        }
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseDiscriminatedUnion"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union from a flat object whose first field is
            the discriminator."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shape": {
                    "type": "rectangle",
                    "width": 3,
                    "height": 4
                }
            }"""
        code: 200
        params: {
            shape: {
                rectangle: { width: 3, height: 4 }
            }
        }
    }
    {
        id: "RpcV2JsonResponseDiscriminatedUnionDiscriminatorLast"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union whose discriminator field is the last
            field of the object."""
        tags: ["discriminated-unions"]
        appliesTo: "client"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shape": {
                    "width": 3,
                    "height": 4,
                    "type": "rectangle"
                }
            }"""
        code: 200
        params: {
            shape: {
                rectangle: { width: 3, height: 4 }
            }
        }
    }
    {
        id: "RpcV2JsonResponseDiscriminatedUnionDiscriminatorInMiddle"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union whose discriminator field is between
            other fields of the object."""
        tags: ["discriminated-unions"]
        appliesTo: "client"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shape": {
                    "width": 3,
                    "type": "rectangle",
                    "height": 4
                }
            }"""
        code: 200
        params: {
            shape: {
                rectangle: { width: 3, height: 4 }
            }
        }
    }
    {
        id: "RpcV2JsonResponseDiscriminatedUnionDefaultDiscriminator"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union whose trait has no value, which uses the
            default discriminator field name, __type."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "defaultShape": {
                    "__type": "circle",
                    "radius": 5
                }
            }"""
        code: 200
        params: {
            defaultShape: {
                circle: { radius: 5 }
            }
        }
    }
    {
        id: "RpcV2JsonResponseNestedDiscriminatedUnionDiscriminatorsLast"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union that contains another discriminated
            union, where the discriminator field of each object is its last field."""
        tags: ["discriminated-unions"]
        appliesTo: "client"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shape": {
                    "content": {
                        "radius": 5,
                        "type": "circle"
                    },
                    "type": "frame"
                }
            }"""
        code: 200
        params: {
            shape: {
                frame: {
                    content: {
                        circle: { radius: 5 }
                    }
                }
            }
        }
    }
    {
        id: "RpcV2JsonResponseDiscriminatedUnionIgnoresUnknownFields"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union while ignoring fields that are not
            members of the set member's target structure, including a __type field
            when the discriminator field has a different name."""
        tags: ["discriminated-unions"]
        appliesTo: "client"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shape": {
                    "type": "circle",
                    "__type": "rectangle",
                    "radius": 5,
                    "unknown": "ignored"
                }
            }"""
        code: 200
        params: {
            shape: {
                circle: { radius: 5 }
            }
        }
    }
    {
        id: "RpcV2JsonResponseDiscriminatedUnionList"
        protocol: rpcv2Json
        documentation: """
            Deserializes a list of discriminated unions, including elements whose
            discriminator field is not the first field."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shapes": [
                    {
                        "type": "circle",
                        "radius": 5
                    },
                    {
                        "width": 3,
                        "height": 4,
                        "type": "rectangle"
                    }
                ]
            }"""
        code: 200
        params: {
            shapes: [
                {
                    circle: { radius: 5 }
                }
                {
                    rectangle: { width: 3, height: 4 }
                }
            ]
        }
    }
    {
        id: "RpcV2JsonResponseStandardUnionInDiscriminatedUnion"
        protocol: rpcv2Json
        documentation: """
            Deserializes a discriminated union whose set member targets a structure that
            contains a standard union, which uses the standard union encoding."""
        tags: ["discriminated-unions"]
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        bodyMediaType: "application/json"
        body: """
            {
                "shape": {
                    "type": "labeled",
                    "label": {
                        "text": "hello"
                    }
                }
            }"""
        code: 200
        params: {
            shape: {
                labeled: {
                    label: { text: "hello" }
                }
            }
        }
    }
])
operation DiscriminatedUnionOperation {
    input: DiscriminatedUnionStructure
    output: DiscriminatedUnionStructure
}

structure DiscriminatedUnionStructure {
    shape: DiscriminatedShape
    defaultShape: DefaultDiscriminatedShape
    shapes: DiscriminatedShapeList
}

list DiscriminatedShapeList {
    member: DiscriminatedShape
}

@discriminated("type")
union DiscriminatedShape {
    circle: DiscriminatedCircle
    rectangle: DiscriminatedRectangle
    frame: DiscriminatedFrame
    labeled: DiscriminatedLabeled
}

@discriminated
union DefaultDiscriminatedShape {
    circle: DiscriminatedCircle
}

structure DiscriminatedCircle {
    radius: Integer
}

structure DiscriminatedRectangle {
    width: Integer
    height: Integer
}

structure DiscriminatedFrame {
    content: DiscriminatedShape
}

structure DiscriminatedLabeled {
    label: DiscriminatedLabel
}

union DiscriminatedLabel {
    text: String
    code: Integer
}
