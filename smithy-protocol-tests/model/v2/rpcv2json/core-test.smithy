$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply ScalarMembers @httpRequestTests([
    {
        id: "RpcV2JsonScalarMembersSerialize"
        documentation: "Serializes all scalar members"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "booleanMember": true,
                "byteMember": 5,
                "shortMember": 256,
                "integerMember": 1234,
                "longMember": 999999999999,
                "floatMember": 1.5,
                "doubleMember": 2.5,
                "stringMember": "hello",
                "blobMember": "Zm9v",
                "dateTimeMember": 1609504496,
                "epochSecondsMember": 1612325106,
                "httpDateMember": 1614834367,
                "stringEnum": "Foo",
                "intEnum": 1
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleanMember: true
            byteMember: 5
            shortMember: 256
            integerMember: 1234
            longMember: 999999999999
            floatMember: 1.5
            doubleMember: 2.5
            stringMember: "hello"
            blobMember: "foo"
            dateTimeMember: 1609504496
            epochSecondsMember: 1612325106
            httpDateMember: 1614834367
            stringEnum: "Foo"
            intEnum: 1
        }
    }
    {
        id: "RpcV2JsonScalarMembersSerializeZeroValues"
        documentation: "Serializes zero/false/empty scalar values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "booleanMember": false,
                "byteMember": 0,
                "shortMember": 0,
                "integerMember": 0,
                "longMember": 0,
                "floatMember": 0,
                "doubleMember": 0,
                "stringMember": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleanMember: false
            byteMember: 0
            shortMember: 0
            integerMember: 0
            longMember: 0
            floatMember: 0
            doubleMember: 0
            stringMember: ""
        }
    }
    {
        id: "RpcV2JsonScalarMembersNaN"
        tags: ["non-finite-floats"]
        documentation: "Serializes NaN float values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "floatMember": "NaN",
                "doubleMember": "NaN"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { floatMember: "NaN", doubleMember: "NaN" }
    }
    {
        id: "RpcV2JsonScalarMembersInfinity"
        tags: ["non-finite-floats"]
        documentation: "Serializes Infinity float values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "floatMember": "Infinity",
                "doubleMember": "Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { floatMember: "Infinity", doubleMember: "Infinity" }
    }
    {
        id: "RpcV2JsonScalarMembersNegativeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Serializes -Infinity float values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "floatMember": "-Infinity",
                "doubleMember": "-Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { floatMember: "-Infinity", doubleMember: "-Infinity" }
    }
    {
        id: "RpcV2JsonScalarMembersOmitsNullValues"
        tags: ["null-on-wire"]
        documentation: "Non-sparse struct members that are null are omitted"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "stringMember": "only this"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { stringMember: "only this" }
    }
])

apply ScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonScalarMembersDeserialize"
        documentation: "Deserializes all scalar members"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleanMember": true,
                "byteMember": 5,
                "shortMember": 256,
                "integerMember": 1234,
                "longMember": 999999999999,
                "floatMember": 1.5,
                "doubleMember": 2.5,
                "stringMember": "hello",
                "blobMember": "Zm9v",
                "dateTimeMember": 1609504496,
                "epochSecondsMember": 1612325106,
                "httpDateMember": 1614834367,
                "stringEnum": "Foo",
                "intEnum": 1
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleanMember: true
            byteMember: 5
            shortMember: 256
            integerMember: 1234
            longMember: 999999999999
            floatMember: 1.5
            doubleMember: 2.5
            stringMember: "hello"
            blobMember: "foo"
            dateTimeMember: 1609504496
            epochSecondsMember: 1612325106
            httpDateMember: 1614834367
            stringEnum: "Foo"
            intEnum: 1
        }
    }
    {
        id: "RpcV2JsonScalarMembersDeserializeNaN"
        tags: ["non-finite-floats"]
        documentation: "Deserializes NaN float values"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "floatMember": "NaN",
                "doubleMember": "NaN"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { floatMember: "NaN", doubleMember: "NaN" }
    }
    {
        id: "RpcV2JsonScalarMembersDeserializeZeroValues"
        documentation: "Deserializes zero/false/empty scalar values"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleanMember": false,
                "byteMember": 0,
                "shortMember": 0,
                "integerMember": 0,
                "longMember": 0,
                "floatMember": 0,
                "doubleMember": 0,
                "stringMember": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleanMember: false
            byteMember: 0
            shortMember: 0
            integerMember: 0
            longMember: 0
            floatMember: 0
            doubleMember: 0
            stringMember: ""
        }
    }
    {
        id: "RpcV2JsonScalarMembersDeserializeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Deserializes Infinity float values"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "floatMember": "Infinity",
                "doubleMember": "Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { floatMember: "Infinity", doubleMember: "Infinity" }
    }
    {
        id: "RpcV2JsonScalarMembersDeserializeNegativeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Deserializes -Infinity float values"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "floatMember": "-Infinity",
                "doubleMember": "-Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { floatMember: "-Infinity", doubleMember: "-Infinity" }
    }
    {
        id: "RpcV2JsonScalarMembersDeserializeIgnoresNullValues"
        tags: ["null-on-wire"]
        documentation: "Client drops a wire null for a dense struct member"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "stringMember": "only this",
                "integerMember": null,
                "booleanMember": null
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: { stringMember: "only this" }
    }
    {
        id: "RpcV2JsonScalarMembersDeserializeIgnoresUnknownFields"
        tags: ["unknown-fields"]
        documentation: "Client ignores unrecognized fields in the response"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "stringMember": "hello",
                "unknownField": "ignored",
                "anotherUnknown": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: { stringMember: "hello" }
    }
])

apply ListOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonListOfScalarsPopulated"
        documentation: "Serializes all scalar list types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfScalars"
        body: """
            {
                "booleans": [true, false],
                "bytes": [5, 6],
                "shorts": [256, 257],
                "integers": [1, 2, 3],
                "longs": [999999999999, 999999999998],
                "floats": [1.5, 2.5],
                "doubles": [3.5, 4.5],
                "strings": ["foo", "bar"],
                "blobs": ["Zm9v", "YmFy"],
                "timestamps": [1609459200, 1609545600],
                "enums": ["Foo", "Bar"],
                "intEnums": [1, 2]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: [true, false]
            bytes: [5, 6]
            shorts: [256, 257]
            integers: [1, 2, 3]
            longs: [999999999999, 999999999998]
            floats: [1.5, 2.5]
            doubles: [3.5, 4.5]
            strings: ["foo", "bar"]
            blobs: ["foo", "bar"]
            timestamps: [1609459200, 1609545600]
            enums: ["Foo", "Bar"]
            intEnums: [1, 2]
        }
    }
    {
        id: "RpcV2JsonListOfScalarsEmpty"
        tags: ["empty"]
        documentation: "Serializes empty lists"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfScalars"
        body: """
            {
                "booleans": [],
                "bytes": [],
                "shorts": [],
                "integers": [],
                "longs": [],
                "floats": [],
                "doubles": [],
                "strings": [],
                "blobs": [],
                "timestamps": [],
                "enums": [],
                "intEnums": []
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: []
            bytes: []
            shorts: []
            integers: []
            longs: []
            floats: []
            doubles: []
            strings: []
            blobs: []
            timestamps: []
            enums: []
            intEnums: []
        }
    }
])

apply ListOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonListOfScalarsPopulatedResponse"
        documentation: "Deserializes all scalar list types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [true, false],
                "bytes": [5, 6],
                "shorts": [256, 257],
                "integers": [1, 2, 3],
                "longs": [999999999999, 999999999998],
                "floats": [1.5, 2.5],
                "doubles": [3.5, 4.5],
                "strings": ["foo", "bar"],
                "blobs": ["Zm9v", "YmFy"],
                "timestamps": [1609459200, 1609545600],
                "enums": ["Foo", "Bar"],
                "intEnums": [1, 2]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: [true, false]
            bytes: [5, 6]
            shorts: [256, 257]
            integers: [1, 2, 3]
            longs: [999999999999, 999999999998]
            floats: [1.5, 2.5]
            doubles: [3.5, 4.5]
            strings: ["foo", "bar"]
            blobs: ["foo", "bar"]
            timestamps: [1609459200, 1609545600]
            enums: ["Foo", "Bar"]
            intEnums: [1, 2]
        }
    }
    {
        id: "RpcV2JsonListOfScalarsEmptyResponse"
        tags: ["empty"]
        documentation: "Deserializes empty lists"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [],
                "bytes": [],
                "shorts": [],
                "integers": [],
                "longs": [],
                "floats": [],
                "doubles": [],
                "strings": [],
                "blobs": [],
                "timestamps": [],
                "enums": [],
                "intEnums": []
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: []
            bytes: []
            shorts: []
            integers: []
            longs: []
            floats: []
            doubles: []
            strings: []
            blobs: []
            timestamps: []
            enums: []
            intEnums: []
        }
    }
])

apply SparseListOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonSparseListOfScalarsNullAtStart"
        documentation: "Serializes sparse lists with a null as the first element"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseListOfScalars"
        body: """
            {
                "booleans": [null, true, false],
                "bytes": [null, 5, 6],
                "shorts": [null, 256, 257],
                "integers": [null, 1, 2],
                "longs": [null, 999999999999, 999999999998],
                "floats": [null, 1.5, 2.5],
                "doubles": [null, 3.5, 4.5],
                "strings": [null, "foo", "bar"],
                "blobs": [null, "Zm9v", "YmFy"],
                "timestamps": [null, 1609459200, 1609545600],
                "enums": [null, "Foo", "Bar"],
                "intEnums": [null, 1, 2]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: [null, true, false]
            bytes: [null, 5, 6]
            shorts: [null, 256, 257]
            integers: [null, 1, 2]
            longs: [null, 999999999999, 999999999998]
            floats: [null, 1.5, 2.5]
            doubles: [null, 3.5, 4.5]
            strings: [null, "foo", "bar"]
            blobs: [null, "foo", "bar"]
            timestamps: [null, 1609459200, 1609545600]
            enums: [null, "Foo", "Bar"]
            intEnums: [null, 1, 2]
        }
    }
    {
        id: "RpcV2JsonSparseListOfScalarsNullInMiddle"
        documentation: "Serializes sparse lists with a null as the middle element"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseListOfScalars"
        body: """
            {
                "booleans": [true, null, false],
                "bytes": [5, null, 6],
                "shorts": [256, null, 257],
                "integers": [1, null, 2],
                "longs": [999999999999, null, 999999999998],
                "floats": [1.5, null, 2.5],
                "doubles": [3.5, null, 4.5],
                "strings": ["foo", null, "bar"],
                "blobs": ["Zm9v", null, "YmFy"],
                "timestamps": [1609459200, null, 1609545600],
                "enums": ["Foo", null, "Bar"],
                "intEnums": [1, null, 2]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: [true, null, false]
            bytes: [5, null, 6]
            shorts: [256, null, 257]
            integers: [1, null, 2]
            longs: [999999999999, null, 999999999998]
            floats: [1.5, null, 2.5]
            doubles: [3.5, null, 4.5]
            strings: ["foo", null, "bar"]
            blobs: ["foo", null, "bar"]
            timestamps: [1609459200, null, 1609545600]
            enums: ["Foo", null, "Bar"]
            intEnums: [1, null, 2]
        }
    }
    {
        id: "RpcV2JsonSparseListOfScalarsNullAtEnd"
        documentation: "Serializes sparse lists with a null as the last element"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseListOfScalars"
        body: """
            {
                "booleans": [true, false, null],
                "bytes": [5, 6, null],
                "shorts": [256, 257, null],
                "integers": [1, 2, null],
                "longs": [999999999999, 999999999998, null],
                "floats": [1.5, 2.5, null],
                "doubles": [3.5, 4.5, null],
                "strings": ["foo", "bar", null],
                "blobs": ["Zm9v", "YmFy", null],
                "timestamps": [1609459200, 1609545600, null],
                "enums": ["Foo", "Bar", null],
                "intEnums": [1, 2, null]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: [true, false, null]
            bytes: [5, 6, null]
            shorts: [256, 257, null]
            integers: [1, 2, null]
            longs: [999999999999, 999999999998, null]
            floats: [1.5, 2.5, null]
            doubles: [3.5, 4.5, null]
            strings: ["foo", "bar", null]
            blobs: ["foo", "bar", null]
            timestamps: [1609459200, 1609545600, null]
            enums: ["Foo", "Bar", null]
            intEnums: [1, 2, null]
        }
    }
])

apply SparseListOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonSparseListOfScalarsNullAtStartResponse"
        documentation: "Deserializes sparse lists with a null as the first element"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [null, true, false],
                "bytes": [null, 5, 6],
                "shorts": [null, 256, 257],
                "integers": [null, 1, 2],
                "longs": [null, 999999999999, 999999999998],
                "floats": [null, 1.5, 2.5],
                "doubles": [null, 3.5, 4.5],
                "strings": [null, "foo", "bar"],
                "blobs": [null, "Zm9v", "YmFy"],
                "timestamps": [null, 1609459200, 1609545600],
                "enums": [null, "Foo", "Bar"],
                "intEnums": [null, 1, 2]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: [null, true, false]
            bytes: [null, 5, 6]
            shorts: [null, 256, 257]
            integers: [null, 1, 2]
            longs: [null, 999999999999, 999999999998]
            floats: [null, 1.5, 2.5]
            doubles: [null, 3.5, 4.5]
            strings: [null, "foo", "bar"]
            blobs: [null, "foo", "bar"]
            timestamps: [null, 1609459200, 1609545600]
            enums: [null, "Foo", "Bar"]
            intEnums: [null, 1, 2]
        }
    }
    {
        id: "RpcV2JsonSparseListOfScalarsNullInMiddleResponse"
        documentation: "Deserializes sparse lists with a null as the middle element"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [true, null, false],
                "bytes": [5, null, 6],
                "shorts": [256, null, 257],
                "integers": [1, null, 2],
                "longs": [999999999999, null, 999999999998],
                "floats": [1.5, null, 2.5],
                "doubles": [3.5, null, 4.5],
                "strings": ["foo", null, "bar"],
                "blobs": ["Zm9v", null, "YmFy"],
                "timestamps": [1609459200, null, 1609545600],
                "enums": ["Foo", null, "Bar"],
                "intEnums": [1, null, 2]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: [true, null, false]
            bytes: [5, null, 6]
            shorts: [256, null, 257]
            integers: [1, null, 2]
            longs: [999999999999, null, 999999999998]
            floats: [1.5, null, 2.5]
            doubles: [3.5, null, 4.5]
            strings: ["foo", null, "bar"]
            blobs: ["foo", null, "bar"]
            timestamps: [1609459200, null, 1609545600]
            enums: ["Foo", null, "Bar"]
            intEnums: [1, null, 2]
        }
    }
    {
        id: "RpcV2JsonSparseListOfScalarsNullAtEndResponse"
        documentation: "Deserializes sparse lists with a null as the last element"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [true, false, null],
                "bytes": [5, 6, null],
                "shorts": [256, 257, null],
                "integers": [1, 2, null],
                "longs": [999999999999, 999999999998, null],
                "floats": [1.5, 2.5, null],
                "doubles": [3.5, 4.5, null],
                "strings": ["foo", "bar", null],
                "blobs": ["Zm9v", "YmFy", null],
                "timestamps": [1609459200, 1609545600, null],
                "enums": ["Foo", "Bar", null],
                "intEnums": [1, 2, null]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: [true, false, null]
            bytes: [5, 6, null]
            shorts: [256, 257, null]
            integers: [1, 2, null]
            longs: [999999999999, 999999999998, null]
            floats: [1.5, 2.5, null]
            doubles: [3.5, 4.5, null]
            strings: ["foo", "bar", null]
            blobs: ["foo", "bar", null]
            timestamps: [1609459200, 1609545600, null]
            enums: ["Foo", "Bar", null]
            intEnums: [1, 2, null]
        }
    }
])

apply MapOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonMapOfScalarsPopulated"
        documentation: "Serializes all scalar map types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfScalars"
        body: """
            {
                "booleans": {"a": true, "b": false},
                "bytes": {"a": 5, "b": 6},
                "shorts": {"a": 256, "b": 257},
                "integers": {"a": 1, "b": 2},
                "longs": {"a": 999999999999, "b": 999999999998},
                "floats": {"a": 1.5, "b": 2.5},
                "doubles": {"a": 3.5, "b": 4.5},
                "strings": {"a": "foo", "b": "bar"},
                "blobs": {"a": "Zm9v", "b": "YmFy"},
                "timestamps": {"a": 1609459200, "b": 1609545600},
                "enums": {"a": "Foo", "b": "Bar"},
                "intEnums": {"a": 1, "b": 2}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: { a: true, b: false }
            bytes: { a: 5, b: 6 }
            shorts: { a: 256, b: 257 }
            integers: { a: 1, b: 2 }
            longs: { a: 999999999999, b: 999999999998 }
            floats: { a: 1.5, b: 2.5 }
            doubles: { a: 3.5, b: 4.5 }
            strings: { a: "foo", b: "bar" }
            blobs: { a: "foo", b: "bar" }
            timestamps: { a: 1609459200, b: 1609545600 }
            enums: { a: "Foo", b: "Bar" }
            intEnums: { a: 1, b: 2 }
        }
    }
    {
        id: "RpcV2JsonMapOfScalarsEmpty"
        tags: ["empty"]
        documentation: "Serializes empty maps"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfScalars"
        body: """
            {
                "booleans": {},
                "bytes": {},
                "shorts": {},
                "integers": {},
                "longs": {},
                "floats": {},
                "doubles": {},
                "strings": {},
                "blobs": {},
                "timestamps": {},
                "enums": {},
                "intEnums": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: {}
            bytes: {}
            shorts: {}
            integers: {}
            longs: {}
            floats: {}
            doubles: {}
            strings: {}
            blobs: {}
            timestamps: {}
            enums: {}
            intEnums: {}
        }
    }
])

apply MapOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonMapOfScalarsPopulatedResponse"
        documentation: "Deserializes all scalar map types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {"a": true, "b": false},
                "bytes": {"a": 5, "b": 6},
                "shorts": {"a": 256, "b": 257},
                "integers": {"a": 1, "b": 2},
                "longs": {"a": 999999999999, "b": 999999999998},
                "floats": {"a": 1.5, "b": 2.5},
                "doubles": {"a": 3.5, "b": 4.5},
                "strings": {"a": "foo", "b": "bar"},
                "blobs": {"a": "Zm9v", "b": "YmFy"},
                "timestamps": {"a": 1609459200, "b": 1609545600},
                "enums": {"a": "Foo", "b": "Bar"},
                "intEnums": {"a": 1, "b": 2}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: { a: true, b: false }
            bytes: { a: 5, b: 6 }
            shorts: { a: 256, b: 257 }
            integers: { a: 1, b: 2 }
            longs: { a: 999999999999, b: 999999999998 }
            floats: { a: 1.5, b: 2.5 }
            doubles: { a: 3.5, b: 4.5 }
            strings: { a: "foo", b: "bar" }
            blobs: { a: "foo", b: "bar" }
            timestamps: { a: 1609459200, b: 1609545600 }
            enums: { a: "Foo", b: "Bar" }
            intEnums: { a: 1, b: 2 }
        }
    }
    {
        id: "RpcV2JsonMapOfScalarsEmptyResponse"
        tags: ["empty"]
        documentation: "Deserializes empty maps"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {},
                "bytes": {},
                "shorts": {},
                "integers": {},
                "longs": {},
                "floats": {},
                "doubles": {},
                "strings": {},
                "blobs": {},
                "timestamps": {},
                "enums": {},
                "intEnums": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: {}
            bytes: {}
            shorts: {}
            integers: {}
            longs: {}
            floats: {}
            doubles: {}
            strings: {}
            blobs: {}
            timestamps: {}
            enums: {}
            intEnums: {}
        }
    }
])

apply SparseMapOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonSparseMapOfScalarsNullAtStart"
        documentation: "Serializes sparse maps with a null as the first entry"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseMapOfScalars"
        body: """
            {
                "booleans": {"a": null, "b": true, "c": false},
                "bytes": {"a": null, "b": 5, "c": 6},
                "shorts": {"a": null, "b": 256, "c": 257},
                "integers": {"a": null, "b": 1, "c": 2},
                "longs": {"a": null, "b": 999999999999, "c": 999999999998},
                "floats": {"a": null, "b": 1.5, "c": 2.5},
                "doubles": {"a": null, "b": 3.5, "c": 4.5},
                "strings": {"a": null, "b": "foo", "c": "bar"},
                "blobs": {"a": null, "b": "Zm9v", "c": "YmFy"},
                "timestamps": {"a": null, "b": 1609459200, "c": 1609545600},
                "enums": {"a": null, "b": "Foo", "c": "Bar"},
                "intEnums": {"a": null, "b": 1, "c": 2}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: { a: null, b: true, c: false }
            bytes: { a: null, b: 5, c: 6 }
            shorts: { a: null, b: 256, c: 257 }
            integers: { a: null, b: 1, c: 2 }
            longs: { a: null, b: 999999999999, c: 999999999998 }
            floats: { a: null, b: 1.5, c: 2.5 }
            doubles: { a: null, b: 3.5, c: 4.5 }
            strings: { a: null, b: "foo", c: "bar" }
            blobs: { a: null, b: "foo", c: "bar" }
            timestamps: { a: null, b: 1609459200, c: 1609545600 }
            enums: { a: null, b: "Foo", c: "Bar" }
            intEnums: { a: null, b: 1, c: 2 }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfScalarsNullInMiddle"
        documentation: "Serializes sparse maps with a null as the middle entry"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseMapOfScalars"
        body: """
            {
                "booleans": {"a": true, "b": null, "c": false},
                "bytes": {"a": 5, "b": null, "c": 6},
                "shorts": {"a": 256, "b": null, "c": 257},
                "integers": {"a": 1, "b": null, "c": 2},
                "longs": {"a": 999999999999, "b": null, "c": 999999999998},
                "floats": {"a": 1.5, "b": null, "c": 2.5},
                "doubles": {"a": 3.5, "b": null, "c": 4.5},
                "strings": {"a": "foo", "b": null, "c": "bar"},
                "blobs": {"a": "Zm9v", "b": null, "c": "YmFy"},
                "timestamps": {"a": 1609459200, "b": null, "c": 1609545600},
                "enums": {"a": "Foo", "b": null, "c": "Bar"},
                "intEnums": {"a": 1, "b": null, "c": 2}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: { a: true, b: null, c: false }
            bytes: { a: 5, b: null, c: 6 }
            shorts: { a: 256, b: null, c: 257 }
            integers: { a: 1, b: null, c: 2 }
            longs: { a: 999999999999, b: null, c: 999999999998 }
            floats: { a: 1.5, b: null, c: 2.5 }
            doubles: { a: 3.5, b: null, c: 4.5 }
            strings: { a: "foo", b: null, c: "bar" }
            blobs: { a: "foo", b: null, c: "bar" }
            timestamps: { a: 1609459200, b: null, c: 1609545600 }
            enums: { a: "Foo", b: null, c: "Bar" }
            intEnums: { a: 1, b: null, c: 2 }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfScalarsNullAtEnd"
        documentation: "Serializes sparse maps with a null as the last entry"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseMapOfScalars"
        body: """
            {
                "booleans": {"a": true, "b": false, "c": null},
                "bytes": {"a": 5, "b": 6, "c": null},
                "shorts": {"a": 256, "b": 257, "c": null},
                "integers": {"a": 1, "b": 2, "c": null},
                "longs": {"a": 999999999999, "b": 999999999998, "c": null},
                "floats": {"a": 1.5, "b": 2.5, "c": null},
                "doubles": {"a": 3.5, "b": 4.5, "c": null},
                "strings": {"a": "foo", "b": "bar", "c": null},
                "blobs": {"a": "Zm9v", "b": "YmFy", "c": null},
                "timestamps": {"a": 1609459200, "b": 1609545600, "c": null},
                "enums": {"a": "Foo", "b": "Bar", "c": null},
                "intEnums": {"a": 1, "b": 2, "c": null}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: { a: true, b: false, c: null }
            bytes: { a: 5, b: 6, c: null }
            shorts: { a: 256, b: 257, c: null }
            integers: { a: 1, b: 2, c: null }
            longs: { a: 999999999999, b: 999999999998, c: null }
            floats: { a: 1.5, b: 2.5, c: null }
            doubles: { a: 3.5, b: 4.5, c: null }
            strings: { a: "foo", b: "bar", c: null }
            blobs: { a: "foo", b: "bar", c: null }
            timestamps: { a: 1609459200, b: 1609545600, c: null }
            enums: { a: "Foo", b: "Bar", c: null }
            intEnums: { a: 1, b: 2, c: null }
        }
    }
])

apply SparseMapOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonSparseMapOfScalarsNullAtStartResponse"
        documentation: "Deserializes sparse maps with a null as the first entry"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {"a": null, "b": true, "c": false},
                "bytes": {"a": null, "b": 5, "c": 6},
                "shorts": {"a": null, "b": 256, "c": 257},
                "integers": {"a": null, "b": 1, "c": 2},
                "longs": {"a": null, "b": 999999999999, "c": 999999999998},
                "floats": {"a": null, "b": 1.5, "c": 2.5},
                "doubles": {"a": null, "b": 3.5, "c": 4.5},
                "strings": {"a": null, "b": "foo", "c": "bar"},
                "blobs": {"a": null, "b": "Zm9v", "c": "YmFy"},
                "timestamps": {"a": null, "b": 1609459200, "c": 1609545600},
                "enums": {"a": null, "b": "Foo", "c": "Bar"},
                "intEnums": {"a": null, "b": 1, "c": 2}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: { a: null, b: true, c: false }
            bytes: { a: null, b: 5, c: 6 }
            shorts: { a: null, b: 256, c: 257 }
            integers: { a: null, b: 1, c: 2 }
            longs: { a: null, b: 999999999999, c: 999999999998 }
            floats: { a: null, b: 1.5, c: 2.5 }
            doubles: { a: null, b: 3.5, c: 4.5 }
            strings: { a: null, b: "foo", c: "bar" }
            blobs: { a: null, b: "foo", c: "bar" }
            timestamps: { a: null, b: 1609459200, c: 1609545600 }
            enums: { a: null, b: "Foo", c: "Bar" }
            intEnums: { a: null, b: 1, c: 2 }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfScalarsNullInMiddleResponse"
        documentation: "Deserializes sparse maps with a null as the middle entry"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {"a": true, "b": null, "c": false},
                "bytes": {"a": 5, "b": null, "c": 6},
                "shorts": {"a": 256, "b": null, "c": 257},
                "integers": {"a": 1, "b": null, "c": 2},
                "longs": {"a": 999999999999, "b": null, "c": 999999999998},
                "floats": {"a": 1.5, "b": null, "c": 2.5},
                "doubles": {"a": 3.5, "b": null, "c": 4.5},
                "strings": {"a": "foo", "b": null, "c": "bar"},
                "blobs": {"a": "Zm9v", "b": null, "c": "YmFy"},
                "timestamps": {"a": 1609459200, "b": null, "c": 1609545600},
                "enums": {"a": "Foo", "b": null, "c": "Bar"},
                "intEnums": {"a": 1, "b": null, "c": 2}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: { a: true, b: null, c: false }
            bytes: { a: 5, b: null, c: 6 }
            shorts: { a: 256, b: null, c: 257 }
            integers: { a: 1, b: null, c: 2 }
            longs: { a: 999999999999, b: null, c: 999999999998 }
            floats: { a: 1.5, b: null, c: 2.5 }
            doubles: { a: 3.5, b: null, c: 4.5 }
            strings: { a: "foo", b: null, c: "bar" }
            blobs: { a: "foo", b: null, c: "bar" }
            timestamps: { a: 1609459200, b: null, c: 1609545600 }
            enums: { a: "Foo", b: null, c: "Bar" }
            intEnums: { a: 1, b: null, c: 2 }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfScalarsNullAtEndResponse"
        documentation: "Deserializes sparse maps with a null as the last entry"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {"a": true, "b": false, "c": null},
                "bytes": {"a": 5, "b": 6, "c": null},
                "shorts": {"a": 256, "b": 257, "c": null},
                "integers": {"a": 1, "b": 2, "c": null},
                "longs": {"a": 999999999999, "b": 999999999998, "c": null},
                "floats": {"a": 1.5, "b": 2.5, "c": null},
                "doubles": {"a": 3.5, "b": 4.5, "c": null},
                "strings": {"a": "foo", "b": "bar", "c": null},
                "blobs": {"a": "Zm9v", "b": "YmFy", "c": null},
                "timestamps": {"a": 1609459200, "b": 1609545600, "c": null},
                "enums": {"a": "Foo", "b": "Bar", "c": null},
                "intEnums": {"a": 1, "b": 2, "c": null}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: { a: true, b: false, c: null }
            bytes: { a: 5, b: 6, c: null }
            shorts: { a: 256, b: 257, c: null }
            integers: { a: 1, b: 2, c: null }
            longs: { a: 999999999999, b: 999999999998, c: null }
            floats: { a: 1.5, b: 2.5, c: null }
            doubles: { a: 3.5, b: 4.5, c: null }
            strings: { a: "foo", b: "bar", c: null }
            blobs: { a: "foo", b: "bar", c: null }
            timestamps: { a: 1609459200, b: 1609545600, c: null }
            enums: { a: "Foo", b: "Bar", c: null }
            intEnums: { a: 1, b: 2, c: null }
        }
    }
])

apply StructOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonStructOfScalarsPopulated"
        documentation: "Serializes a fully populated nested struct"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/StructOfScalars"
        body: """
            {
                "value": {
                    "booleanMember": true,
                    "byteMember": 5,
                    "shortMember": 256,
                    "integerMember": 1234,
                    "longMember": 999999999999,
                    "floatMember": 1.5,
                    "doubleMember": 2.5,
                    "stringMember": "hello",
                    "blobMember": "Zm9v",
                    "dateTimeMember": 1609504496,
                    "epochSecondsMember": 1612325106,
                    "httpDateMember": 1614834367,
                    "stringEnum": "Foo",
                    "intEnum": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                booleanMember: true
                byteMember: 5
                shortMember: 256
                integerMember: 1234
                longMember: 999999999999
                floatMember: 1.5
                doubleMember: 2.5
                stringMember: "hello"
                blobMember: "foo"
                dateTimeMember: 1609504496
                epochSecondsMember: 1612325106
                httpDateMember: 1614834367
                stringEnum: "Foo"
                intEnum: 1
            }
        }
    }
])

apply StructOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonStructOfScalarsPopulatedResponse"
        documentation: "Deserializes a fully populated nested struct"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "booleanMember": true,
                    "byteMember": 5,
                    "shortMember": 256,
                    "integerMember": 1234,
                    "longMember": 999999999999,
                    "floatMember": 1.5,
                    "doubleMember": 2.5,
                    "stringMember": "hello",
                    "blobMember": "Zm9v",
                    "dateTimeMember": 1609504496,
                    "epochSecondsMember": 1612325106,
                    "httpDateMember": 1614834367,
                    "stringEnum": "Foo",
                    "intEnum": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                booleanMember: true
                byteMember: 5
                shortMember: 256
                integerMember: 1234
                longMember: 999999999999
                floatMember: 1.5
                doubleMember: 2.5
                stringMember: "hello"
                blobMember: "foo"
                dateTimeMember: 1609504496
                epochSecondsMember: 1612325106
                httpDateMember: 1614834367
                stringEnum: "Foo"
                intEnum: 1
            }
        }
    }
])

apply UnionOfScalars @httpRequestTests([
    {
        id: "RpcV2JsonUnionOfScalarsStringSerialize"
        documentation: "Serializes union string variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "stringValue": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { stringValue: "hello" }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsBooleanSerialize"
        documentation: "Serializes union boolean variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "booleanValue": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { booleanValue: true }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsByteSerialize"
        documentation: "Serializes union byte variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "byteValue": 9
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { byteValue: 9 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsShortSerialize"
        documentation: "Serializes union short variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "shortValue": 512
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { shortValue: 512 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsIntegerSerialize"
        documentation: "Serializes union integer variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "integerValue": 42
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { integerValue: 42 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsLongSerialize"
        documentation: "Serializes union long variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "longValue": 999999999999
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { longValue: 999999999999 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsFloatSerialize"
        documentation: "Serializes union float variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "floatValue": 1.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { floatValue: 1.5 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsDoubleSerialize"
        documentation: "Serializes union double variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "doubleValue": 2.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { doubleValue: 2.5 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsBlobSerialize"
        documentation: "Serializes union blob variant (base64 on wire)"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "blobValue": "Zm9v"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { blobValue: "foo" }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsTimestampSerialize"
        documentation: "Serializes union timestamp variant (epoch seconds on wire)"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "timestampValue": 1609459200
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { timestampValue: 1609459200 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsEnumSerialize"
        documentation: "Serializes union enum variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "enumValue": "Foo"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { enumValue: "Foo" }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsIntEnumSerialize"
        documentation: "Serializes union intEnum variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfScalars"
        body: """
            {
                "value": {
                    "intEnumValue": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: { intEnumValue: 1 }
        }
    }
])

apply UnionOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonUnionOfScalarsStringDeserialize"
        documentation: "Deserializes union string variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "stringValue": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { stringValue: "hello" }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsBooleanDeserialize"
        documentation: "Deserializes union boolean variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "booleanValue": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { booleanValue: true }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsByteDeserialize"
        documentation: "Deserializes union byte variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "byteValue": 9
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { byteValue: 9 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsShortDeserialize"
        documentation: "Deserializes union short variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "shortValue": 512
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { shortValue: 512 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsIntegerDeserialize"
        documentation: "Deserializes union integer variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "integerValue": 42
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { integerValue: 42 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsLongDeserialize"
        documentation: "Deserializes union long variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "longValue": 999999999999
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { longValue: 999999999999 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsFloatDeserialize"
        documentation: "Deserializes union float variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "floatValue": 1.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { floatValue: 1.5 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsDoubleDeserialize"
        documentation: "Deserializes union double variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "doubleValue": 2.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { doubleValue: 2.5 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsBlobDeserialize"
        documentation: "Deserializes union blob variant (base64 on wire)"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "blobValue": "Zm9v"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { blobValue: "foo" }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsTimestampDeserialize"
        documentation: "Deserializes union timestamp variant (epoch seconds on wire)"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "timestampValue": 1609459200
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { timestampValue: 1609459200 }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsEnumDeserialize"
        documentation: "Deserializes union enum variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "enumValue": "Foo"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { enumValue: "Foo" }
        }
    }
    {
        id: "RpcV2JsonUnionOfScalarsIntEnumDeserialize"
        documentation: "Deserializes union intEnum variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "intEnumValue": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { intEnumValue: 1 }
        }
    }
])

apply UnionOfStruct @httpRequestTests([
    {
        id: "RpcV2JsonUnionOfStructSerialize"
        documentation: "Serializes union struct variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfStruct"
        body: """
            {
                "value": {
                    "structValue": {
                        "stringMember": "hello",
                        "integerMember": 42,
                        "booleanMember": true
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                structValue: { stringMember: "hello", integerMember: 42, booleanMember: true }
            }
        }
    }
])

apply UnionOfStruct @httpResponseTests([
    {
        id: "RpcV2JsonUnionOfStructDeserialize"
        documentation: "Deserializes union struct variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "structValue": {
                        "stringMember": "hello",
                        "integerMember": 42,
                        "booleanMember": true
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                structValue: { stringMember: "hello", integerMember: 42, booleanMember: true }
            }
        }
    }
])

apply UnionOfList @httpRequestTests([
    {
        id: "RpcV2JsonUnionOfListSerialize"
        documentation: "Serializes union list variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfList"
        body: """
            {
                "value": {
                    "listValue": ["a", "b", "c"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                listValue: ["a", "b", "c"]
            }
        }
    }
])

apply UnionOfList @httpResponseTests([
    {
        id: "RpcV2JsonUnionOfListDeserialize"
        documentation: "Deserializes union list variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "listValue": ["a", "b", "c"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                listValue: ["a", "b", "c"]
            }
        }
    }
])

apply UnionOfMap @httpRequestTests([
    {
        id: "RpcV2JsonUnionOfMapSerialize"
        documentation: "Serializes union map variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfMap"
        body: """
            {
                "value": {
                    "mapValue": {
                        "k1": "v1",
                        "k2": "v2"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                mapValue: { k1: "v1", k2: "v2" }
            }
        }
    }
])

apply UnionOfMap @httpResponseTests([
    {
        id: "RpcV2JsonUnionOfMapDeserialize"
        documentation: "Deserializes union map variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "mapValue": {
                        "k1": "v1",
                        "k2": "v2"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                mapValue: { k1: "v1", k2: "v2" }
            }
        }
    }
])

apply UnionOfUnion @httpRequestTests([
    {
        id: "RpcV2JsonUnionOfUnionSerialize"
        documentation: "Serializes union containing another union"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/UnionOfUnion"
        body: """
            {
                "value": {
                    "unionValue": {
                        "stringValue": "nested"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                unionValue: { stringValue: "nested" }
            }
        }
    }
])

apply UnionOfUnion @httpResponseTests([
    {
        id: "RpcV2JsonUnionOfUnionDeserialize"
        documentation: "Deserializes union containing another union"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "unionValue": {
                        "stringValue": "nested"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                unionValue: { stringValue: "nested" }
            }
        }
    }
])

apply ListOfStructs @httpRequestTests([
    {
        id: "RpcV2JsonListOfStructsSerialize"
        documentation: "Serializes a list of structs"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfStructs"
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: [
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
            ]
        }
    }
])

apply ListOfStructs @httpResponseTests([
    {
        id: "RpcV2JsonListOfStructsDeserialize"
        documentation: "Deserializes a list of structs"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: [
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
            ]
        }
    }
])

apply ListOfMaps @httpRequestTests([
    {
        id: "RpcV2JsonListOfMapsSerialize"
        documentation: "Serializes lists of maps for all scalar leaf types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfMaps"
        body: """
            {
                "booleans": [{"k1": true, "k2": false}, {"k3": false, "k4": true, "k5": true}],
                "integers": [{"k1": 1, "k2": 2}, {"k3": 3, "k4": 4, "k5": 5}],
                "strings": [{"k1": "a", "k2": "b"}, {"k3": "c", "k4": "d", "k5": "e"}],
                "blobs": [{"k1": "Zm9v", "k2": "YmFy"}, {"k3": "YmF6", "k4": "cXV4", "k5": "cXV1eA=="}],
                "timestamps": [{"k1": 1609502096, "k2": 1609588496}, {"k3": 1609674896, "k4": 1609761296, "k5": 1609847696}],
                "enums": [{"k1": "Foo", "k2": "Bar"}, {"k3": "Baz", "k4": "Foo", "k5": "Bar"}],
                "intEnums": [{"k1": 1, "k2": 2}, {"k3": 3, "k4": 1, "k5": 2}]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: [
                {
                    k1: true
                    k2: false
                }
                {
                    k3: false
                    k4: true
                    k5: true
                }
            ]
            integers: [
                {
                    k1: 1
                    k2: 2
                }
                {
                    k3: 3
                    k4: 4
                    k5: 5
                }
            ]
            strings: [
                {
                    k1: "a"
                    k2: "b"
                }
                {
                    k3: "c"
                    k4: "d"
                    k5: "e"
                }
            ]
            blobs: [
                {
                    k1: "foo"
                    k2: "bar"
                }
                {
                    k3: "baz"
                    k4: "qux"
                    k5: "quux"
                }
            ]
            timestamps: [
                {
                    k1: 1609502096
                    k2: 1609588496
                }
                {
                    k3: 1609674896
                    k4: 1609761296
                    k5: 1609847696
                }
            ]
            enums: [
                {
                    k1: "Foo"
                    k2: "Bar"
                }
                {
                    k3: "Baz"
                    k4: "Foo"
                    k5: "Bar"
                }
            ]
            intEnums: [
                {
                    k1: 1
                    k2: 2
                }
                {
                    k3: 3
                    k4: 1
                    k5: 2
                }
            ]
        }
    }
])

apply ListOfMaps @httpResponseTests([
    {
        id: "RpcV2JsonListOfMapsDeserialize"
        documentation: "Deserializes lists of maps for all scalar leaf types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [{"k1": true, "k2": false}, {"k3": false, "k4": true, "k5": true}],
                "integers": [{"k1": 1, "k2": 2}, {"k3": 3, "k4": 4, "k5": 5}],
                "strings": [{"k1": "a", "k2": "b"}, {"k3": "c", "k4": "d", "k5": "e"}],
                "blobs": [{"k1": "Zm9v", "k2": "YmFy"}, {"k3": "YmF6", "k4": "cXV4", "k5": "cXV1eA=="}],
                "timestamps": [{"k1": 1609502096, "k2": 1609588496}, {"k3": 1609674896, "k4": 1609761296, "k5": 1609847696}],
                "enums": [{"k1": "Foo", "k2": "Bar"}, {"k3": "Baz", "k4": "Foo", "k5": "Bar"}],
                "intEnums": [{"k1": 1, "k2": 2}, {"k3": 3, "k4": 1, "k5": 2}]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: [
                {
                    k1: true
                    k2: false
                }
                {
                    k3: false
                    k4: true
                    k5: true
                }
            ]
            integers: [
                {
                    k1: 1
                    k2: 2
                }
                {
                    k3: 3
                    k4: 4
                    k5: 5
                }
            ]
            strings: [
                {
                    k1: "a"
                    k2: "b"
                }
                {
                    k3: "c"
                    k4: "d"
                    k5: "e"
                }
            ]
            blobs: [
                {
                    k1: "foo"
                    k2: "bar"
                }
                {
                    k3: "baz"
                    k4: "qux"
                    k5: "quux"
                }
            ]
            timestamps: [
                {
                    k1: 1609502096
                    k2: 1609588496
                }
                {
                    k3: 1609674896
                    k4: 1609761296
                    k5: 1609847696
                }
            ]
            enums: [
                {
                    k1: "Foo"
                    k2: "Bar"
                }
                {
                    k3: "Baz"
                    k4: "Foo"
                    k5: "Bar"
                }
            ]
            intEnums: [
                {
                    k1: 1
                    k2: 2
                }
                {
                    k3: 3
                    k4: 1
                    k5: 2
                }
            ]
        }
    }
])

apply ListOfLists @httpRequestTests([
    {
        id: "RpcV2JsonListOfListsSerialize"
        documentation: "Serializes lists of lists for all scalar leaf types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfLists"
        body: """
            {
                "booleans": [[true, false], [false, true, true]],
                "integers": [[1, 2], [3, 4, 5]],
                "strings": [["a", "b"], ["c", "d", "e"]],
                "blobs": [["Zm9v", "YmFy"], ["YmF6", "cXV4", "cXV1eA=="]],
                "timestamps": [[1609502096, 1609588496], [1609674896, 1609761296, 1609847696]],
                "enums": [["Foo", "Bar"], ["Baz", "Foo", "Bar"]],
                "intEnums": [[1, 2], [3, 1, 2]]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: [
                [true, false]
                [false, true, true]
            ]
            integers: [
                [1, 2]
                [3, 4, 5]
            ]
            strings: [
                ["a", "b"]
                ["c", "d", "e"]
            ]
            blobs: [
                ["foo", "bar"]
                ["baz", "qux", "quux"]
            ]
            timestamps: [
                [1609502096, 1609588496]
                [1609674896, 1609761296, 1609847696]
            ]
            enums: [
                ["Foo", "Bar"]
                ["Baz", "Foo", "Bar"]
            ]
            intEnums: [
                [1, 2]
                [3, 1, 2]
            ]
        }
    }
])

apply ListOfLists @httpResponseTests([
    {
        id: "RpcV2JsonListOfListsDeserialize"
        documentation: "Deserializes lists of lists for all scalar leaf types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": [[true, false], [false, true, true]],
                "integers": [[1, 2], [3, 4, 5]],
                "strings": [["a", "b"], ["c", "d", "e"]],
                "blobs": [["Zm9v", "YmFy"], ["YmF6", "cXV4", "cXV1eA=="]],
                "timestamps": [[1609502096, 1609588496], [1609674896, 1609761296, 1609847696]],
                "enums": [["Foo", "Bar"], ["Baz", "Foo", "Bar"]],
                "intEnums": [[1, 2], [3, 1, 2]]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: [
                [true, false]
                [false, true, true]
            ]
            integers: [
                [1, 2]
                [3, 4, 5]
            ]
            strings: [
                ["a", "b"]
                ["c", "d", "e"]
            ]
            blobs: [
                ["foo", "bar"]
                ["baz", "qux", "quux"]
            ]
            timestamps: [
                [1609502096, 1609588496]
                [1609674896, 1609761296, 1609847696]
            ]
            enums: [
                ["Foo", "Bar"]
                ["Baz", "Foo", "Bar"]
            ]
            intEnums: [
                [1, 2]
                [3, 1, 2]
            ]
        }
    }
])

apply ListOfUnions @httpRequestTests([
    {
        id: "RpcV2JsonListOfUnionsSerialize"
        documentation: "Serializes a list of unions with different variants"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfUnions"
        body: """
            {
                "values": [
                    {"stringValue": "hello"},
                    {"integerValue": 42}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: [
                {
                    stringValue: "hello"
                }
                {
                    integerValue: 42
                }
            ]
        }
    }
])

apply ListOfUnions @httpResponseTests([
    {
        id: "RpcV2JsonListOfUnionsDeserialize"
        documentation: "Deserializes a list of unions with different variants"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": [
                    {"stringValue": "hello"},
                    {"integerValue": 42}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: [
                {
                    stringValue: "hello"
                }
                {
                    integerValue: 42
                }
            ]
        }
    }
])

apply MapOfStructs @httpRequestTests([
    {
        id: "RpcV2JsonMapOfStructsSerialize"
        documentation: "Serializes a map of structs"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfStructs"
        body: """
            {
                "values": {
                    "first": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "second": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: {
                first: { stringMember: "foo", integerMember: 1, booleanMember: true }
                second: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply MapOfStructs @httpResponseTests([
    {
        id: "RpcV2JsonMapOfStructsDeserialize"
        documentation: "Deserializes a map of structs"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": {
                    "first": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "second": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: {
                first: { stringMember: "foo", integerMember: 1, booleanMember: true }
                second: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply MapOfMaps @httpRequestTests([
    {
        id: "RpcV2JsonMapOfMapsSerialize"
        documentation: "Serializes maps of maps for all scalar leaf types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfMaps"
        body: """
            {
                "booleans": {"first": {"k1": true, "k2": false}, "second": {"k3": false, "k4": true, "k5": true}},
                "integers": {"first": {"k1": 1, "k2": 2}, "second": {"k3": 3, "k4": 4, "k5": 5}},
                "strings": {"first": {"k1": "a", "k2": "b"}, "second": {"k3": "c", "k4": "d", "k5": "e"}},
                "blobs": {"first": {"k1": "Zm9v", "k2": "YmFy"}, "second": {"k3": "YmF6", "k4": "cXV4", "k5": "cXV1eA=="}},
                "timestamps": {"first": {"k1": 1609502096, "k2": 1609588496}, "second": {"k3": 1609674896, "k4": 1609761296, "k5": 1609847696}},
                "enums": {"first": {"k1": "Foo", "k2": "Bar"}, "second": {"k3": "Baz", "k4": "Foo", "k5": "Bar"}},
                "intEnums": {"first": {"k1": 1, "k2": 2}, "second": {"k3": 3, "k4": 1, "k5": 2}}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: {
                first: { k1: true, k2: false }
                second: { k3: false, k4: true, k5: true }
            }
            integers: {
                first: { k1: 1, k2: 2 }
                second: { k3: 3, k4: 4, k5: 5 }
            }
            strings: {
                first: { k1: "a", k2: "b" }
                second: { k3: "c", k4: "d", k5: "e" }
            }
            blobs: {
                first: { k1: "foo", k2: "bar" }
                second: { k3: "baz", k4: "qux", k5: "quux" }
            }
            timestamps: {
                first: { k1: 1609502096, k2: 1609588496 }
                second: { k3: 1609674896, k4: 1609761296, k5: 1609847696 }
            }
            enums: {
                first: { k1: "Foo", k2: "Bar" }
                second: { k3: "Baz", k4: "Foo", k5: "Bar" }
            }
            intEnums: {
                first: { k1: 1, k2: 2 }
                second: { k3: 3, k4: 1, k5: 2 }
            }
        }
    }
])

apply MapOfMaps @httpResponseTests([
    {
        id: "RpcV2JsonMapOfMapsDeserialize"
        documentation: "Deserializes maps of maps for all scalar leaf types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {"first": {"k1": true, "k2": false}, "second": {"k3": false, "k4": true, "k5": true}},
                "integers": {"first": {"k1": 1, "k2": 2}, "second": {"k3": 3, "k4": 4, "k5": 5}},
                "strings": {"first": {"k1": "a", "k2": "b"}, "second": {"k3": "c", "k4": "d", "k5": "e"}},
                "blobs": {"first": {"k1": "Zm9v", "k2": "YmFy"}, "second": {"k3": "YmF6", "k4": "cXV4", "k5": "cXV1eA=="}},
                "timestamps": {"first": {"k1": 1609502096, "k2": 1609588496}, "second": {"k3": 1609674896, "k4": 1609761296, "k5": 1609847696}},
                "enums": {"first": {"k1": "Foo", "k2": "Bar"}, "second": {"k3": "Baz", "k4": "Foo", "k5": "Bar"}},
                "intEnums": {"first": {"k1": 1, "k2": 2}, "second": {"k3": 3, "k4": 1, "k5": 2}}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: {
                first: { k1: true, k2: false }
                second: { k3: false, k4: true, k5: true }
            }
            integers: {
                first: { k1: 1, k2: 2 }
                second: { k3: 3, k4: 4, k5: 5 }
            }
            strings: {
                first: { k1: "a", k2: "b" }
                second: { k3: "c", k4: "d", k5: "e" }
            }
            blobs: {
                first: { k1: "foo", k2: "bar" }
                second: { k3: "baz", k4: "qux", k5: "quux" }
            }
            timestamps: {
                first: { k1: 1609502096, k2: 1609588496 }
                second: { k3: 1609674896, k4: 1609761296, k5: 1609847696 }
            }
            enums: {
                first: { k1: "Foo", k2: "Bar" }
                second: { k3: "Baz", k4: "Foo", k5: "Bar" }
            }
            intEnums: {
                first: { k1: 1, k2: 2 }
                second: { k3: 3, k4: 1, k5: 2 }
            }
        }
    }
])

apply MapOfLists @httpRequestTests([
    {
        id: "RpcV2JsonMapOfListsSerialize"
        documentation: "Serializes maps of lists for all scalar leaf types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfLists"
        body: """
            {
                "booleans": {"first": [true, false], "second": [false, true, true]},
                "integers": {"first": [1, 2], "second": [3, 4, 5]},
                "strings": {"first": ["a", "b"], "second": ["c", "d", "e"]},
                "blobs": {"first": ["Zm9v", "YmFy"], "second": ["YmF6", "cXV4", "cXV1eA=="]},
                "timestamps": {"first": [1609502096, 1609588496], "second": [1609674896, 1609761296, 1609847696]},
                "enums": {"first": ["Foo", "Bar"], "second": ["Baz", "Foo", "Bar"]},
                "intEnums": {"first": [1, 2], "second": [3, 1, 2]}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            booleans: {
                first: [true, false]
                second: [false, true, true]
            }
            integers: {
                first: [1, 2]
                second: [3, 4, 5]
            }
            strings: {
                first: ["a", "b"]
                second: ["c", "d", "e"]
            }
            blobs: {
                first: ["foo", "bar"]
                second: ["baz", "qux", "quux"]
            }
            timestamps: {
                first: [1609502096, 1609588496]
                second: [1609674896, 1609761296, 1609847696]
            }
            enums: {
                first: ["Foo", "Bar"]
                second: ["Baz", "Foo", "Bar"]
            }
            intEnums: {
                first: [1, 2]
                second: [3, 1, 2]
            }
        }
    }
])

apply MapOfLists @httpResponseTests([
    {
        id: "RpcV2JsonMapOfListsDeserialize"
        documentation: "Deserializes maps of lists for all scalar leaf types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "booleans": {"first": [true, false], "second": [false, true, true]},
                "integers": {"first": [1, 2], "second": [3, 4, 5]},
                "strings": {"first": ["a", "b"], "second": ["c", "d", "e"]},
                "blobs": {"first": ["Zm9v", "YmFy"], "second": ["YmF6", "cXV4", "cXV1eA=="]},
                "timestamps": {"first": [1609502096, 1609588496], "second": [1609674896, 1609761296, 1609847696]},
                "enums": {"first": ["Foo", "Bar"], "second": ["Baz", "Foo", "Bar"]},
                "intEnums": {"first": [1, 2], "second": [3, 1, 2]}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            booleans: {
                first: [true, false]
                second: [false, true, true]
            }
            integers: {
                first: [1, 2]
                second: [3, 4, 5]
            }
            strings: {
                first: ["a", "b"]
                second: ["c", "d", "e"]
            }
            blobs: {
                first: ["foo", "bar"]
                second: ["baz", "qux", "quux"]
            }
            timestamps: {
                first: [1609502096, 1609588496]
                second: [1609674896, 1609761296, 1609847696]
            }
            enums: {
                first: ["Foo", "Bar"]
                second: ["Baz", "Foo", "Bar"]
            }
            intEnums: {
                first: [1, 2]
                second: [3, 1, 2]
            }
        }
    }
])

apply MapOfUnions @httpRequestTests([
    {
        id: "RpcV2JsonMapOfUnionsSerialize"
        documentation: "Serializes a map of unions with different variants"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfUnions"
        body: """
            {
                "values": {
                    "first": {"stringValue": "hello"},
                    "second": {"integerValue": 42}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: {
                first: { stringValue: "hello" }
                second: { integerValue: 42 }
            }
        }
    }
])

apply MapOfUnions @httpResponseTests([
    {
        id: "RpcV2JsonMapOfUnionsDeserialize"
        documentation: "Deserializes a map of unions with different variants"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": {
                    "first": {"stringValue": "hello"},
                    "second": {"integerValue": 42}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: {
                first: { stringValue: "hello" }
                second: { integerValue: 42 }
            }
        }
    }
])

apply SparseListOfStructs @httpRequestTests([
    {
        id: "RpcV2JsonSparseListOfStructsNullAtStart"
        documentation: "Serializes a sparse list with a null as the first element"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseListOfStructs"
        body: """
            {
                "values": [
                    null,
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: [
                null
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
            ]
        }
    }
    {
        id: "RpcV2JsonSparseListOfStructsNullInMiddle"
        documentation: "Serializes a sparse list with a null as the middle element"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseListOfStructs"
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    null,
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: [
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                null
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
            ]
        }
    }
    {
        id: "RpcV2JsonSparseListOfStructsNullAtEnd"
        documentation: "Serializes a sparse list with a null as the last element"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseListOfStructs"
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false},
                    null
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: [
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
                null
            ]
        }
    }
])

apply SparseListOfStructs @httpResponseTests([
    {
        id: "RpcV2JsonSparseListOfStructsNullAtStartResponse"
        documentation: "Deserializes a sparse list with a null as the first element"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": [
                    null,
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: [
                null
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
            ]
        }
    }
    {
        id: "RpcV2JsonSparseListOfStructsNullInMiddleResponse"
        documentation: "Deserializes a sparse list with a null as the middle element"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    null,
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: [
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                null
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
            ]
        }
    }
    {
        id: "RpcV2JsonSparseListOfStructsNullAtEndResponse"
        documentation: "Deserializes a sparse list with a null as the last element"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false},
                    null
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: [
                {
                    stringMember: "foo"
                    integerMember: 1
                    booleanMember: true
                }
                {
                    stringMember: "bar"
                    integerMember: 2
                    booleanMember: false
                }
                null
            ]
        }
    }
])

apply SparseMapOfStructs @httpRequestTests([
    {
        id: "RpcV2JsonSparseMapOfStructsNullAtStart"
        documentation: "Serializes a sparse map with a null as the first entry"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseMapOfStructs"
        body: """
            {
                "values": {
                    "a": null,
                    "b": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "c": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: {
                a: null
                b: { stringMember: "foo", integerMember: 1, booleanMember: true }
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfStructsNullInMiddle"
        documentation: "Serializes a sparse map with a null as the middle entry"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseMapOfStructs"
        body: """
            {
                "values": {
                    "a": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "b": null,
                    "c": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: null
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfStructsNullAtEnd"
        documentation: "Serializes a sparse map with a null as the last entry"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/SparseMapOfStructs"
        body: """
            {
                "values": {
                    "a": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "b": {"stringMember": "bar", "integerMember": 2, "booleanMember": false},
                    "c": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: { stringMember: "bar", integerMember: 2, booleanMember: false }
                c: null
            }
        }
    }
])

apply SparseMapOfStructs @httpResponseTests([
    {
        id: "RpcV2JsonSparseMapOfStructsNullAtStartResponse"
        documentation: "Deserializes a sparse map with a null as the first entry"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": {
                    "a": null,
                    "b": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "c": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: {
                a: null
                b: { stringMember: "foo", integerMember: 1, booleanMember: true }
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfStructsNullInMiddleResponse"
        documentation: "Deserializes a sparse map with a null as the middle entry"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": {
                    "a": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "b": null,
                    "c": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: null
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "RpcV2JsonSparseMapOfStructsNullAtEndResponse"
        documentation: "Deserializes a sparse map with a null as the last entry"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": {
                    "a": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "b": {"stringMember": "bar", "integerMember": 2, "booleanMember": false},
                    "c": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: { stringMember: "bar", integerMember: 2, booleanMember: false }
                c: null
            }
        }
    }
])

apply RecursiveStruct @httpRequestTests([
    {
        id: "RpcV2JsonRecursiveStructTwoLevelsDeep"
        documentation: "Serializes recursive struct two levels deep via recursiveMember"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RecursiveStruct"
        body: """
            {
                "value": {
                    "stringMember": "level1",
                    "recursiveMember": {
                        "stringMember": "level2"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                stringMember: "level1"
                recursiveMember: { stringMember: "level2" }
            }
        }
    }
    {
        id: "RpcV2JsonRecursiveStructViaList"
        documentation: "Serializes recursive struct via list"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RecursiveStruct"
        body: """
            {
                "value": {
                    "recursiveList": [
                        {
                            "stringMember": "inList1"
                        },
                        {
                            "stringMember": "inList2"
                        }
                    ]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                recursiveList: [
                    {
                        stringMember: "inList1"
                    }
                    {
                        stringMember: "inList2"
                    }
                ]
            }
        }
    }
    {
        id: "RpcV2JsonRecursiveStructViaMap"
        documentation: "Serializes recursive struct via map"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RecursiveStruct"
        body: """
            {
                "value": {
                    "recursiveMap": {
                        "key1": {
                            "stringMember": "inMap1"
                        },
                        "key2": {
                            "stringMember": "inMap2"
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                recursiveMap: {
                    key1: { stringMember: "inMap1" }
                    key2: { stringMember: "inMap2" }
                }
            }
        }
    }
])

apply RecursiveStruct @httpResponseTests([
    {
        id: "RpcV2JsonRecursiveStructTwoLevelsDeepDeserialize"
        documentation: "Deserializes recursive struct two levels deep via recursiveMember"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "stringMember": "level1",
                    "recursiveMember": {
                        "stringMember": "level2"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                stringMember: "level1"
                recursiveMember: { stringMember: "level2" }
            }
        }
    }
    {
        id: "RpcV2JsonRecursiveStructViaListDeserialize"
        documentation: "Deserializes recursive struct via list"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "recursiveList": [
                        {
                            "stringMember": "inList1"
                        },
                        {
                            "stringMember": "inList2"
                        }
                    ]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                recursiveList: [
                    {
                        stringMember: "inList1"
                    }
                    {
                        stringMember: "inList2"
                    }
                ]
            }
        }
    }
    {
        id: "RpcV2JsonRecursiveStructViaMapDeserialize"
        documentation: "Deserializes recursive struct via map"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "recursiveMap": {
                        "key1": {
                            "stringMember": "inMap1"
                        },
                        "key2": {
                            "stringMember": "inMap2"
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                recursiveMap: {
                    key1: { stringMember: "inMap1" }
                    key2: { stringMember: "inMap2" }
                }
            }
        }
    }
])

apply RecursiveUnion @httpRequestTests([
    {
        id: "RpcV2JsonRecursiveUnionDirectRecursion"
        documentation: "Serializes recursive union two levels deep"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RecursiveUnion"
        body: """
            {
                "value": {
                    "recursiveValue": {
                        "stringValue": "nested"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                recursiveValue: { stringValue: "nested" }
            }
        }
    }
    {
        id: "RpcV2JsonRecursiveUnionThroughStruct"
        documentation: "Serializes recursive union through struct"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RecursiveUnion"
        body: """
            {
                "value": {
                    "structValue": {
                        "value": {
                            "stringValue": "deep"
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                structValue: {
                    value: { stringValue: "deep" }
                }
            }
        }
    }
])

apply RecursiveUnion @httpResponseTests([
    {
        id: "RpcV2JsonRecursiveUnionDirectRecursionDeserialize"
        documentation: "Deserializes recursive union two levels deep"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "recursiveValue": {
                        "stringValue": "nested"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                recursiveValue: { stringValue: "nested" }
            }
        }
    }
    {
        id: "RpcV2JsonRecursiveUnionThroughStructDeserialize"
        documentation: "Deserializes recursive union through struct"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "structValue": {
                        "value": {
                            "stringValue": "deep"
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                structValue: {
                    value: { stringValue: "deep" }
                }
            }
        }
    }
])

apply EmptyInputOutput @httpRequestTests([
    {
        id: "RpcV2JsonEmptyInputOutputSerialize"
        documentation: "Serializes empty input as empty JSON object"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/EmptyInputOutput"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {}
    }
])

apply EmptyInputOutput @httpResponseTests([
    {
        id: "RpcV2JsonEmptyInputOutputDeserialize"
        documentation: "Deserializes empty JSON object body"
        protocol: rpcv2Json
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {}
    }
    {
        id: "RpcV2JsonEmptyInputOutputDeserializeEmptyBody"
        tags: ["absent-response-body"]
        documentation: "Deserializes empty string body as valid empty output"
        protocol: rpcv2Json
        code: 200
        body: ""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: {}
    }
])

apply NoInputOutput @httpRequestTests([
    {
        id: "RpcV2JsonNoInputOutputSerialize"
        documentation: """
            Sends no body at all for an operation whose input targets the Unit
            shape, and omits Content-Type accordingly"""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NoInputOutput"
        body: ""
        headers: { "smithy-protocol": "rpc-v2-json", Accept: "application/json" }
        forbidHeaders: ["Content-Type", "X-Amz-Target"]
        params: {}
    }
])

apply SimpleError @httpResponseTests([
    {
        id: "RpcV2JsonSimpleErrorDeserialize"
        documentation: "Deserializes simple client error with __type discrimination"
        protocol: rpcv2Json
        code: 400
        body: """
            {
                "__type": "smithy.protocoltests.corpus#SimpleError",
                "message": "oops"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { message: "oops" }
    }
])

apply ComplexError @httpResponseTests([
    {
        id: "RpcV2JsonComplexErrorDeserialize"
        documentation: "Deserializes complex server error with nested struct"
        protocol: rpcv2Json
        code: 500
        body: """
            {
                "__type": "smithy.protocoltests.corpus#ComplexError",
                "message": "something went wrong",
                "code": 42,
                "nested": {
                    "stringMember": "nestedValue",
                    "integerMember": 99
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            message: "something went wrong"
            code: 42
            nested: { stringMember: "nestedValue", integerMember: 99 }
        }
    }
])
