$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.framework#ValidationException

apply OperationWithDefaults @httpRequestTests([
    {
        id: "RpcV2CborClientPopulatesDefaultValuesInInput"
        documentation: "Client populates default values in input."
        protocol: rpcv2Cbor
        appliesTo: "client"
        tags: ["defaults"]
        method: "POST"
        uri: "/"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        bodyMediaType: "application/cbor"
        body: "v2hkZWZhdWx0c65tZGVmYXVsdFN0cmluZ2JoaW5kZWZhdWx0Qm9vbGVhbvVrZGVmYXVsdExpc3SAcGRlZmF1bHRUaW1lc3RhbXDB+wAAAAAAAAAAa2RlZmF1bHRCbG9iY2FiY2tkZWZhdWx0Qnl0ZQFsZGVmYXVsdFNob3J0AW5kZWZhdWx0SW50ZWdlcgprZGVmYXVsdExvbmcYZGxkZWZhdWx0RmxvYXT7P/AAAAAAAABtZGVmYXVsdERvdWJsZfs/8AAAAAAAAGpkZWZhdWx0TWFwoGtkZWZhdWx0RW51bWNGT09uZGVmYXVsdEludEVudW0B/w=="
        params: {
            defaults: {}
        }
    }
    {
        id: "RpcV2CborClientSkipsTopLevelDefaultValuesInInput"
        documentation: "Client skips top level default values in input."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        method: "POST"
        bodyMediaType: "application/cbor"
        uri: "/"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        body: "v/8="
        params: {
        }
    }
    {
        id: "RpcV2CborClientUsesExplicitlyProvidedMemberValuesOverDefaults"
        documentation: "Client uses explicitly provided member values over defaults"
        appliesTo: "client"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        method: "POST"
        bodyMediaType: "application/cbor"
        uri: "/"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        body: "v2hkZWZhdWx0c65tZGVmYXVsdFN0cmluZ2NieWVuZGVmYXVsdEJvb2xlYW71a2RlZmF1bHRMaXN0gWFhcGRlZmF1bHRUaW1lc3RhbXDB+z/wAAAAAAAAa2RlZmF1bHRCbG9iYmhpa2RlZmF1bHRCeXRlAmxkZWZhdWx0U2hvcnQCbmRlZmF1bHRJbnRlZ2VyFGtkZWZhdWx0TG9uZxjIbGRlZmF1bHRGbG9hdPtAAAAAAAAAAG1kZWZhdWx0RG91Ymxl+0AAAAAAAAAAamRlZmF1bHRNYXChZG5hbWVkSmFja2tkZWZhdWx0RW51bWNCQVJuZGVmYXVsdEludEVudW0C/w=="
        params: {
            defaults: {
                defaultString: "bye",
                defaultBoolean: true,
                defaultList: ["a"],
                defaultTimestamp: 1,
                defaultBlob: "hi",
                defaultByte: 2,
                defaultShort: 2,
                defaultInteger: 20,
                defaultLong: 200,
                defaultFloat: 2.0,
                defaultDouble: 2.0,
                defaultMap: {name: "Jack"},
                defaultEnum: "BAR",
                defaultIntEnum: 2
            }
        }
    }
    {
        id: "RpcV2CborServerPopulatesDefaultsWhenMissingInRequestBody"
        documentation: "Server populates default values when missing in request body."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        method: "POST"
        bodyMediaType: "application/cbor"
        uri: "/"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        body: "v2hkZWZhdWx0c6D/"
        params: {
            defaults: {
                defaultString: "hi"
                defaultBoolean: true
                defaultList: []
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
        id: "RpcV2CborClientPopulatesDefaultsValuesWhenMissingInResponse"
        documentation: "Client populates default values when missing in response."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        code: 200
        bodyMediaType: "application/cbor"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        body: "v/8="
        params: {
            defaultString: "hi"
            defaultBoolean: true
            defaultList: []
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
        id: "RpcV2CborClientIgnoresDefaultValuesIfMemberValuesArePresentInResponse"
        documentation: "Client ignores default values if member values are present in the response."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        code: 200
        bodyMediaType: "application/cbor"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        body: "v21kZWZhdWx0U3RyaW5nY2J5ZW5kZWZhdWx0Qm9vbGVhbvRrZGVmYXVsdExpc3SBYWFwZGVmYXVsdFRpbWVzdGFtcMH7QAAAAAAAAABrZGVmYXVsdEJsb2JiaGlrZGVmYXVsdEJ5dGUCbGRlZmF1bHRTaG9ydAJuZGVmYXVsdEludGVnZXIUa2RlZmF1bHRMb25nGMhsZGVmYXVsdEZsb2F0+0AAAAAAAAAAbWRlZmF1bHREb3VibGX7QAAAAAAAAABqZGVmYXVsdE1hcKFkbmFtZWRKYWNra2RlZmF1bHRFbnVtY0JBUm5kZWZhdWx0SW50RW51bQL/"
        params: {
            defaultString: "bye",
            defaultBoolean: false,
            defaultList: ["a"],
            defaultTimestamp: 2,
            defaultBlob: "hi",
            defaultByte: 2,
            defaultShort: 2,
            defaultInteger: 20,
            defaultLong: 200,
            defaultFloat: 2.0,
            defaultDouble: 2.0,
            defaultMap: {name: "Jack"},
            defaultEnum: "BAR",
            defaultIntEnum: 2
        }
    }
    {
        id: "RpcV2CborServerPopulatesDefaultsInResponseWhenMissingInParams"
        documentation: "Server populates default values in response when missing in params."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        code: 200
        bodyMediaType: "application/cbor"
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        body: "v21kZWZhdWx0U3RyaW5nYmhpbmRlZmF1bHRCb29sZWFu9WtkZWZhdWx0TGlzdIBwZGVmYXVsdFRpbWVzdGFtcMH7AAAAAAAAAABrZGVmYXVsdEJsb2JjYWJja2RlZmF1bHRCeXRlAWxkZWZhdWx0U2hvcnQBbmRlZmF1bHRJbnRlZ2VyCmtkZWZhdWx0TG9uZxhkbGRlZmF1bHRGbG9hdPs/8AAAAAAAAG1kZWZhdWx0RG91Ymxl+z/wAAAAAAAAamRlZmF1bHRNYXCga2RlZmF1bHRFbnVtY0ZPT25kZWZhdWx0SW50RW51bQH/"
        params: {}
    }
])

operation OperationWithDefaults {
    input := {
        defaults: Defaults

        topLevelDefault: String = "hi" // Client should ignore default values in input shape
    }

    output := with [DefaultsMixin] {}

    errors: [ValidationException]
}

structure Defaults with [DefaultsMixin] {}

@mixin
structure DefaultsMixin {
    defaultString: String = "hi"
    defaultBoolean: Boolean = true
    defaultList: TestStringList = []
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
