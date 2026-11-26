$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply smithy.protocoltests.corpus#NamedScalarMembers @httpRequestTests([
    {
        id: "AwsJson11NamedScalarMembersSerialize"
        documentation: """
            awsJson1_1 ignores @jsonName and @xmlName, so every member
            serializes under its own name even though both traits are present
            on all of them"""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedScalarMembers" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#NamedScalarMembers @httpResponseTests([
    {
        id: "AwsJson11NamedScalarMembersDeserialize"
        documentation: """
            awsJson1_1 ignores @jsonName and @xmlName, so every member
            deserializes from its own name even though both traits are present
            on all of them"""
        protocol: awsJson1_1
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
        headers: { "Content-Type": "application/x-amz-json-1.1" }
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

apply smithy.protocoltests.corpus#NamedStructOfScalars @httpRequestTests([
    {
        id: "AwsJson11NamedStructOfScalarsSerialize"
        documentation: "Serializes a nested structure whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedStructOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            nested: { stringMember: "nestedNamed", integerMember: 91, booleanMember: true, timestampMember: 1609588496 }
        }
    }
])

apply smithy.protocoltests.corpus#NamedStructOfScalars @httpResponseTests([
    {
        id: "AwsJson11NamedStructOfScalarsDeserialize"
        documentation: "Deserializes a nested structure whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
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
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            nested: { stringMember: "nestedNamed", integerMember: 91, booleanMember: true, timestampMember: 1609588496 }
        }
    }
])

apply smithy.protocoltests.corpus#NamedListOfScalars @httpRequestTests([
    {
        id: "AwsJson11NamedListOfScalarsSerialize"
        documentation: "Serializes lists under their member names, ignoring @jsonName and @xmlName"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "strings": ["alpha", "beta"],
                "integers": [11, 22],
                "timestamps": [1609502096, 1609588496]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedListOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            strings: ["alpha", "beta"]
            integers: [11, 22]
            timestamps: [1609502096, 1609588496]
        }
    }
])

apply smithy.protocoltests.corpus#NamedListOfScalars @httpResponseTests([
    {
        id: "AwsJson11NamedListOfScalarsDeserialize"
        documentation: "Deserializes lists under their member names, ignoring @jsonName and @xmlName"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "strings": ["alpha", "beta"],
                "integers": [11, 22],
                "timestamps": [1609502096, 1609588496]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            strings: ["alpha", "beta"]
            integers: [11, 22]
            timestamps: [1609502096, 1609588496]
        }
    }
])

apply smithy.protocoltests.corpus#NamedMapOfScalars @httpRequestTests([
    {
        id: "AwsJson11NamedMapOfScalarsSerialize"
        documentation: "Serializes maps under their member names, ignoring @jsonName and @xmlName"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedMapOfScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            strings: { stringKeyOne: "gamma", stringKeyTwo: "delta" }
            integers: { integerKeyOne: 33, integerKeyTwo: 44 }
            timestamps: { timestampKeyOne: 1609674896, timestampKeyTwo: 1609761296 }
        }
    }
])

apply smithy.protocoltests.corpus#NamedMapOfScalars @httpResponseTests([
    {
        id: "AwsJson11NamedMapOfScalarsDeserialize"
        documentation: "Deserializes maps under their member names, ignoring @jsonName and @xmlName"
        protocol: awsJson1_1
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
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            strings: { stringKeyOne: "gamma", stringKeyTwo: "delta" }
            integers: { integerKeyOne: 33, integerKeyTwo: 44 }
            timestamps: { timestampKeyOne: 1609674896, timestampKeyTwo: 1609761296 }
        }
    }
])

apply smithy.protocoltests.corpus#NamedUnionMembers @httpRequestTests([
    {
        id: "AwsJson11NamedUnionStringSerialize"
        documentation: "Serializes the string variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "stringMember": "unionString"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedUnionMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { stringMember: "unionString" }
        }
    }
    {
        id: "AwsJson11NamedUnionIntegerSerialize"
        documentation: "Serializes the integer variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "integerMember": 55
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedUnionMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { integerMember: 55 }
        }
    }
    {
        id: "AwsJson11NamedUnionBooleanSerialize"
        documentation: "Serializes the boolean variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "booleanMember": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedUnionMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            value: { booleanMember: true }
        }
    }
    {
        id: "AwsJson11NamedUnionListSerialize"
        documentation: "Serializes the list variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "listMember": ["epsilon", "zeta"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedUnionMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                listMember: ["epsilon", "zeta"]
            }
        }
    }
    {
        id: "AwsJson11NamedUnionMapSerialize"
        documentation: "Serializes the map variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedUnionMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                mapMember: { mapKeyOne: "eta", mapKeyTwo: "theta" }
            }
        }
    }
    {
        id: "AwsJson11NamedUnionStructSerialize"
        documentation: "Serializes the structure variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NamedUnionMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                structMember: { stringMember: "unionNested", integerMember: 63, booleanMember: true, timestampMember: 1609674896 }
            }
        }
    }
])

apply smithy.protocoltests.corpus#NamedUnionMembers @httpResponseTests([
    {
        id: "AwsJson11NamedUnionStringDeserialize"
        documentation: "Deserializes the string variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "value": {
                    "stringMember": "unionString"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: { stringMember: "unionString" }
        }
    }
    {
        id: "AwsJson11NamedUnionIntegerDeserialize"
        documentation: "Deserializes the integer variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "value": {
                    "integerMember": 55
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: { integerMember: 55 }
        }
    }
    {
        id: "AwsJson11NamedUnionBooleanDeserialize"
        documentation: "Deserializes the boolean variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "value": {
                    "booleanMember": true
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: { booleanMember: true }
        }
    }
    {
        id: "AwsJson11NamedUnionListDeserialize"
        documentation: "Deserializes the list variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "value": {
                    "listMember": ["epsilon", "zeta"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: {
                listMember: ["epsilon", "zeta"]
            }
        }
    }
    {
        id: "AwsJson11NamedUnionMapDeserialize"
        documentation: "Deserializes the map variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
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
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: {
                mapMember: { mapKeyOne: "eta", mapKeyTwo: "theta" }
            }
        }
    }
    {
        id: "AwsJson11NamedUnionStructDeserialize"
        documentation: "Deserializes the structure variant of a union whose members carry naming traits, which awsJson1_1 ignores"
        protocol: awsJson1_1
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
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: {
                structMember: { stringMember: "unionNested", integerMember: 63, booleanMember: true, timestampMember: 1609674896 }
            }
        }
    }
])
