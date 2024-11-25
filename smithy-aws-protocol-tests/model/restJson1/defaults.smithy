$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply OperationWithDefaults @httpRequestTests([
    {
        id: "RestJsonClientPopulatesDefaultValuesInInput"
        documentation: "Client populates default values in input."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithDefaults"
        headers: {"Content-Type": "application/json"}
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
                    "defaultIntEnum": 1,
                    "emptyString": "",
                    "falseBoolean": false,
                    "emptyBlob": "",
                    "zeroByte": 0,
                    "zeroShort": 0,
                    "zeroInteger": 0,
                    "zeroLong": 0,
                    "zeroFloat": 0.0,
                    "zeroDouble": 0.0
                }
            }"""
        params: {
            defaults: {}
        }
    }
    {
        id: "RestJsonClientSkipsTopLevelDefaultValuesInInput"
        documentation: "Client skips top level default values in input."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithDefaults"
        headers: {"Content-Type": "application/json"}
        body: """
            {
            }"""
        params: {
        }
    }
    {
        id: "RestJsonClientUsesExplicitlyProvidedMemberValuesOverDefaults"
        documentation: "Client uses explicitly provided member values over defaults"
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithDefaults"
        headers: {"Content-Type": "application/json"}
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
                emptyString: "foo"
                falseBoolean: true
                emptyBlob: "hi"
                zeroByte: 1
                zeroShort: 1
                zeroInteger: 1
                zeroLong: 1
                zeroFloat: 1.0
                zeroDouble: 1.0
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
                    "defaultIntEnum": 2,
                    "emptyString": "foo",
                    "falseBoolean": true,
                    "emptyBlob": "aGk=",
                    "zeroByte": 1,
                    "zeroShort": 1,
                    "zeroInteger": 1,
                    "zeroLong": 1,
                    "zeroFloat": 1.0,
                    "zeroDouble": 1.0
                }
            }"""
    }
    {
        id: "RestJsonServerPopulatesDefaultsWhenMissingInRequestBody"
        documentation: "Server populates default values when missing in request body."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithDefaults"
        headers: {"Content-Type": "application/json"}
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
                emptyString: ""
                falseBoolean: false
                emptyBlob: ""
                zeroByte: 0
                zeroShort: 0
                zeroInteger: 0
                zeroLong: 0
                zeroFloat: 0.0
                zeroDouble: 0.0
            },
            topLevelDefault: "hi"
            otherTopLevelDefault: 0
        }
    }
    {
        id: "RestJsonClientUsesExplicitlyProvidedValuesInTopLevel"
        documentation: "Any time a value is provided for a member in the top level of input, it is used, regardless of if its the default."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithDefaults"
        headers: {"Content-Type": "application/json"}
        body: """
            {
                "topLevelDefault": "hi",
                "otherTopLevelDefault": 0
            }"""
        params: {
            topLevelDefault: "hi"
            otherTopLevelDefault: 0
        }
    }
    {
        id: "RestJsonClientIgnoresNonTopLevelDefaultsOnMembersWithClientOptional"
        documentation: "Typically, non top-level members would have defaults filled in, but if they have the clientOptional trait, the defaults should be ignored."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithDefaults"
        headers: {"Content-Type": "application/json"}
        body: """
            {
                "clientOptionalDefaults": {}
            }"""
        params: {
            clientOptionalDefaults: {}
        }
    }
])

apply OperationWithDefaults @httpResponseTests([
    {
        id: "RestJsonClientPopulatesDefaultsValuesWhenMissingInResponse"
        documentation: "Client populates default values when missing in response."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
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
            emptyString: ""
            falseBoolean: false
            emptyBlob: ""
            zeroByte: 0
            zeroShort: 0
            zeroInteger: 0
            zeroLong: 0
            zeroFloat: 0.0
            zeroDouble: 0.0
        }
    }
    {
        id: "RestJsonClientIgnoresDefaultValuesIfMemberValuesArePresentInResponse"
        documentation: "Client ignores default values if member values are present in the response."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
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
                "defaultIntEnum": 2,
                "emptyString": "foo",
                "falseBoolean": true,
                "emptyBlob": "aGk=",
                "zeroByte": 1,
                "zeroShort": 1,
                "zeroInteger": 1,
                "zeroLong": 1,
                "zeroFloat": 1.0,
                "zeroDouble": 1.0
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
            emptyString: "foo"
            falseBoolean: true
            emptyBlob: "hi"
            zeroByte: 1
            zeroShort: 1
            zeroInteger: 1
            zeroLong: 1
            zeroFloat: 1.0
            zeroDouble: 1.0
        }
    }
    {
        id: "RestJsonServerPopulatesDefaultsInResponseWhenMissingInParams"
        documentation: "Server populates default values in response when missing in params."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: restJson1
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
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
                "defaultIntEnum": 1,
                "emptyString": "",
                "falseBoolean": false,
                "emptyBlob": "",
                "zeroByte": 0,
                "zeroShort": 0,
                "zeroInteger": 0,
                "zeroLong": 0,
                "zeroFloat": 0.0,
                "zeroDouble": 0.0
            }"""
        params: {}
    }
])

@http(uri: "/OperationWithDefaults", method: "POST")
operation OperationWithDefaults {
    input := {
        defaults: Defaults
        clientOptionalDefaults: ClientOptionalDefaults
        topLevelDefault: String = "hi" // Client should ignore default values in input shape
        otherTopLevelDefault: Integer = 0
    }

    output := with [DefaultsMixin] {}
}

structure Defaults with [DefaultsMixin] {}

structure ClientOptionalDefaults {
    @clientOptional
    member: Integer = 0
}

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
    defaultBlob: Blob = "YWJj"
    defaultByte: Byte = 1
    defaultShort: Short = 1
    defaultInteger: Integer = 10
    defaultLong: Long = 100
    defaultFloat: Float = 1.0
    defaultDouble: Double = 1.0
    defaultMap: TestStringMap = {}
    defaultEnum: TestEnum = "FOO"
    defaultIntEnum: TestIntEnum = 1
    emptyString: String = ""
    falseBoolean: Boolean = false
    emptyBlob: Blob = ""
    zeroByte: Byte = 0
    zeroShort: Short = 0
    zeroInteger: Integer = 0
    zeroLong: Long = 0
    zeroFloat: Float = 0.0
    zeroDouble: Double = 0.0
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
