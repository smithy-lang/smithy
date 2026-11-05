$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use smithy.test#InitialHttpRequest
use smithy.test#eventStreamTests
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply smithy.protocoltests.corpus#ScalarMembers @httpRequestTests([
    {
        id: "AwsJson10ScalarMembersArbitraryPrecision"
        documentation: """
            Serializes integers and decimals too large for int64/float64 as
            JSON numbers, carrying every digit even though many parsers would
            route a JSON number through a double"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "bigIntegerMember": 1234567890123456789012345678901234567890,
                "bigDecimalMember": 3.141592653589793238462643383279502884197
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        params: {
            bigIntegerMember: 1234567890123456789012345678901234567890
            bigDecimalMember: 3.141592653589793238462643383279502884197
        }
        tags: ["arbitrary-precision"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersArbitraryPrecisionDeserialize"
        documentation: "Deserializes arbitrary-precision numbers without losing digits"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "bigIntegerMember": 1234567890123456789012345678901234567890,
                "bigDecimalMember": 3.141592653589793238462643383279502884197
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            bigIntegerMember: 1234567890123456789012345678901234567890
            bigDecimalMember: 3.141592653589793238462643383279502884197
        }
        tags: ["arbitrary-precision"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpRequestTests([
    {
        id: "AwsJson10ScalarMembersNumericMinima"
        documentation: """
            Serializes the minimum value of a byte, short, and integer, including
            negatives. The long is -(2^53 + 1) rather than the minimum of the long
            type: it is the smallest negative integer a double cannot represent
            exactly, so it catches implementations that route longs through a
            double"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "byteMember": -128,
                "shortMember": -32768,
                "integerMember": -2147483648,
                "longMember": -9007199254740993
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        params: { byteMember: -128, shortMember: -32768, integerMember: -2147483648, longMember: -9007199254740993 }
        tags: ["numeric-boundaries"]
    }
    {
        id: "AwsJson10ScalarMembersNumericMaxima"
        documentation: """
            Serializes the maximum value of each integral type, plus a long above
            2^53 and a double at full 17-significant-digit precision"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "byteMember": 127,
                "shortMember": 32767,
                "integerMember": 2147483647,
                "longMember": 9007199254740993,
                "doubleMember": 123456789.12345679
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
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

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersNumericMinimaDeserialize"
        documentation: """
            Deserializes the minimum value of a byte, short, and integer,
            including negatives. The long is -(2^53 + 1) rather than the minimum
            of the long type: it is the smallest negative integer a double cannot
            represent exactly, so it catches implementations that route longs
            through a double"""
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "byteMember": -128,
                "shortMember": -32768,
                "integerMember": -2147483648,
                "longMember": -9007199254740993
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { byteMember: -128, shortMember: -32768, integerMember: -2147483648, longMember: -9007199254740993 }
        tags: ["numeric-boundaries"]
    }
    {
        id: "AwsJson10ScalarMembersNumericMaximaDeserialize"
        documentation: "Deserializes maxima, a long above 2^53, and a full-precision double"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#ScalarMembers @httpRequestTests([
    {
        id: "AwsJson10ScalarMembersStringEscaping"
        documentation: "Escapes quotes, backslashes, control characters and preserves non-BMP text"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "stringMember": "quote \\" backslash \\\\ newline \\n tab \\t control \\u0001 astral \\ud83d\\ude00"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        params: { stringMember: "quote \" backslash \\ newline \n tab \t control \u0001 astral \ud83d\ude00" }
        tags: ["string-escaping"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersStringEscapingDeserialize"
        documentation: "Unescapes quotes, backslashes, control characters and non-BMP text"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "stringMember": "quote \\" backslash \\\\ newline \\n tab \\t control \\u0001 astral \\ud83d\\ude00"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { stringMember: "quote \" backslash \\ newline \n tab \t control \u0001 astral \ud83d\ude00" }
        tags: ["string-escaping"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpRequestTests([
    {
        id: "AwsJson10ScalarMembersBlobPaddingOneByte"
        documentation: "Base64-encodes a one-byte blob, which requires two padding characters"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "blobMember": "Zg=="
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        params: { blobMember: "f" }
        tags: ["blob-encoding"]
    }
    {
        id: "AwsJson10ScalarMembersBlobPaddingTwoBytes"
        documentation: "Base64-encodes a two-byte blob, which requires one padding character"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "blobMember": "Zm8="
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        params: { blobMember: "fo" }
        tags: ["blob-encoding"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersBlobPaddingOneByteDeserialize"
        documentation: "Decodes a base64 blob with two padding characters"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "blobMember": "Zg=="
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { blobMember: "f" }
        tags: ["blob-encoding"]
    }
    {
        id: "AwsJson10ScalarMembersBlobPaddingTwoBytesDeserialize"
        documentation: "Decodes a base64 blob with one padding character"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "blobMember": "Zm8="
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { blobMember: "fo" }
        tags: ["blob-encoding"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersFractionalSecondsDeserialize"
        documentation: "Preserves sub-second precision carried in the fraction of an epoch-seconds number"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "epochSecondsMember": 1609502096.123
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { epochSecondsMember: 1609502096.123 }
        appliesTo: "client"
        tags: ["timestamp-fractional-seconds"]
    }
])

apply smithy.protocoltests.corpus#HttpErrorGone @httpResponseTests([
    {
        id: "AwsJson10ErrorFromErrorTypeHeader"
        tags: ["error-discrimination"]
        documentation: """
            Resolves the error from the X-Amzn-Errortype header when the body
            carries no __type"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amzn-Errortype": "HttpErrorGone" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorFromCodeBodyField"
        tags: ["error-discrimination"]
        documentation: """
            Resolves the error from the body's code field when there is no
            __type or X-Amzn-Errortype"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "code": "HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorTypeHeaderStripsNamespace"
        tags: ["error-discrimination"]
        documentation: """
            Strips the namespace from a shape ID sent in the X-Amzn-Errortype
            header"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amzn-Errortype": "smithy.protocoltests.corpus#HttpErrorGone" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorTypeStripsUri"
        tags: ["error-discrimination"]
        documentation: """
            Strips the legacy URI suffix that follows a colon in the __type
            field"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "HttpErrorGone:http://internal.amazon.com/coral/com.amazon.coral.validate/",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorTypeStripsNamespaceAndUri"
        tags: ["error-discrimination"]
        documentation: """
            Strips both the namespace and the legacy URI suffix from the
            __type field"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorGone:http://internal.amazon.com/coral/com.amazon.coral.validate/",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorTypeBareName"
        tags: ["error-discrimination"]
        documentation: """
            Accepts a bare shape name with no namespace in the __type field"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorFromErrorTypeHeaderWithUri"
        tags: ["error-discrimination"]
        documentation: """
            Strips the legacy URI suffix that follows a colon in the X-Amzn-Errortype header"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
            "X-Amzn-Errortype": "HttpErrorGone:http://internal.amazon.com/coral/com.amazon.coral.validate/"
        }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorFromErrorTypeHeaderWithNamespaceAndUri"
        tags: ["error-discrimination"]
        documentation: """
            Strips both the namespace and the legacy URI suffix from the X-Amzn-Errortype header"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
            "X-Amzn-Errortype": "smithy.protocoltests.corpus#HttpErrorGone:http://internal.amazon.com/coral/com.amazon.coral.validate/"
        }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorFromErrorTypeHeaderNoBody"
        tags: ["error-discrimination"]
        documentation: """
            Resolves the error from the X-Amzn-Errortype header when the response has no body at all"""
        protocol: awsJson1_0
        code: 410
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amzn-Errortype": "HttpErrorGone" }
        params: {}
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorFromCodeBodyFieldWithNamespace"
        tags: ["error-discrimination"]
        documentation: """
            Strips the namespace from a shape ID sent in the body's code field"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "code": "smithy.protocoltests.corpus#HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorFromCodeBodyFieldWithNamespaceAndUri"
        tags: ["error-discrimination"]
        documentation: """
            Strips both the namespace and the legacy URI suffix from the body's code field"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "code": "smithy.protocoltests.corpus#HttpErrorGone:http://internal.amazon.com/coral/com.amazon.coral.validate/",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorTypeUnrecognizedNamespace"
        tags: ["error-discrimination"]
        documentation: """
            Only the part after the # in the __type field is considered, so a namespace the client has never seen does not change which error is surfaced"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "com.example.unrecognized#HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorIgnoresNestedType"
        tags: ["error-discrimination"]
        documentation: """
            A __type property nested inside an unmodeled member is not considered when determining which error to surface"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01",
                "errorDetails": [
                    {
                        "__type": "com.amazon.internal#ErrorDetails",
                        "reason": "some reason"
                    }
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ErrorTypeOnlyBody"
        tags: ["error-discrimination"]
        documentation: """
            Surfaces an error whose body carries only the __type field and none of the error's members"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorGone"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {}
        appliesTo: "client"
    }
])

apply smithy.protocoltests.corpus#UnionOfScalars @httpResponseTests([
    {
        id: "AwsJson10UnionIgnoresUnrecognizedTypeMember"
        documentation: "Ignores an unrecognized __type member alongside a union variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "__type": "smithy.protocoltests.corpus#NotARealShape",
                    "stringValue": "union string"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { stringValue: "union string" }
        }
        appliesTo: "client"
        tags: ["unknown-fields", "error-discrimination"]
    }
])

apply smithy.protocoltests.corpus#EventStreamRequest @eventStreamTests([
    {
        id: "AwsJson10EventStreamRequestEnvelope"
        documentation: """
            A streaming request body uses the event stream media type"""
        protocol: awsJson1_0
        initialRequest: {
            method: "POST"
            uri: "/"
            headers: { "Content-Type": "application/vnd.amazon.eventstream", "X-Amz-Target": "AwsJson10CorpusTests.EventStreamRequest" }
            forbidHeaders: ["Content-Length"]
        }
        initialRequestShape: InitialHttpRequest
    }
])

apply smithy.protocoltests.corpus#EventStreamResponse @eventStreamTests([
    {
        id: "AwsJson10EventStreamResponseEnvelope"
        documentation: """
            A buffered request that expects a streaming response keeps its JSON
            content type"""
        protocol: awsJson1_0
        initialRequest: {
            method: "POST"
            uri: "/"
            headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.EventStreamResponse" }
        }
        initialRequestShape: InitialHttpRequest
    }
])

apply smithy.protocoltests.corpus#NoInputOutput @httpRequestTests([
    {
        id: "AwsJson10NoInputServerAllowsEmptyJsonObject"
        documentation: "Servers should accept an empty JSON object for an operation with no input"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: "{}"
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.NoInputOutput" }
        appliesTo: "server"
    }
    {
        id: "AwsJson10NoInputServerAllowsEmptyBody"
        documentation: """
            Servers should accept an empty body for an operation with no input"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: ""
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.NoInputOutput" }
        appliesTo: "server"
    }
])

apply smithy.protocoltests.corpus#EventStreamResponse @eventStreamTests([
    {
        id: "AwsJson10EventStreamResponseArbitraryPrecision"
        documentation: """
            An event payload carries arbitrary-precision numbers as JSON
            numbers, the same as a buffered body does, so the framing layer
            does not reintroduce a double round-trip"""
        protocol: awsJson1_0
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
                    {"bigIntegerMember":1234567890123456789012345678901234567890,"bigDecimalMember":3.141592653589793238462643383279502884197}"""
                bodyMediaType: "application/json"
            }
        ]
        tags: ["arbitrary-precision"]
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersDeserializeDateTimeNegativeOffset"
        tags: ["timestamp-offset"]
        documentation: """
            Normalizes a date-time with a negative UTC offset to the same
            instant as its UTC form"""
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "dateTimeMember": "2021-01-01T11:34:56-01:00"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { dateTimeMember: 1609504496 }
        appliesTo: "client"
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDateTimePositiveOffset"
        tags: ["timestamp-offset"]
        documentation: """
            Normalizes a date-time with a positive UTC offset to the same
            instant as its UTC form"""
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "dateTimeMember": "2021-01-01T13:34:56+01:00"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { dateTimeMember: 1609504496 }
        appliesTo: "client"
    }
])

apply smithy.protocoltests.corpus#UnionOfScalars @httpResponseTests([
    {
        id: "AwsJson10UnionOfScalarsDeserializeIgnoresNullVariants"
        tags: ["null-on-wire"]
        documentation: """
            Client drops a wire null for every union variant other than the
            one that is set, whatever the variant's type"""
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "booleanValue": null,
                    "byteValue": null,
                    "shortValue": null,
                    "integerValue": null,
                    "longValue": null,
                    "floatValue": null,
                    "doubleValue": null,
                    "stringValue": "only this",
                    "blobValue": null,
                    "timestampValue": null,
                    "enumValue": null,
                    "intEnumValue": null,
                    "listValue": null,
                    "mapValue": null,
                    "structValue": null,
                    "unionValue": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { stringValue: "only this" }
        }
        appliesTo: "client"
    }
])

apply smithy.protocoltests.corpus#NoInputOutput @httpResponseTests([
    {
        id: "AwsJson10NoInputOutputDeserializeIgnoresUnknownFields"
        tags: ["unknown-fields"]
        documentation: """
            Ignores unmodeled members, scalar and nested, in the JSON object
            returned for an operation whose output targets the Unit shape"""
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "unexpected": true,
                "unexpectedNested": {
                    "items": [1, 2],
                    "label": "ignored"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {}
        appliesTo: "client"
    }
])
