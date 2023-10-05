$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply OperationWithDefaults @httpRequestTests([
    {
        id: "AwsJson10ClientPopulatesDefaultValuesInInput"
        documentation: "Client populates default values in input."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: awsJson1_0
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: """
            {
                "defaults": {
                    "defaultString": "hi",
                    "defaultBoolean": true,
                    "defaultList": [],
                    "defaultDocumentMap": {},
                    "defaultDocumentString": "hi",
                    "defaultDocumentBoolean": true,
                    "defaultDocumentList": [],
                    "defaultTimestamp": 0,
                    "defaultBlob": "YWJj",
                    "defaultByte": 1,
                    "defaultShort": 1,
                    "defaultInteger": 10,
                    "defaultLong": 100,
                    "defaultFloat": 1.0,
                    "defaultDouble": 1.0,
                    "defaultMap": {},
                    "defaultEnum": "FOO",
                    "defaultIntEnum": 1
                }
            }"""
        params: {
            defaults: {}
        }
    }
    {
        id: "AwsJson10ClientSkipsTopLevelDefaultValuesInInput"
        documentation: "Client skips top level default values in input."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: awsJson1_0
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: """
            {
            }"""
        params: {
        }
    }
    {
        id: "AwsJson10ClientUsesExplicitlyProvidedMemberValuesOverDefaults"
        documentation: "Client uses explicitly provided member values over defaults"
        appliesTo: "client"
        tags: ["defaults"]
        protocol: awsJson1_0
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        params: {
            defaults: {
                defaultString: "bye"
                defaultBoolean: true
                defaultList: ["a"]
                defaultDocumentMap: {name: "Jack"}
                defaultDocumentString: "bye"
                defaultDocumentBoolean: true
                defaultDocumentList: ["b"]
                defaultNullDocument: "notNull"
                defaultTimestamp: 1
                defaultBlob: "hi"
                defaultByte: 2
                defaultShort: 2
                defaultInteger: 20
                defaultLong: 200
                defaultFloat: 2.0
                defaultDouble: 2.0
                defaultMap: {name: "Jack"}
                defaultEnum: "BAR"
                defaultIntEnum: 2
            }
        }
        body: """
            {
                "defaults": {
                    "defaultString": "bye",
                    "defaultBoolean": true,
                    "defaultList": ["a"],
                    "defaultDocumentMap": {"name": "Jack"},
                    "defaultDocumentString": "bye",
                    "defaultDocumentBoolean": true,
                    "defaultDocumentList": ["b"],
                    "defaultNullDocument": "notNull",
                    "defaultTimestamp": 1,
                    "defaultBlob": "aGk=",
                    "defaultByte": 2,
                    "defaultShort": 2,
                    "defaultInteger": 20,
                    "defaultLong": 200,
                    "defaultFloat": 2.0,
                    "defaultDouble": 2.0,
                    "defaultMap": {"name": "Jack"},
                    "defaultEnum": "BAR",
                    "defaultIntEnum": 2
                }
            }"""
    }
    {
        id: "AwsJson10ServerPopulatesDefaultsWhenMissingInRequestBody"
        documentation: "Server populates default values when missing in request body."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: awsJson1_0
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: """
            {
            "defaults": {}
            }"""
        params: {
            defaults: {
                defaultString: "hi"
                defaultBoolean: true
                defaultList: []
                defaultDocumentMap: {}
                defaultDocumentString: "hi"
                defaultDocumentBoolean: true
                defaultDocumentList: []
                defaultTimestamp: 0
                defaultBlob: "abc"
                defaultByte: 1
                defaultShort: 1
                defaultInteger: 10
                defaultLong: 100
                defaultFloat: 1.0
                defaultDouble: 1.0
                defaultMap: {}
                defaultEnum: "FOO"
                defaultIntEnum: 1
            },
            topLevelDefault: "hi"
        }
    }
])

apply OperationWithDefaults @httpResponseTests([
    {
        id: "AwsJson10ClientPopulatesDefaultsValuesWhenMissingInResponse"
        documentation: "Client populates default values when missing in response."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: awsJson1_0
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: "{}"
        params: {
            defaultString: "hi"
            defaultBoolean: true
            defaultList: []
            defaultDocumentMap: {}
            defaultDocumentString: "hi"
            defaultDocumentBoolean: true
            defaultDocumentList: []
            defaultTimestamp: 0
            defaultBlob: "abc"
            defaultByte: 1
            defaultShort: 1
            defaultInteger: 10
            defaultLong: 100
            defaultFloat: 1.0
            defaultDouble: 1.0
            defaultMap: {}
            defaultEnum: "FOO"
            defaultIntEnum: 1
        }
    }
    {
        id: "AwsJson10ClientIgnoresDefaultValuesIfMemberValuesArePresentInResponse"
        documentation: "Client ignores default values if member values are present in the response."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: awsJson1_0
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: """
            {
                "defaultString": "bye",
                "defaultBoolean": false,
                "defaultList": ["a"],
                "defaultDocumentMap": {"name": "Jack"},
                "defaultDocumentString": "bye",
                "defaultDocumentBoolean": false,
                "defaultDocumentList": ["b"],
                "defaultNullDocument": "notNull",
                "defaultTimestamp": 2,
                "defaultBlob": "aGk=",
                "defaultByte": 2,
                "defaultShort": 2,
                "defaultInteger": 20,
                "defaultLong": 200,
                "defaultFloat": 2.0,
                "defaultDouble": 2.0,
                "defaultMap": {"name": "Jack"},
                "defaultEnum": "BAR",
                "defaultIntEnum": 2
            }"""
        params: {
            defaultString: "bye"
            defaultBoolean: false
            defaultList: ["a"]
            defaultDocumentMap: {name: "Jack"}
            defaultDocumentString: "bye"
            defaultDocumentBoolean: false
            defaultDocumentList: ["b"]
            defaultNullDocument: "notNull"
            defaultTimestamp: 2
            defaultBlob: "hi"
            defaultByte: 2
            defaultShort: 2
            defaultInteger: 20
            defaultLong: 200
            defaultFloat: 2.0
            defaultDouble: 2.0
            defaultMap: {name: "Jack"}
            defaultEnum: "BAR"
            defaultIntEnum: 2
        }
    }
    {
        id: "AwsJson10ServerPopulatesDefaultsInResponseWhenMissingInParams"
        documentation: "Server populates default values in response when missing in params."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: awsJson1_0
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: """
            {
                "defaultString": "hi",
                "defaultBoolean": true,
                "defaultList": [],
                "defaultDocumentMap": {},
                "defaultDocumentString": "hi",
                "defaultDocumentBoolean": true,
                "defaultDocumentList": [],
                "defaultTimestamp": 0,
                "defaultBlob": "YWJj",
                "defaultByte": 1,
                "defaultShort": 1,
                "defaultInteger": 10,
                "defaultLong": 100,
                "defaultFloat": 1.0,
                "defaultDouble": 1.0,
                "defaultMap": {},
                "defaultEnum": "FOO",
                "defaultIntEnum": 1
            }"""
        params: {}
    }
])

operation OperationWithDefaults {
    input := {
        defaults: Defaults

        topLevelDefault: String = "hi" // Client should ignore default values in input shape
    }

    output := with [DefaultsMixin] {}
}

structure Defaults with [DefaultsMixin] {}

@mixin
structure DefaultsMixin {
    defaultString: String = "hi"
    defaultBoolean: Boolean = true
    defaultList: TestStringList = []
    defaultDocumentMap: Document = {}
    defaultDocumentString: Document = "hi"
    defaultDocumentBoolean: Document = true
    defaultDocumentList: Document = []
    defaultNullDocument: Document = null
    defaultTimestamp: Timestamp = 0
    defaultBlob: Blob = "abc"
    defaultByte: Byte = 1
    defaultShort: Short = 1
    defaultInteger: Integer = 10
    defaultLong: Long = 100
    defaultFloat: Float = 1.0
    defaultDouble: Double = 1.0
    defaultMap: TestStringMap = {}
    defaultEnum: TestEnum = "FOO"
    defaultIntEnum: TestIntEnum = 1
}

list TestStringList {
    member: String
}

map TestStringMap {
    key: String
    value: String
}

enum TestEnum {
    FOO
    BAR
    BAZ
}

intEnum TestIntEnum {
    ONE = 1
    TWO = 2
}
