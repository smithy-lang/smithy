$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#InitialHttpRequest
use smithy.test#eventStreamTests
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply ScalarMembers @httpRequestTests([
    {
        id: "RpcV2JsonScalarMembersArbitraryPrecision"
        documentation: """
            Serializes integers and decimals too large for int64/float64 as JSON
            STRINGS, which is how this protocol keeps arbitrary precision intact
            (a JSON number would be routed through a double by many parsers)"""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "bigIntegerMember": "1234567890123456789012345678901234567890",
                "bigDecimalMember": "3.141592653589793238462643383279502884197"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        params: {
            bigIntegerMember: 1234567890123456789012345678901234567890
            bigDecimalMember: 3.141592653589793238462643383279502884197
        }
        tags: ["arbitrary-precision"]
    }
])

apply ScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonScalarMembersArbitraryPrecisionDeserialize"
        documentation: "Deserializes arbitrary-precision numbers without losing digits"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "bigIntegerMember": "1234567890123456789012345678901234567890",
                "bigDecimalMember": "3.141592653589793238462643383279502884197"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            bigIntegerMember: 1234567890123456789012345678901234567890
            bigDecimalMember: 3.141592653589793238462643383279502884197
        }
        tags: ["arbitrary-precision"]
    }
])

apply ScalarMembers @httpRequestTests([
    {
        id: "RpcV2JsonScalarMembersNumericMinima"
        documentation: "Serializes the minimum value of each integral type, including negatives"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "byteMember": -128,
                "shortMember": -32768,
                "integerMember": -2147483648,
                "longMember": -9007199254740993
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        params: { byteMember: -128, shortMember: -32768, integerMember: -2147483648, longMember: -9007199254740993 }
        tags: ["numeric-boundaries"]
    }
    {
        id: "RpcV2JsonScalarMembersNumericMaxima"
        documentation: """
            Serializes the maximum value of each integral type, plus a long above
            2^53 and a double at full 17-significant-digit precision"""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "byteMember": 127,
                "shortMember": 32767,
                "integerMember": 2147483647,
                "longMember": 9007199254740993,
                "doubleMember": 123456789.12345679
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        params: {
            byteMember: 127
            shortMember: 32767
            integerMember: 2147483647
            longMember: 9007199254740993
            doubleMember: 123456789.12345679
        }
        tags: ["numeric-boundaries"]
    }
])

apply ScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonScalarMembersNumericMinimaDeserialize"
        documentation: "Deserializes the minimum value of each integral type, including negatives"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "byteMember": -128,
                "shortMember": -32768,
                "integerMember": -2147483648,
                "longMember": -9007199254740993
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { byteMember: -128, shortMember: -32768, integerMember: -2147483648, longMember: -9007199254740993 }
        tags: ["numeric-boundaries"]
    }
    {
        id: "RpcV2JsonScalarMembersNumericMaximaDeserialize"
        documentation: "Deserializes maxima, a long above 2^53, and a full-precision double"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "byteMember": 127,
                "shortMember": 32767,
                "integerMember": 2147483647,
                "longMember": 9007199254740993,
                "doubleMember": 123456789.12345679
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            byteMember: 127
            shortMember: 32767
            integerMember: 2147483647
            longMember: 9007199254740993
            doubleMember: 123456789.12345679
        }
        tags: ["numeric-boundaries"]
    }
])

apply ScalarMembers @httpRequestTests([
    {
        id: "RpcV2JsonScalarMembersStringEscaping"
        documentation: "Escapes quotes, backslashes, control characters and preserves non-BMP text"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "stringMember": "quote \\" backslash \\\\ newline \\n tab \\t control \\u0001 astral \\ud83d\\ude00"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        params: { stringMember: "quote \" backslash \\ newline \n tab \t control \u0001 astral \ud83d\ude00" }
        tags: ["string-escaping"]
    }
])

apply ScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonScalarMembersStringEscapingDeserialize"
        documentation: "Unescapes quotes, backslashes, control characters and non-BMP text"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "stringMember": "quote \\" backslash \\\\ newline \\n tab \\t control \\u0001 astral \\ud83d\\ude00"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { stringMember: "quote \" backslash \\ newline \n tab \t control \u0001 astral \ud83d\ude00" }
        tags: ["string-escaping"]
    }
])

apply ScalarMembers @httpRequestTests([
    {
        id: "RpcV2JsonScalarMembersBlobPaddingOneByte"
        documentation: "Base64-encodes a one-byte blob, which requires two padding characters"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "blobMember": "Zg=="
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        params: { blobMember: "f" }
        tags: ["blob-encoding"]
    }
    {
        id: "RpcV2JsonScalarMembersBlobPaddingTwoBytes"
        documentation: "Base64-encodes a two-byte blob, which requires one padding character"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ScalarMembers"
        body: """
            {
                "blobMember": "Zm8="
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        params: { blobMember: "fo" }
        tags: ["blob-encoding"]
    }
])

apply ScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonScalarMembersBlobPaddingOneByteDeserialize"
        documentation: "Decodes a base64 blob with two padding characters"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "blobMember": "Zg=="
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { blobMember: "f" }
        tags: ["blob-encoding"]
    }
    {
        id: "RpcV2JsonScalarMembersBlobPaddingTwoBytesDeserialize"
        documentation: "Decodes a base64 blob with one padding character"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "blobMember": "Zm8="
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { blobMember: "fo" }
        tags: ["blob-encoding"]
    }
])

apply ScalarMembers @httpResponseTests([
    {
        id: "RpcV2JsonScalarMembersFractionalSecondsDeserialize"
        documentation: "Preserves sub-second precision carried in the fraction of an epoch-seconds number"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "epochSecondsMember": 1609502096.123
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { epochSecondsMember: 1609502096.123 }
        appliesTo: "client"
        tags: ["timestamp-fractional-seconds"]
    }
])

apply HttpErrorGone @httpResponseTests([
    {
        id: "RpcV2JsonErrorIgnoresErrorTypeHeader"
        documentation: """
            Resolves the error from the body's __type even when an
            X-Amzn-ErrorType header names a different shape, because clients
            MUST ignore that header under this protocol"""
        protocol: rpcv2Json
        code: 410
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "X-Amzn-ErrorType": "smithy.protocoltests.corpus#HttpErrorConflict"
        }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
        tags: ["error-discrimination"]
    }
    {
        id: "RpcV2JsonErrorIgnoresCodeBodyField"
        documentation: """
            Resolves the error from __type even when Code and code body fields
            name a different shape, because they MUST NOT be used to
            distinguish which error is contained"""
        protocol: rpcv2Json
        code: 410
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorGone",
                "Code": "HttpErrorConflict",
                "code": "HttpErrorServiceUnavailable",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
        tags: ["error-discrimination"]
    }
])

apply UnionOfScalars @httpResponseTests([
    {
        id: "RpcV2JsonUnionIgnoresUnrecognizedTypeMember"
        documentation: "Ignores an unrecognized __type member alongside a union variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "__type": "smithy.protocoltests.corpus#NotARealShape",
                    "stringValue": "union string"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: { stringValue: "union string" }
        }
        appliesTo: "client"
        tags: ["unknown-fields", "error-discrimination"]
    }
])

apply EventStreamRequest @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamRequestEnvelope"
        documentation: """
            A streaming request body uses the event stream media type, while
            Accept stays application/json because this operation's response is
            a buffered RPC response"""
        protocol: rpcv2Json
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2JsonCorpusTests/operation/EventStreamRequest"
            headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/vnd.amazon.eventstream", Accept: "application/json" }
        }
        initialRequestShape: InitialHttpRequest
    }
])

apply EventStreamResponse @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamResponseEnvelope"
        documentation: """
            A buffered request that expects a streaming response sets Accept to
            the event stream media type while its own Content-Type stays
            application/json"""
        protocol: rpcv2Json
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2JsonCorpusTests/operation/EventStreamResponse"
            headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/vnd.amazon.eventstream" }
        }
        initialRequestShape: InitialHttpRequest
    }
])

apply NoInputOutput @httpRequestTests([
    {
        id: "RpcV2JsonNoInputServerAllowsEmptyJsonObject"
        documentation: "Servers should accept an empty JSON object for an operation with no input"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NoInputOutput"
        body: "{}"
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        appliesTo: "server"
    }
    {
        id: "RpcV2JsonNoInputServerAllowsEmptyBody"
        documentation: """
            Servers should accept an empty body for an operation with no input,
            and must not fail merely because Accept is set"""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NoInputOutput"
        body: ""
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        appliesTo: "server"
    }
])

apply EventStreamResponse @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamResponseArbitraryPrecision"
        documentation: """
            An event payload carries arbitrary-precision numbers as JSON STRINGS,
            the same as a buffered body does, so the framing layer does not
            reintroduce a double round-trip"""
        protocol: rpcv2Json
        events: [
            {
                type: "response"
                params: {
                    scalarsEvent: {
                        bigIntegerMember: 1234567890123456789012345678901234567890
                        bigDecimalMember: 3.141592653589793238462643383279502884197
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"bigIntegerMember":"1234567890123456789012345678901234567890","bigDecimalMember":"3.141592653589793238462643383279502884197"}"""
                bodyMediaType: "application/json"
            }
        ]
        tags: ["arbitrary-precision"]
    }
])
