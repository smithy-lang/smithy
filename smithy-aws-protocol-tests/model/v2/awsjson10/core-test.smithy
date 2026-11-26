$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply smithy.protocoltests.corpus#ScalarMembers @httpRequestTests([
    {
        id: "AwsJson10ScalarMembersSerializeBoolean"
        documentation: "Serializes a boolean"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "booleanMember": true
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { booleanMember: true }
    }
    {
        id: "AwsJson10ScalarMembersSerializeBooleanZero"
        documentation: "Serializes a boolean whose value is false"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "booleanMember": false
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { booleanMember: false }
    }
    {
        id: "AwsJson10ScalarMembersSerializeByte"
        documentation: "Serializes a byte"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "byteMember": 5
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { byteMember: 5 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeByteZero"
        documentation: "Serializes a byte whose value is zero"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "byteMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { byteMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeShort"
        documentation: "Serializes a short"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "shortMember": 256
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { shortMember: 256 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeShortZero"
        documentation: "Serializes a short whose value is zero"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "shortMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { shortMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeInteger"
        documentation: "Serializes a integer"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "integerMember": 1234
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { integerMember: 1234 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeIntegerZero"
        documentation: "Serializes a integer whose value is zero"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "integerMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { integerMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeLong"
        documentation: "Serializes a long"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "longMember": 999999999999
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { longMember: 999999999999 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeLongZero"
        documentation: "Serializes a long whose value is zero"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "longMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { longMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeFloat"
        documentation: "Serializes a float"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "floatMember": 1.5
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { floatMember: 1.5 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeFloatZero"
        documentation: "Serializes a float whose value is zero"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "floatMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { floatMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeDouble"
        documentation: "Serializes a double"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "doubleMember": 2.5
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { doubleMember: 2.5 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeDoubleZero"
        documentation: "Serializes a double whose value is zero"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "doubleMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { doubleMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeBigInteger"
        tags: ["arbitrary-precision"]
        documentation: "Serializes a bigInteger as a JSON number"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "bigIntegerMember": 12345
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { bigIntegerMember: 12345 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeBigDecimal"
        tags: ["arbitrary-precision"]
        documentation: "Serializes a bigDecimal as a JSON number"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "bigDecimalMember": 123.456
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { bigDecimalMember: 123.456 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeString"
        documentation: "Serializes a string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "stringMember": "hello"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { stringMember: "hello" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeStringZero"
        documentation: "Serializes a string whose value is an empty string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "stringMember": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { stringMember: "" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeBlob"
        documentation: "Serializes a blob as a base64 string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "blobMember": "Zm9v"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { blobMember: "foo" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeBlobZero"
        documentation: "Serializes a blob whose value is an empty blob"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "blobMember": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { blobMember: "" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeDateTime"
        documentation: "Serializes a timestamp with a date-time format as a date-time string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "dateTimeMember": "2021-01-01T12:34:56Z"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { dateTimeMember: 1609504496 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeEpochSeconds"
        documentation: "Serializes a timestamp with an epoch-seconds format as epoch seconds"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "epochSecondsMember": 1612325106
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { epochSecondsMember: 1612325106 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeHttpDate"
        documentation: "Serializes a timestamp with a http-date format as a http-date string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "httpDateMember": "Thu, 04 Mar 2021 05:06:07 GMT"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { httpDateMember: 1614834367 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeStringEnum"
        documentation: "Serializes a string enum"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "stringEnum": "Foo"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { stringEnum: "Foo" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeIntEnum"
        documentation: "Serializes a intEnum"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "intEnum": 1
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { intEnum: 1 }
    }
    {
        id: "AwsJson10ScalarMembersSerializeFloatNaN"
        tags: ["non-finite-floats"]
        documentation: "Serializes a NaN float as a JSON string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "floatMember": "NaN"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { floatMember: "NaN" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeFloatInfinity"
        tags: ["non-finite-floats"]
        documentation: "Serializes a Infinity float as a JSON string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "floatMember": "Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { floatMember: "Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeFloatNegativeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Serializes a -Infinity float as a JSON string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "floatMember": "-Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { floatMember: "-Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeDoubleNaN"
        tags: ["non-finite-floats"]
        documentation: "Serializes a NaN double as a JSON string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "doubleMember": "NaN"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { doubleMember: "NaN" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeDoubleInfinity"
        tags: ["non-finite-floats"]
        documentation: "Serializes a Infinity double as a JSON string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "doubleMember": "Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { doubleMember: "Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersSerializeDoubleNegativeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Serializes a -Infinity double as a JSON string"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "doubleMember": "-Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { doubleMember: "-Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersOmitsNullValues"
        tags: ["null-on-wire"]
        documentation: "Non-sparse struct members that are null are omitted"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "stringMember": "only this"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ScalarMembers" }
        requireHeaders: ["Content-Length"]
        params: { stringMember: "only this" }
    }
])

apply smithy.protocoltests.corpus#ScalarMembers @httpResponseTests([
    {
        id: "AwsJson10ScalarMembersDeserializeBoolean"
        documentation: "Deserializes a boolean"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "booleanMember": true
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { booleanMember: true }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeBooleanZero"
        documentation: "Deserializes a boolean whose value is false"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "booleanMember": false
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { booleanMember: false }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeByte"
        documentation: "Deserializes a byte"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "byteMember": 5
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { byteMember: 5 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeByteZero"
        documentation: "Deserializes a byte whose value is zero"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "byteMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { byteMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeShort"
        documentation: "Deserializes a short"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "shortMember": 256
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { shortMember: 256 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeShortZero"
        documentation: "Deserializes a short whose value is zero"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "shortMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { shortMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeInteger"
        documentation: "Deserializes a integer"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "integerMember": 1234
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { integerMember: 1234 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeIntegerZero"
        documentation: "Deserializes a integer whose value is zero"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "integerMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { integerMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeLong"
        documentation: "Deserializes a long"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "longMember": 999999999999
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { longMember: 999999999999 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeLongZero"
        documentation: "Deserializes a long whose value is zero"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "longMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { longMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeFloat"
        documentation: "Deserializes a float"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "floatMember": 1.5
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { floatMember: 1.5 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeFloatZero"
        documentation: "Deserializes a float whose value is zero"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "floatMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { floatMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDouble"
        documentation: "Deserializes a double"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "doubleMember": 2.5
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { doubleMember: 2.5 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDoubleZero"
        documentation: "Deserializes a double whose value is zero"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "doubleMember": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { doubleMember: 0 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeBigInteger"
        tags: ["arbitrary-precision"]
        documentation: "Deserializes a bigInteger from a JSON number"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "bigIntegerMember": 12345
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { bigIntegerMember: 12345 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeBigDecimal"
        tags: ["arbitrary-precision"]
        documentation: "Deserializes a bigDecimal from a JSON number"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "bigDecimalMember": 123.456
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { bigDecimalMember: 123.456 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeString"
        documentation: "Deserializes a string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "stringMember": "hello"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { stringMember: "hello" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeStringZero"
        documentation: "Deserializes a string whose value is an empty string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "stringMember": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { stringMember: "" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeBlob"
        documentation: "Deserializes a blob from a base64 string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "blobMember": "Zm9v"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { blobMember: "foo" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeBlobZero"
        documentation: "Deserializes a blob whose value is an empty blob"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "blobMember": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { blobMember: "" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDateTime"
        documentation: "Deserializes a timestamp with a date-time format from a date-time string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "dateTimeMember": "2021-01-01T12:34:56Z"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { dateTimeMember: 1609504496 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeEpochSeconds"
        documentation: "Deserializes a timestamp with an epoch-seconds format from epoch seconds"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "epochSecondsMember": 1612325106
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { epochSecondsMember: 1612325106 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeHttpDate"
        documentation: "Deserializes a timestamp with a http-date format from a http-date string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "httpDateMember": "Thu, 04 Mar 2021 05:06:07 GMT"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { httpDateMember: 1614834367 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeStringEnum"
        documentation: "Deserializes a string enum"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "stringEnum": "Foo"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { stringEnum: "Foo" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeIntEnum"
        documentation: "Deserializes a intEnum"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "intEnum": 1
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { intEnum: 1 }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeFloatNaN"
        tags: ["non-finite-floats"]
        documentation: "Deserializes a NaN float from a JSON string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "floatMember": "NaN"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { floatMember: "NaN" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeFloatInfinity"
        tags: ["non-finite-floats"]
        documentation: "Deserializes a Infinity float from a JSON string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "floatMember": "Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { floatMember: "Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeFloatNegativeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Deserializes a -Infinity float from a JSON string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "floatMember": "-Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { floatMember: "-Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDoubleNaN"
        tags: ["non-finite-floats"]
        documentation: "Deserializes a NaN double from a JSON string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "doubleMember": "NaN"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { doubleMember: "NaN" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDoubleInfinity"
        tags: ["non-finite-floats"]
        documentation: "Deserializes a Infinity double from a JSON string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "doubleMember": "Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { doubleMember: "Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeDoubleNegativeInfinity"
        tags: ["non-finite-floats"]
        documentation: "Deserializes a -Infinity double from a JSON string"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "doubleMember": "-Infinity"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { doubleMember: "-Infinity" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeIgnoresNullValues"
        tags: ["null-on-wire"]
        documentation: "Client drops a wire null for a dense struct member"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "stringMember": "only this",
                "integerMember": null,
                "booleanMember": null
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        appliesTo: "client"
        params: { stringMember: "only this" }
    }
    {
        id: "AwsJson10ScalarMembersDeserializeIgnoresUnknownFields"
        tags: ["unknown-fields"]
        documentation: "Client ignores unrecognized fields in the response"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "stringMember": "hello",
                "unknownField": "ignored",
                "anotherUnknown": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        appliesTo: "client"
        params: { stringMember: "hello" }
    }
])

apply smithy.protocoltests.corpus#ListOfScalars @httpRequestTests([
    {
        id: "AwsJson10ListOfScalarsPopulated"
        documentation: "Serializes all scalar list types"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ListOfScalars" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10ListOfScalarsEmpty"
        tags: ["empty"]
        documentation: "Serializes empty lists"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ListOfScalars" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#ListOfScalars @httpResponseTests([
    {
        id: "AwsJson10ListOfScalarsPopulatedResponse"
        documentation: "Deserializes all scalar list types"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10ListOfScalarsEmptyResponse"
        tags: ["empty"]
        documentation: "Deserializes empty lists"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#SparseListOfScalars @httpRequestTests([
    {
        id: "AwsJson10SparseListOfScalarsNullAtStart"
        documentation: "Serializes sparse lists with a null as the first element"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseListOfScalars" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10SparseListOfScalarsNullInMiddle"
        documentation: "Serializes sparse lists with a null as the middle element"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseListOfScalars" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10SparseListOfScalarsNullAtEnd"
        documentation: "Serializes sparse lists with a null as the last element"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseListOfScalars" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#SparseListOfScalars @httpResponseTests([
    {
        id: "AwsJson10SparseListOfScalarsNullAtStartResponse"
        documentation: "Deserializes sparse lists with a null as the first element"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10SparseListOfScalarsNullInMiddleResponse"
        documentation: "Deserializes sparse lists with a null as the middle element"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10SparseListOfScalarsNullAtEndResponse"
        documentation: "Deserializes sparse lists with a null as the last element"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#MapOfScalars @httpRequestTests([
    {
        id: "AwsJson10MapOfScalarsPopulated"
        documentation: "Serializes all scalar map types"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.MapOfScalars" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10MapOfScalarsEmpty"
        tags: ["empty"]
        documentation: "Serializes empty maps"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.MapOfScalars" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#MapOfScalars @httpResponseTests([
    {
        id: "AwsJson10MapOfScalarsPopulatedResponse"
        documentation: "Deserializes all scalar map types"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10MapOfScalarsEmptyResponse"
        tags: ["empty"]
        documentation: "Deserializes empty maps"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#SparseMapOfScalars @httpRequestTests([
    {
        id: "AwsJson10SparseMapOfScalarsNullAtStart"
        documentation: "Serializes sparse maps with a null as the first entry"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseMapOfScalars" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10SparseMapOfScalarsNullInMiddle"
        documentation: "Serializes sparse maps with a null as the middle entry"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseMapOfScalars" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10SparseMapOfScalarsNullAtEnd"
        documentation: "Serializes sparse maps with a null as the last entry"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseMapOfScalars" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#SparseMapOfScalars @httpResponseTests([
    {
        id: "AwsJson10SparseMapOfScalarsNullAtStartResponse"
        documentation: "Deserializes sparse maps with a null as the first entry"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10SparseMapOfScalarsNullInMiddleResponse"
        documentation: "Deserializes sparse maps with a null as the middle entry"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10SparseMapOfScalarsNullAtEndResponse"
        documentation: "Deserializes sparse maps with a null as the last entry"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#StructOfScalars @httpRequestTests([
    {
        id: "AwsJson10StructOfScalarsPopulated"
        documentation: "Serializes a fully populated nested struct"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
                    "dateTimeMember": "2021-01-01T12:34:56Z",
                    "epochSecondsMember": 1612325106,
                    "httpDateMember": "Thu, 04 Mar 2021 05:06:07 GMT",
                    "stringEnum": "Foo",
                    "intEnum": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.StructOfScalars" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#StructOfScalars @httpResponseTests([
    {
        id: "AwsJson10StructOfScalarsPopulatedResponse"
        documentation: "Deserializes a fully populated nested struct"
        protocol: awsJson1_0
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
                    "dateTimeMember": "2021-01-01T12:34:56Z",
                    "epochSecondsMember": 1612325106,
                    "httpDateMember": "Thu, 04 Mar 2021 05:06:07 GMT",
                    "stringEnum": "Foo",
                    "intEnum": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#StructOfScalars @httpRequestTests([
    {
        id: "AwsJson10StructOfScalarsAllMembersUnset"
        tags: ["empty"]
        documentation: """
            Serializes a nested struct whose members are all unset as an empty JSON object"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.StructOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {}
        }
    }
    {
        id: "AwsJson10StructOfScalarsEmptyStruct"
        tags: ["empty"]
        documentation: """
            Serializes a member that targets a structure with no members as an empty JSON object"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "emptyValue": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.StructOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            emptyValue: {}
        }
    }
])

apply smithy.protocoltests.corpus#StructOfScalars @httpResponseTests([
    {
        id: "AwsJson10StructOfScalarsAllMembersUnsetResponse"
        tags: ["empty"]
        documentation: "Deserializes a nested struct whose members are all absent"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {}
        }
    }
    {
        id: "AwsJson10StructOfScalarsEmptyStructResponse"
        tags: ["empty"]
        documentation: "Deserializes a member that targets a structure with no members"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "emptyValue": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            emptyValue: {}
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfScalars @httpRequestTests([
    {
        id: "AwsJson10UnionOfScalarsStringSerialize"
        documentation: "Serializes union string variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "stringValue": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { stringValue: "hello" }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsBooleanSerialize"
        documentation: "Serializes union boolean variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "booleanValue": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { booleanValue: true }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsByteSerialize"
        documentation: "Serializes union byte variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "byteValue": 9
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { byteValue: 9 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsShortSerialize"
        documentation: "Serializes union short variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "shortValue": 512
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { shortValue: 512 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsIntegerSerialize"
        documentation: "Serializes union integer variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "integerValue": 42
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { integerValue: 42 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsLongSerialize"
        documentation: "Serializes union long variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "longValue": 999999999999
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { longValue: 999999999999 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsFloatSerialize"
        documentation: "Serializes union float variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "floatValue": 1.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { floatValue: 1.5 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsDoubleSerialize"
        documentation: "Serializes union double variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "doubleValue": 2.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { doubleValue: 2.5 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsBlobSerialize"
        documentation: "Serializes union blob variant (base64 on wire)"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "blobValue": "Zm9v"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { blobValue: "foo" }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsTimestampSerialize"
        documentation: "Serializes union timestamp variant (epoch seconds on wire)"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "timestampValue": 1609459200
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { timestampValue: 1609459200 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsEnumSerialize"
        documentation: "Serializes union enum variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "enumValue": "Foo"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { enumValue: "Foo" }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsIntEnumSerialize"
        documentation: "Serializes union intEnum variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "intEnumValue": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { intEnumValue: 1 }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfScalars @httpResponseTests([
    {
        id: "AwsJson10UnionOfScalarsStringDeserialize"
        documentation: "Deserializes union string variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "stringValue": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { stringValue: "hello" }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsBooleanDeserialize"
        documentation: "Deserializes union boolean variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "booleanValue": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { booleanValue: true }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsByteDeserialize"
        documentation: "Deserializes union byte variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "byteValue": 9
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { byteValue: 9 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsShortDeserialize"
        documentation: "Deserializes union short variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "shortValue": 512
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { shortValue: 512 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsIntegerDeserialize"
        documentation: "Deserializes union integer variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "integerValue": 42
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { integerValue: 42 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsLongDeserialize"
        documentation: "Deserializes union long variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "longValue": 999999999999
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { longValue: 999999999999 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsFloatDeserialize"
        documentation: "Deserializes union float variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "floatValue": 1.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { floatValue: 1.5 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsDoubleDeserialize"
        documentation: "Deserializes union double variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "doubleValue": 2.5
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { doubleValue: 2.5 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsBlobDeserialize"
        documentation: "Deserializes union blob variant (base64 on wire)"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "blobValue": "Zm9v"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { blobValue: "foo" }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsTimestampDeserialize"
        documentation: "Deserializes union timestamp variant (epoch seconds on wire)"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "timestampValue": 1609459200
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { timestampValue: 1609459200 }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsEnumDeserialize"
        documentation: "Deserializes union enum variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "enumValue": "Foo"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { enumValue: "Foo" }
        }
    }
    {
        id: "AwsJson10UnionOfScalarsIntEnumDeserialize"
        documentation: "Deserializes union intEnum variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "intEnumValue": 1
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: { intEnumValue: 1 }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfStruct @httpRequestTests([
    {
        id: "AwsJson10UnionOfStructSerialize"
        documentation: "Serializes union struct variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfStruct" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                structValue: { stringMember: "hello", integerMember: 42, booleanMember: true }
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfStruct @httpResponseTests([
    {
        id: "AwsJson10UnionOfStructDeserialize"
        documentation: "Deserializes union struct variant"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                structValue: { stringMember: "hello", integerMember: 42, booleanMember: true }
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfList @httpRequestTests([
    {
        id: "AwsJson10UnionOfListSerialize"
        documentation: "Serializes union list variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "listValue": ["a", "b", "c"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfList" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                listValue: ["a", "b", "c"]
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfList @httpResponseTests([
    {
        id: "AwsJson10UnionOfListDeserialize"
        documentation: "Deserializes union list variant"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "value": {
                    "listValue": ["a", "b", "c"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                listValue: ["a", "b", "c"]
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfMap @httpRequestTests([
    {
        id: "AwsJson10UnionOfMapSerialize"
        documentation: "Serializes union map variant"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfMap" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                mapValue: { k1: "v1", k2: "v2" }
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfMap @httpResponseTests([
    {
        id: "AwsJson10UnionOfMapDeserialize"
        documentation: "Deserializes union map variant"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                mapValue: { k1: "v1", k2: "v2" }
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfUnion @httpRequestTests([
    {
        id: "AwsJson10UnionOfUnionSerialize"
        documentation: "Serializes union containing another union"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "unionValue": {
                        "stringValue": "nested"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.UnionOfUnion" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                unionValue: { stringValue: "nested" }
            }
        }
    }
])

apply smithy.protocoltests.corpus#UnionOfUnion @httpResponseTests([
    {
        id: "AwsJson10UnionOfUnionDeserialize"
        documentation: "Deserializes union containing another union"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                unionValue: { stringValue: "nested" }
            }
        }
    }
])

apply smithy.protocoltests.corpus#ListOfStructs @httpRequestTests([
    {
        id: "AwsJson10ListOfStructsSerialize"
        documentation: "Serializes a list of structs"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ListOfStructs" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#ListOfStructs @httpResponseTests([
    {
        id: "AwsJson10ListOfStructsDeserialize"
        documentation: "Deserializes a list of structs"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#ListOfMaps @httpRequestTests([
    {
        id: "AwsJson10ListOfMapsSerialize"
        documentation: "Serializes lists of maps for all scalar leaf types"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ListOfMaps" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#ListOfMaps @httpResponseTests([
    {
        id: "AwsJson10ListOfMapsDeserialize"
        documentation: "Deserializes lists of maps for all scalar leaf types"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#ListOfLists @httpRequestTests([
    {
        id: "AwsJson10ListOfListsSerialize"
        documentation: "Serializes lists of lists for all scalar leaf types"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ListOfLists" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#ListOfLists @httpResponseTests([
    {
        id: "AwsJson10ListOfListsDeserialize"
        documentation: "Deserializes lists of lists for all scalar leaf types"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#ListOfUnions @httpRequestTests([
    {
        id: "AwsJson10ListOfUnionsSerialize"
        documentation: "Serializes a list of unions with different variants"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": [
                    {"stringValue": "hello"},
                    {"integerValue": 42}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.ListOfUnions" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#ListOfUnions @httpResponseTests([
    {
        id: "AwsJson10ListOfUnionsDeserialize"
        documentation: "Deserializes a list of unions with different variants"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "values": [
                    {"stringValue": "hello"},
                    {"integerValue": 42}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#MapOfStructs @httpRequestTests([
    {
        id: "AwsJson10MapOfStructsSerialize"
        documentation: "Serializes a map of structs"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": {
                    "first": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "second": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.MapOfStructs" }
        requireHeaders: ["Content-Length"]
        params: {
            values: {
                first: { stringMember: "foo", integerMember: 1, booleanMember: true }
                second: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply smithy.protocoltests.corpus#MapOfStructs @httpResponseTests([
    {
        id: "AwsJson10MapOfStructsDeserialize"
        documentation: "Deserializes a map of structs"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "values": {
                    "first": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "second": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            values: {
                first: { stringMember: "foo", integerMember: 1, booleanMember: true }
                second: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply smithy.protocoltests.corpus#MapOfMaps @httpRequestTests([
    {
        id: "AwsJson10MapOfMapsSerialize"
        documentation: "Serializes maps of maps for all scalar leaf types"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.MapOfMaps" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#MapOfMaps @httpResponseTests([
    {
        id: "AwsJson10MapOfMapsDeserialize"
        documentation: "Deserializes maps of maps for all scalar leaf types"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#MapOfLists @httpRequestTests([
    {
        id: "AwsJson10MapOfListsSerialize"
        documentation: "Serializes maps of lists for all scalar leaf types"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.MapOfLists" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#MapOfLists @httpResponseTests([
    {
        id: "AwsJson10MapOfListsDeserialize"
        documentation: "Deserializes maps of lists for all scalar leaf types"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#MapOfUnions @httpRequestTests([
    {
        id: "AwsJson10MapOfUnionsSerialize"
        documentation: "Serializes a map of unions with different variants"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": {
                    "first": {"stringValue": "hello"},
                    "second": {"integerValue": 42}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.MapOfUnions" }
        requireHeaders: ["Content-Length"]
        params: {
            values: {
                first: { stringValue: "hello" }
                second: { integerValue: 42 }
            }
        }
    }
])

apply smithy.protocoltests.corpus#MapOfUnions @httpResponseTests([
    {
        id: "AwsJson10MapOfUnionsDeserialize"
        documentation: "Deserializes a map of unions with different variants"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "values": {
                    "first": {"stringValue": "hello"},
                    "second": {"integerValue": 42}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            values: {
                first: { stringValue: "hello" }
                second: { integerValue: 42 }
            }
        }
    }
])

apply smithy.protocoltests.corpus#SparseListOfStructs @httpRequestTests([
    {
        id: "AwsJson10SparseListOfStructsNullAtStart"
        documentation: "Serializes a sparse list with a null as the first element"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": [
                    null,
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseListOfStructs" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10SparseListOfStructsNullInMiddle"
        documentation: "Serializes a sparse list with a null as the middle element"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    null,
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseListOfStructs" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10SparseListOfStructsNullAtEnd"
        documentation: "Serializes a sparse list with a null as the last element"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": [
                    {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    {"stringMember": "bar", "integerMember": 2, "booleanMember": false},
                    null
                ]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseListOfStructs" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#SparseListOfStructs @httpResponseTests([
    {
        id: "AwsJson10SparseListOfStructsNullAtStartResponse"
        documentation: "Deserializes a sparse list with a null as the first element"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10SparseListOfStructsNullInMiddleResponse"
        documentation: "Deserializes a sparse list with a null as the middle element"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10SparseListOfStructsNullAtEndResponse"
        documentation: "Deserializes a sparse list with a null as the last element"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#SparseMapOfStructs @httpRequestTests([
    {
        id: "AwsJson10SparseMapOfStructsNullAtStart"
        documentation: "Serializes a sparse map with a null as the first entry"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": {
                    "a": null,
                    "b": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "c": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseMapOfStructs" }
        requireHeaders: ["Content-Length"]
        params: {
            values: {
                a: null
                b: { stringMember: "foo", integerMember: 1, booleanMember: true }
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "AwsJson10SparseMapOfStructsNullInMiddle"
        documentation: "Serializes a sparse map with a null as the middle entry"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": {
                    "a": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "b": null,
                    "c": {"stringMember": "bar", "integerMember": 2, "booleanMember": false}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseMapOfStructs" }
        requireHeaders: ["Content-Length"]
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: null
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "AwsJson10SparseMapOfStructsNullAtEnd"
        documentation: "Serializes a sparse map with a null as the last entry"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "values": {
                    "a": {"stringMember": "foo", "integerMember": 1, "booleanMember": true},
                    "b": {"stringMember": "bar", "integerMember": 2, "booleanMember": false},
                    "c": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.SparseMapOfStructs" }
        requireHeaders: ["Content-Length"]
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: { stringMember: "bar", integerMember: 2, booleanMember: false }
                c: null
            }
        }
    }
])

apply smithy.protocoltests.corpus#SparseMapOfStructs @httpResponseTests([
    {
        id: "AwsJson10SparseMapOfStructsNullAtStartResponse"
        documentation: "Deserializes a sparse map with a null as the first entry"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            values: {
                a: null
                b: { stringMember: "foo", integerMember: 1, booleanMember: true }
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "AwsJson10SparseMapOfStructsNullInMiddleResponse"
        documentation: "Deserializes a sparse map with a null as the middle entry"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: null
                c: { stringMember: "bar", integerMember: 2, booleanMember: false }
            }
        }
    }
    {
        id: "AwsJson10SparseMapOfStructsNullAtEndResponse"
        documentation: "Deserializes a sparse map with a null as the last entry"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            values: {
                a: { stringMember: "foo", integerMember: 1, booleanMember: true }
                b: { stringMember: "bar", integerMember: 2, booleanMember: false }
                c: null
            }
        }
    }
])

apply smithy.protocoltests.corpus#RecursiveStruct @httpRequestTests([
    {
        id: "AwsJson10RecursiveStructTwoLevelsDeep"
        documentation: "Serializes recursive struct two levels deep via recursiveMember"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.RecursiveStruct" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                stringMember: "level1"
                recursiveMember: { stringMember: "level2" }
            }
        }
    }
    {
        id: "AwsJson10RecursiveStructViaList"
        documentation: "Serializes recursive struct via list"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.RecursiveStruct" }
        requireHeaders: ["Content-Length"]
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
        id: "AwsJson10RecursiveStructViaMap"
        documentation: "Serializes recursive struct via map"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.RecursiveStruct" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#RecursiveStruct @httpResponseTests([
    {
        id: "AwsJson10RecursiveStructTwoLevelsDeepDeserialize"
        documentation: "Deserializes recursive struct two levels deep via recursiveMember"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                stringMember: "level1"
                recursiveMember: { stringMember: "level2" }
            }
        }
    }
    {
        id: "AwsJson10RecursiveStructViaListDeserialize"
        documentation: "Deserializes recursive struct via list"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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
        id: "AwsJson10RecursiveStructViaMapDeserialize"
        documentation: "Deserializes recursive struct via map"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
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

apply smithy.protocoltests.corpus#RecursiveUnion @httpRequestTests([
    {
        id: "AwsJson10RecursiveUnionDirectRecursion"
        documentation: "Serializes recursive union two levels deep"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "recursiveValue": {
                        "stringValue": "nested"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.RecursiveUnion" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                recursiveValue: { stringValue: "nested" }
            }
        }
    }
    {
        id: "AwsJson10RecursiveUnionThroughStruct"
        documentation: "Serializes recursive union through struct"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.RecursiveUnion" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                structValue: {
                    value: { stringValue: "deep" }
                }
            }
        }
    }
])

apply smithy.protocoltests.corpus#RecursiveUnion @httpResponseTests([
    {
        id: "AwsJson10RecursiveUnionDirectRecursionDeserialize"
        documentation: "Deserializes recursive union two levels deep"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                recursiveValue: { stringValue: "nested" }
            }
        }
    }
    {
        id: "AwsJson10RecursiveUnionThroughStructDeserialize"
        documentation: "Deserializes recursive union through struct"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            value: {
                structValue: {
                    value: { stringValue: "deep" }
                }
            }
        }
    }
])

apply smithy.protocoltests.corpus#EmptyInputOutput @httpRequestTests([
    {
        id: "AwsJson10EmptyInputOutputSerialize"
        documentation: "Serializes empty input as empty JSON object"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.EmptyInputOutput" }
        requireHeaders: ["Content-Length"]
        params: {}
    }
    {
        id: "AwsJson10EmptyInputOutputServerAllowsEmptyBody"
        tags: ["absent-request-body"]
        documentation: """
            Servers accept an empty body for an operation with an empty input
            structure, the request counterpart to clients accepting an empty
            response body"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: ""
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.EmptyInputOutput" }
        appliesTo: "server"
        params: {}
    }
])

apply smithy.protocoltests.corpus#EmptyInputOutput @httpResponseTests([
    {
        id: "AwsJson10EmptyInputOutputDeserialize"
        documentation: "Deserializes empty JSON object body"
        protocol: awsJson1_0
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {}
    }
    {
        id: "AwsJson10EmptyInputOutputDeserializeEmptyBody"
        tags: ["absent-response-body"]
        documentation: "Deserializes empty string body as valid empty output"
        protocol: awsJson1_0
        code: 200
        body: ""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        appliesTo: "client"
        params: {}
    }
])

apply smithy.protocoltests.corpus#NoInputOutput @httpRequestTests([
    {
        id: "AwsJson10NoInputOutputSerialize"
        documentation: """
            Sends an empty JSON object for an operation whose input targets the
            Unit shape"""
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10CorpusTests.NoInputOutput" }
        requireHeaders: ["Content-Length"]
        params: {}
    }
])

apply smithy.protocoltests.corpus#NoInputOutput @httpResponseTests([
    {
        id: "AwsJson10NoInputOutputDeserialize"
        documentation: """
            Accepts an empty body for an operation whose output targets the Unit
            shape, which still carries the protocol Content-Type"""
        protocol: awsJson1_0
        code: 200
        body: ""
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {}
    }
    {
        id: "AwsJson10NoInputOutputDeserializeEmptyObject"
        documentation: """
            Accepts an empty JSON object for an operation whose output targets
            the Unit shape"""
        protocol: awsJson1_0
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {}
        appliesTo: "client"
    }
])

apply smithy.protocoltests.corpus#SimpleError @httpResponseTests([
    {
        id: "AwsJson10SimpleErrorDeserialize"
        documentation: "Deserializes simple client error with __type discrimination"
        protocol: awsJson1_0
        code: 400
        body: """
            {
                "__type": "smithy.protocoltests.corpus#SimpleError",
                "message": "oops"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "oops" }
    }
])

apply smithy.protocoltests.corpus#ComplexError @httpResponseTests([
    {
        id: "AwsJson10ComplexErrorDeserialize"
        documentation: "Deserializes complex server error with nested struct"
        protocol: awsJson1_0
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
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: {
            message: "something went wrong"
            code: 42
            nested: { stringMember: "nestedValue", integerMember: 99 }
        }
    }
])
