$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply smithy.protocoltests.corpus#DefaultScalars @httpRequestTests([
    {
        id: "AwsJson11DefaultScalarsOmitsDefaults"
        tags: ["top-level-default-omission"]
        documentation: """
            Client does not synthesize top-level input defaults. The members are
            absent from params, so nothing is serialized: an explicitly provided
            value is always sent, even when it equals the default, so this case
            asserts the client does not fill them in on the caller's behalf."""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultScalars" }
        requireHeaders: ["Content-Length"]
        appliesTo: "client"
        params: {}
    }
    {
        id: "AwsJson11DefaultScalarsSerializeNonDefaults"
        tags: ["explicit-over-default"]
        documentation: "Serializes members when explicitly set to non-default values"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultBoolean": false,
                "defaultByte": 5,
                "defaultShort": 10,
                "defaultInteger": 42,
                "defaultLong": 100,
                "defaultFloat": 1.5,
                "defaultDouble": 2.5,
                "defaultString": "custom",
                "defaultBlob": "aGVsbG8=",
                "defaultEnum": "Bar",
                "defaultIntEnum": 2,
                "zeroBoolean": true,
                "zeroByte": 1,
                "zeroShort": 1,
                "zeroInteger": 1,
                "zeroLong": 1,
                "zeroFloat": 3.25,
                "zeroDouble": 4.75,
                "emptyString": "not empty",
                "emptyBlob": "YWJj"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            defaultBoolean: false
            defaultByte: 5
            defaultShort: 10
            defaultInteger: 42
            defaultLong: 100
            defaultFloat: 1.5
            defaultDouble: 2.5
            defaultString: "custom"
            defaultBlob: "hello"
            defaultEnum: "Bar"
            defaultIntEnum: 2
            zeroBoolean: true
            zeroByte: 1
            zeroShort: 1
            zeroInteger: 1
            zeroLong: 1
            zeroFloat: 3.25
            zeroDouble: 4.75
            emptyString: "not empty"
            emptyBlob: "abc"
        }
    }
    {
        id: "AwsJson11DefaultScalarsSerializeExplicitDefaults"
        tags: ["explicit-over-default"]
        documentation: """
            Serializes members that are explicitly set to their default value.
            An explicitly provided value is always sent, even when it equals the
            default, including the zero values"""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultBoolean": true,
                "defaultByte": 7,
                "defaultShort": 8,
                "defaultInteger": 9,
                "defaultLong": 10,
                "defaultFloat": 1.25,
                "defaultDouble": 2.25,
                "defaultString": "default",
                "defaultBlob": "ZGVmYXVsdA==",
                "defaultEnum": "Foo",
                "defaultIntEnum": 1,
                "zeroBoolean": false,
                "zeroByte": 0,
                "zeroShort": 0,
                "zeroInteger": 0,
                "zeroLong": 0,
                "zeroFloat": 0,
                "zeroDouble": 0,
                "emptyString": "",
                "emptyBlob": ""
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultScalars" }
        requireHeaders: ["Content-Length"]
        params: {
            defaultBoolean: true
            defaultByte: 7
            defaultShort: 8
            defaultInteger: 9
            defaultLong: 10
            defaultFloat: 1.25
            defaultDouble: 2.25
            defaultString: "default"
            defaultBlob: "default"
            defaultEnum: "Foo"
            defaultIntEnum: 1
            zeroBoolean: false
            zeroByte: 0
            zeroShort: 0
            zeroInteger: 0
            zeroLong: 0
            zeroFloat: 0
            zeroDouble: 0
            emptyString: ""
            emptyBlob: ""
        }
    }
])

apply smithy.protocoltests.corpus#DefaultScalars @httpResponseTests([
    {
        id: "AwsJson11DefaultScalarsPopulatesDefaultsOnDeserialize"
        documentation: "Client fills default values when members are absent in response"
        protocol: awsJson1_1
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {
            defaultBoolean: true
            defaultByte: 7
            defaultShort: 8
            defaultInteger: 9
            defaultLong: 10
            defaultFloat: 1.25
            defaultDouble: 2.25
            defaultString: "default"
            defaultBlob: "default"
            defaultEnum: "Foo"
            defaultIntEnum: 1
            zeroBoolean: false
            zeroByte: 0
            zeroShort: 0
            zeroInteger: 0
            zeroLong: 0
            zeroFloat: 0
            zeroDouble: 0
            emptyString: ""
            emptyBlob: ""
        }
    }
])

apply smithy.protocoltests.corpus#DefaultCollections @httpRequestTests([
    {
        id: "AwsJson11DefaultCollectionsOmitsEmptyDefaults"
        tags: ["top-level-default-omission"]
        documentation: """
            Client does not synthesize the top-level empty list/map defaults. The
            members are absent from params rather than explicitly set to [] and
            {}, which would have to be serialized."""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultCollections" }
        requireHeaders: ["Content-Length"]
        appliesTo: "client"
        params: {}
    }
    {
        id: "AwsJson11DefaultCollectionsSerializeNonEmpty"
        tags: ["explicit-over-default"]
        documentation: "Serializes non-empty list and map"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultList": ["a", "b"],
                "defaultMap": {"key1": "value1", "key2": "value2"}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultCollections" }
        requireHeaders: ["Content-Length"]
        params: {
            defaultList: ["a", "b"]
            defaultMap: { key1: "value1", key2: "value2" }
        }
    }
    {
        id: "AwsJson11DefaultCollectionsSerializeExplicitEmpty"
        tags: ["explicit-over-default"]
        documentation: """
            Serializes an explicitly provided empty list and map. An explicitly
            provided value is always sent, even when it equals the default"""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultList": [],
                "defaultMap": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultCollections" }
        requireHeaders: ["Content-Length"]
        params: {
            defaultList: []
            defaultMap: {}
        }
    }
])

apply smithy.protocoltests.corpus#NestedDefaults @httpRequestTests([
    {
        id: "AwsJson11NestedDefaultsSerialize"
        documentation: "Serializes nested structs with defaults populated"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "topLevel": {
                    "nested": {
                        "greeting": "hello",
                        "count": 0,
                        "inner": {
                            "farewell": "goodbye"
                        }
                    },
                    "nestedList": [
                        {
                            "greeting": "hi",
                            "count": 5,
                            "inner": {
                                "farewell": "bye"
                            }
                        },
                        {
                            "greeting": "yo",
                            "count": 6,
                            "inner": {
                                "farewell": "later"
                            }
                        }
                    ],
                    "nestedMap": {
                        "entry1": {
                            "greeting": "hey",
                            "count": 10,
                            "inner": {
                                "farewell": "ciao"
                            }
                        },
                        "entry2": {
                            "greeting": "sup",
                            "count": 11,
                            "inner": {
                                "farewell": "adios"
                            }
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NestedDefaults" }
        requireHeaders: ["Content-Length"]
        params: {
            topLevel: {
                nested: {
                    greeting: "hello"
                    count: 0
                    inner: { farewell: "goodbye" }
                }
                nestedList: [
                    {
                        greeting: "hi"
                        count: 5
                        inner: { farewell: "bye" }
                    }
                    {
                        greeting: "yo"
                        count: 6
                        inner: { farewell: "later" }
                    }
                ]
                nestedMap: {
                    entry1: {
                        greeting: "hey"
                        count: 10
                        inner: { farewell: "ciao" }
                    }
                    entry2: {
                        greeting: "sup"
                        count: 11
                        inner: { farewell: "adios" }
                    }
                }
            }
        }
    }
])

apply smithy.protocoltests.corpus#NestedDefaults @httpResponseTests([
    {
        id: "AwsJson11NestedDefaultsDeserializePopulatesDefaults"
        documentation: "Client fills defaults for absent nested fields"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "topLevel": {
                    "nested": {},
                    "nestedList": [{}, {}],
                    "nestedMap": {
                        "entry1": {},
                        "entry2": {}
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {
            topLevel: {
                nested: { greeting: "hello", count: 0 }
                nestedList: [
                    {
                        greeting: "hello"
                        count: 0
                    }
                    {
                        greeting: "hello"
                        count: 0
                    }
                ]
                nestedMap: {
                    entry1: { greeting: "hello", count: 0 }
                    entry2: { greeting: "hello", count: 0 }
                }
            }
        }
    }
])

apply smithy.protocoltests.corpus#RequiredMembers @httpRequestTests([
    {
        id: "AwsJson11RequiredMembersSerialize"
        documentation: "Serializes explicitly provided values for all required members"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "requiredString": "hello",
                "requiredInteger": 42,
                "requiredBoolean": true,
                "requiredList": ["a", "b"],
                "requiredMap": {"key1": "value1", "key2": "value2"},
                "requiredStringWithDefault": "custom",
                "requiredIntegerWithDefault": 5,
                "requiredBooleanWithDefault": true,
                "requiredListWithDefault": ["c", "d"],
                "requiredMapWithDefault": {"k1": "v1", "k2": "v2"}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.RequiredMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            requiredString: "hello"
            requiredInteger: 42
            requiredBoolean: true
            requiredList: ["a", "b"]
            requiredMap: { key1: "value1", key2: "value2" }
            requiredStringWithDefault: "custom"
            requiredIntegerWithDefault: 5
            requiredBooleanWithDefault: true
            requiredListWithDefault: ["c", "d"]
            requiredMapWithDefault: { k1: "v1", k2: "v2" }
        }
    }
])

apply smithy.protocoltests.corpus#RequiredMembers @httpResponseTests([
    {
        id: "AwsJson11RequiredMembersDeserializeZeroValues"
        tags: ["error-correction"]
        documentation: "Client fills zero-values for required fields without defaults when server omits them"
        protocol: awsJson1_1
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {
            requiredString: ""
            requiredInteger: 0
            requiredBoolean: false
            requiredList: []
            requiredMap: {}
            requiredStringWithDefault: "default"
            requiredIntegerWithDefault: 0
            requiredBooleanWithDefault: false
            requiredListWithDefault: []
            requiredMapWithDefault: {}
        }
    }
    {
        id: "AwsJson11RequiredMembersDeserializeDefaults"
        tags: ["error-correction"]
        documentation: "Client fills defaults for required fields with @default when server omits them"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "requiredString": "provided",
                "requiredInteger": 1,
                "requiredBoolean": true,
                "requiredList": ["x", "y"],
                "requiredMap": {"a": "b", "c": "d"}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {
            requiredString: "provided"
            requiredInteger: 1
            requiredBoolean: true
            requiredList: ["x", "y"]
            requiredMap: { a: "b", c: "d" }
            requiredStringWithDefault: "default"
            requiredIntegerWithDefault: 0
            requiredBooleanWithDefault: false
            requiredListWithDefault: []
            requiredMapWithDefault: {}
        }
    }
])

apply smithy.protocoltests.corpus#NullSparseMembers @httpRequestTests([
    {
        id: "AwsJson11NullSparseMembersSerialize"
        documentation: "Null values in sparse collections are serialized as JSON null"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "sparseStringList": [null, "hello", null, "world", null],
                "sparseStringMap": {"key1": null, "key2": "value", "key3": "value2"},
                "sparseStructList": [null, {"stringMember": "a", "integerMember": 1, "booleanMember": true}, null, {"stringMember": "b", "integerMember": 2, "booleanMember": false}],
                "sparseStructMap": {"key1": null, "key2": {"stringMember": "a", "integerMember": 1, "booleanMember": true}, "key3": {"stringMember": "b", "integerMember": 2, "booleanMember": false}}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.NullSparseMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            sparseStringList: [null, "hello", null, "world", null]
            sparseStringMap: { key1: null, key2: "value", key3: "value2" }
            sparseStructList: [
                null
                {
                    stringMember: "a"
                    integerMember: 1
                    booleanMember: true
                }
                null
                {
                    stringMember: "b"
                    integerMember: 2
                    booleanMember: false
                }
            ]
            sparseStructMap: {
                key1: null
                key2: { stringMember: "a", integerMember: 1, booleanMember: true }
                key3: { stringMember: "b", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply smithy.protocoltests.corpus#NullSparseMembers @httpResponseTests([
    {
        id: "AwsJson11NullSparseMembersDeserialize"
        documentation: "Null values in sparse collections are preserved on deserialization"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "sparseStringList": [null, "hello", null, "world", null],
                "sparseStringMap": {"key1": null, "key2": "value", "key3": "value2"},
                "sparseStructList": [null, {"stringMember": "a", "integerMember": 1, "booleanMember": true}, null, {"stringMember": "b", "integerMember": 2, "booleanMember": false}],
                "sparseStructMap": {"key1": null, "key2": {"stringMember": "a", "integerMember": 1, "booleanMember": true}, "key3": {"stringMember": "b", "integerMember": 2, "booleanMember": false}}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            sparseStringList: [null, "hello", null, "world", null]
            sparseStringMap: { key1: null, key2: "value", key3: "value2" }
            sparseStructList: [
                null
                {
                    stringMember: "a"
                    integerMember: 1
                    booleanMember: true
                }
                null
                {
                    stringMember: "b"
                    integerMember: 2
                    booleanMember: false
                }
            ]
            sparseStructMap: {
                key1: null
                key2: { stringMember: "a", integerMember: 1, booleanMember: true }
                key3: { stringMember: "b", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply smithy.protocoltests.corpus#ClientOptionalDefaults @httpRequestTests([
    {
        id: "AwsJson11ClientOptionalDefaultsNotPopulated"
        tags: ["client-optional-default"]
        documentation: "Client does not populate defaults for @clientOptional members"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.ClientOptionalDefaults" }
        requireHeaders: ["Content-Length"]
        appliesTo: "client"
        params: {}
    }
])

apply smithy.protocoltests.corpus#DefaultTimestamps @httpRequestTests([
    {
        id: "AwsJson11DefaultTimestampsOmitsDefaults"
        tags: ["top-level-default-omission"]
        documentation: """
            Client does not synthesize top-level input defaults, so nothing is serialized"""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultTimestamps" }
        requireHeaders: ["Content-Length"]
        appliesTo: "client"
        params: {}
    }
    {
        id: "AwsJson11DefaultTimestampsSerializeNonDefaults"
        tags: ["explicit-over-default"]
        documentation: "Serializes timestamp members when explicitly set to non-default values"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultTimestamp": 1609504496,
                "zeroTimestamp": 1614834367
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultTimestamps" }
        requireHeaders: ["Content-Length"]
        params: { defaultTimestamp: 1609504496, zeroTimestamp: 1614834367 }
    }
    {
        id: "AwsJson11DefaultTimestampsSerializeExplicitDefaults"
        tags: ["explicit-over-default"]
        documentation: """
            Serializes timestamp members explicitly set to their default values, including the zero timestamp"""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultTimestamp": 1609502096,
                "zeroTimestamp": 0
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DefaultTimestamps" }
        requireHeaders: ["Content-Length"]
        params: { defaultTimestamp: 1609502096, zeroTimestamp: 0 }
    }
])

apply smithy.protocoltests.corpus#DefaultTimestamps @httpResponseTests([
    {
        id: "AwsJson11DefaultTimestampsPopulatesDefaultsOnDeserialize"
        documentation: """
            Client fills default timestamp values when members are absent in response"""
        protocol: awsJson1_1
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: { defaultTimestamp: 1609502096, zeroTimestamp: 0 }
    }
])

apply smithy.protocoltests.corpus#DefaultCollections @httpResponseTests([
    {
        id: "AwsJson11DefaultCollectionsPopulatesEmptyDefaultsOnDeserialize"
        documentation: """
            Client fills the empty list and map defaults when the members are absent in response"""
        protocol: awsJson1_1
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {
            defaultList: []
            defaultMap: {}
        }
    }
    {
        id: "AwsJson11DefaultCollectionsDeserializeNonEmpty"
        tags: ["explicit-over-default"]
        documentation: "Deserializes non-empty list and map"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "defaultList": ["a", "b"],
                "defaultMap": {"key1": "value1", "key2": "value2"}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            defaultList: ["a", "b"]
            defaultMap: { key1: "value1", key2: "value2" }
        }
    }
    {
        id: "AwsJson11DefaultCollectionsDeserializeExplicitEmpty"
        tags: ["explicit-over-default"]
        documentation: """
            Deserializes an explicitly empty list and map present in the response, which is not the same as the members being absent"""
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "defaultList": [],
                "defaultMap": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            defaultList: []
            defaultMap: {}
        }
    }
])

apply smithy.protocoltests.corpus#ClientOptionalDefaults @httpResponseTests([
    {
        id: "AwsJson11ClientOptionalDefaultsNotPopulatedOnDeserialize"
        tags: ["client-optional-default"]
        documentation: """
            Client does not populate defaults for @clientOptional members that are absent in response"""
        protocol: awsJson1_1
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {}
    }
])
