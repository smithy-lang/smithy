$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply NamedScalarMembers @httpRequestTests([
    {
        id: "RpcV2JsonNamedScalarMembersSerialize"
        documentation: """
            rpcv2Json ignores @jsonName and @xmlName, so every member
            serializes under its own name even though both traits are present
            on all of them"""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedScalarMembers"
        body: """
            {
                "booleanMember": true,
                "byteMember": 7,
                "shortMember": 300,
                "integerMember": 70000,
                "longMember": 9000000000,
                "floatMember": 3.25,
                "doubleMember": 6.125,
                "stringMember": "named",
                "blobMember": "YmFy",
                "timestampMember": 1609502096,
                "enumMember": "Bar",
                "intEnumMember": 2
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleanMember: true
            byteMember: 7
            shortMember: 300
            integerMember: 70000
            longMember: 9000000000
            floatMember: 3.25
            doubleMember: 6.125
            stringMember: "named"
            blobMember: "bar"
            timestampMember: 1609502096
            enumMember: "Bar"
            intEnumMember: 2
        }
    }
])

apply NamedScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonNamedScalarMembersDeserialize"
        documentation: """
            rpcv2Json ignores @jsonName and @xmlName, so every member
            deserializes from its own name even though both traits are present
            on all of them"""
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleanMember": true,
                "byteMember": 7,
                "shortMember": 300,
                "integerMember": 70000,
                "longMember": 9000000000,
                "floatMember": 3.25,
                "doubleMember": 6.125,
                "stringMember": "named",
                "blobMember": "YmFy",
                "timestampMember": 1609502096,
                "enumMember": "Bar",
                "intEnumMember": 2
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleanMember: true
            byteMember: 7
            shortMember: 300
            integerMember: 70000
            longMember: 9000000000
            floatMember: 3.25
            doubleMember: 6.125
            stringMember: "named"
            blobMember: "bar"
            timestampMember: 1609502096
            enumMember: "Bar"
            intEnumMember: 2
        }
    }
])

apply NamedStructOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonNamedStructOfScalarsSerialize"
        documentation: "Serializes a nested structure whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedStructOfScalars"
        body: """
            {
                "nested": {
                    "stringMember": "nestedNamed",
                    "integerMember": 91,
                    "booleanMember": true,
                    "timestampMember": 1609588496
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            nested: { stringMember: "nestedNamed", integerMember: 91, booleanMember: true, timestampMember: 1609588496 }
        }
    }
])

apply NamedStructOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonNamedStructOfScalarsDeserialize"
        documentation: "Deserializes a nested structure whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "nested": {
                    "stringMember": "nestedNamed",
                    "integerMember": 91,
                    "booleanMember": true,
                    "timestampMember": 1609588496
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            nested: { stringMember: "nestedNamed", integerMember: 91, booleanMember: true, timestampMember: 1609588496 }
        }
    }
])

apply NamedListOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonNamedListOfScalarsSerialize"
        documentation: "Serializes lists under their member names, ignoring @jsonName and @xmlName"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedListOfScalars"
        body: """
            {
                "strings": ["alpha", "beta"],
                "integers": [11, 22],
                "timestamps": [1609502096, 1609588496]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            strings: ["alpha", "beta"]
            integers: [11, 22]
            timestamps: [1609502096, 1609588496]
        }
    }
])

apply NamedListOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonNamedListOfScalarsDeserialize"
        documentation: "Deserializes lists under their member names, ignoring @jsonName and @xmlName"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "strings": ["alpha", "beta"],
                "integers": [11, 22],
                "timestamps": [1609502096, 1609588496]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            strings: ["alpha", "beta"]
            integers: [11, 22]
            timestamps: [1609502096, 1609588496]
        }
    }
])

apply NamedMapOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonNamedMapOfScalarsSerialize"
        documentation: "Serializes maps under their member names, ignoring @jsonName and @xmlName"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedMapOfScalars"
        body: """
            {
                "strings": {
                    "stringKeyOne": "gamma",
                    "stringKeyTwo": "delta"
                },
                "integers": {
                    "integerKeyOne": 33,
                    "integerKeyTwo": 44
                },
                "timestamps": {
                    "timestampKeyOne": 1609674896,
                    "timestampKeyTwo": 1609761296
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            strings: { stringKeyOne: "gamma", stringKeyTwo: "delta" }
            integers: { integerKeyOne: 33, integerKeyTwo: 44 }
            timestamps: { timestampKeyOne: 1609674896, timestampKeyTwo: 1609761296 }
        }
    }
])

apply NamedMapOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonNamedMapOfScalarsDeserialize"
        documentation: "Deserializes maps under their member names, ignoring @jsonName and @xmlName"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "strings": {
                    "stringKeyOne": "gamma",
                    "stringKeyTwo": "delta"
                },
                "integers": {
                    "integerKeyOne": 33,
                    "integerKeyTwo": 44
                },
                "timestamps": {
                    "timestampKeyOne": 1609674896,
                    "timestampKeyTwo": 1609761296
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            strings: { stringKeyOne: "gamma", stringKeyTwo: "delta" }
            integers: { integerKeyOne: 33, integerKeyTwo: 44 }
            timestamps: { timestampKeyOne: 1609674896, timestampKeyTwo: 1609761296 }
        }
    }
])

apply NamedUnionMembers @httpRequestTests([
    {
        id: "RpcV2JsonNamedUnionStringSerialize"
        documentation: "Serializes the string variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedUnionMembers"
        body: """
            {
                "value": {
                    "stringMember": "unionString"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { stringMember: "unionString" }
        }
    }
    {
        id: "RpcV2JsonNamedUnionIntegerSerialize"
        documentation: "Serializes the integer variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedUnionMembers"
        body: """
            {
                "value": {
                    "integerMember": 55
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { integerMember: 55 }
        }
    }
    {
        id: "RpcV2JsonNamedUnionBooleanSerialize"
        documentation: "Serializes the boolean variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedUnionMembers"
        body: """
            {
                "value": {
                    "booleanMember": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { booleanMember: true }
        }
    }
    {
        id: "RpcV2JsonNamedUnionListSerialize"
        documentation: "Serializes the list variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedUnionMembers"
        body: """
            {
                "value": {
                    "listMember": ["epsilon", "zeta"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                listMember: ["epsilon", "zeta"]
            }
        }
    }
    {
        id: "RpcV2JsonNamedUnionMapSerialize"
        documentation: "Serializes the map variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedUnionMembers"
        body: """
            {
                "value": {
                    "mapMember": {
                        "mapKeyOne": "eta",
                        "mapKeyTwo": "theta"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                mapMember: { mapKeyOne: "eta", mapKeyTwo: "theta" }
            }
        }
    }
    {
        id: "RpcV2JsonNamedUnionStructSerialize"
        documentation: "Serializes the structure variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NamedUnionMembers"
        body: """
            {
                "value": {
                    "structMember": {
                        "stringMember": "unionNested",
                        "integerMember": 63,
                        "booleanMember": true,
                        "timestampMember": 1609674896
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                structMember: { stringMember: "unionNested", integerMember: 63, booleanMember: true, timestampMember: 1609674896 }
            }
        }
    }
])

apply NamedUnionMembers @httpResponseTests([
    {
        id: "RpcV2JsonNamedUnionStringDeserialize"
        documentation: "Deserializes the string variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "stringMember": "unionString"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { stringMember: "unionString" }
        }
    }
    {
        id: "RpcV2JsonNamedUnionIntegerDeserialize"
        documentation: "Deserializes the integer variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "integerMember": 55
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { integerMember: 55 }
        }
    }
    {
        id: "RpcV2JsonNamedUnionBooleanDeserialize"
        documentation: "Deserializes the boolean variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "booleanMember": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { booleanMember: true }
        }
    }
    {
        id: "RpcV2JsonNamedUnionListDeserialize"
        documentation: "Deserializes the list variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "listMember": ["epsilon", "zeta"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                listMember: ["epsilon", "zeta"]
            }
        }
    }
    {
        id: "RpcV2JsonNamedUnionMapDeserialize"
        documentation: "Deserializes the map variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "mapMember": {
                        "mapKeyOne": "eta",
                        "mapKeyTwo": "theta"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                mapMember: { mapKeyOne: "eta", mapKeyTwo: "theta" }
            }
        }
    }
    {
        id: "RpcV2JsonNamedUnionStructDeserialize"
        documentation: "Deserializes the structure variant of a union whose members carry naming traits, which rpcv2Json ignores"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "structMember": {
                        "stringMember": "unionNested",
                        "integerMember": 63,
                        "booleanMember": true,
                        "timestampMember": 1609674896
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                structMember: { stringMember: "unionNested", integerMember: 63, booleanMember: true, timestampMember: 1609674896 }
            }
        }
    }
])
