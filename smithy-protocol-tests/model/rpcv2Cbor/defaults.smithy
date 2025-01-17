$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

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
        uri: "/service/RpcV2Protocol/operation/OperationWithDefaults",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        bodyMediaType: "application/cbor"
        body: "v2hkZWZhdWx0c79tZGVmYXVsdFN0cmluZ2JoaW5kZWZhdWx0Qm9vbGVhbvVrZGVmYXVsdExpc3Sf/3BkZWZhdWx0VGltZXN0YW1wwQBrZGVmYXVsdEJsb2JDYWJja2RlZmF1bHRCeXRlAWxkZWZhdWx0U2hvcnQBbmRlZmF1bHRJbnRlZ2VyCmtkZWZhdWx0TG9uZxhkbGRlZmF1bHRGbG9hdPo/gAAAbWRlZmF1bHREb3VibGX6P4AAAGpkZWZhdWx0TWFwv/9rZGVmYXVsdEVudW1jRk9PbmRlZmF1bHRJbnRFbnVtAWtlbXB0eVN0cmluZ2BsZmFsc2VCb29sZWFu9GllbXB0eUJsb2JAaHplcm9CeXRlAGl6ZXJvU2hvcnQAa3plcm9JbnRlZ2VyAGh6ZXJvTG9uZwBpemVyb0Zsb2F0+gAAAABqemVyb0RvdWJsZfoAAAAA//8="
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
        uri: "/service/RpcV2Protocol/operation/OperationWithDefaults",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
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
        uri: "/service/RpcV2Protocol/operation/OperationWithDefaults",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "v2hkZWZhdWx0c7dtZGVmYXVsdFN0cmluZ2NieWVuZGVmYXVsdEJvb2xlYW71a2RlZmF1bHRMaXN0gWFhcGRlZmF1bHRUaW1lc3RhbXDB+z/wAAAAAAAAa2RlZmF1bHRCbG9iQmhpa2RlZmF1bHRCeXRlAmxkZWZhdWx0U2hvcnQCbmRlZmF1bHRJbnRlZ2VyFGtkZWZhdWx0TG9uZxjIbGRlZmF1bHRGbG9hdPpAAAAAbWRlZmF1bHREb3VibGX7QAAAAAAAAABqZGVmYXVsdE1hcKFkbmFtZWRKYWNra2RlZmF1bHRFbnVtY0JBUm5kZWZhdWx0SW50RW51bQJrZW1wdHlTdHJpbmdjZm9vbGZhbHNlQm9vbGVhbvVpZW1wdHlCbG9iQmhpaHplcm9CeXRlAWl6ZXJvU2hvcnQBa3plcm9JbnRlZ2VyAWh6ZXJvTG9uZwFpemVyb0Zsb2F0+j+AAABqemVyb0RvdWJsZfs/8AAAAAAAAP8="
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
                defaultIntEnum: 2,
                emptyString: "foo",
                falseBoolean: true,
                emptyBlob: "hi",
                zeroByte: 1,
                zeroShort: 1,
                zeroInteger: 1,
                zeroLong: 1,
                zeroFloat: 1.0,
                zeroDouble: 1.0
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
        uri: "/service/RpcV2Protocol/operation/OperationWithDefaults",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "v2hkZWZhdWx0c6D/"
        params: {
            defaults: {
                defaultString: "hi",
                defaultBoolean: true,
                defaultList: [],
                defaultTimestamp: 0,
                defaultBlob: "abc",
                defaultByte: 1,
                defaultShort: 1,
                defaultInteger: 10,
                defaultLong: 100,
                defaultFloat: 1.0,
                defaultDouble: 1.0,
                defaultMap: {},
                defaultEnum: "FOO",
                defaultIntEnum: 1,
                emptyString: "",
                falseBoolean: false,
                emptyBlob: "",
                zeroByte: 0,
                zeroShort: 0,
                zeroInteger: 0,
                zeroLong: 0,
                zeroFloat: 0.0,
                zeroDouble: 0.0
            },
            topLevelDefault: "hi",
            otherTopLevelDefault: 0
        }
    }
    {
        id: "RpcV2CborClientUsesExplicitlyProvidedValuesInTopLevel"
        documentation: "Any time a value is provided for a member in the top level of input, it is used, regardless of if its the default."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        method: "POST"
        bodyMediaType: "application/cbor"
        uri: "/service/RpcV2Protocol/operation/OperationWithDefaults",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "v290b3BMZXZlbERlZmF1bHRiaGl0b3RoZXJUb3BMZXZlbERlZmF1bHQA/w=="
        params: {
            topLevelDefault: "hi",
            otherTopLevelDefault: 0
        }
    }
    {
        id: "RpcV2CborClientIgnoresNonTopLevelDefaultsOnMembersWithClientOptional"
        documentation: "Typically, non top-level members would have defaults filled in, but if they have the clientOptional trait, the defaults should be ignored."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: rpcv2Cbor
        method: "POST"
        bodyMediaType: "application/cbor"
        uri: "/service/RpcV2Protocol/operation/OperationWithDefaults",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "v3ZjbGllbnRPcHRpb25hbERlZmF1bHRzoP8="
        params: {
            clientOptionalDefaults: {}
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
        body: "v21kZWZhdWx0U3RyaW5nY2J5ZW5kZWZhdWx0Qm9vbGVhbvRrZGVmYXVsdExpc3SBYWFwZGVmYXVsdFRpbWVzdGFtcMH7QAAAAAAAAABrZGVmYXVsdEJsb2JCaGlrZGVmYXVsdEJ5dGUCbGRlZmF1bHRTaG9ydAJuZGVmYXVsdEludGVnZXIUa2RlZmF1bHRMb25nGMhsZGVmYXVsdEZsb2F0+kAAAABtZGVmYXVsdERvdWJsZftAAAAAAAAAAGpkZWZhdWx0TWFwoWRuYW1lZEphY2trZGVmYXVsdEVudW1jQkFSbmRlZmF1bHRJbnRFbnVtAmtlbXB0eVN0cmluZ2Nmb29sZmFsc2VCb29sZWFu9WllbXB0eUJsb2JCaGloemVyb0J5dGUBaXplcm9TaG9ydAFremVyb0ludGVnZXIBaHplcm9Mb25nAWl6ZXJvRmxvYXT6P4AAAGp6ZXJvRG91Ymxl+z/wAAAAAAAA/w=="
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
            defaultIntEnum: 2,
            emptyString: "foo",
            falseBoolean: true,
            emptyBlob: "hi",
            zeroByte: 1,
            zeroShort: 1,
            zeroInteger: 1,
            zeroLong: 1,
            zeroFloat: 1.0,
            zeroDouble: 1.0
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
        body: "v21kZWZhdWx0U3RyaW5nYmhpbmRlZmF1bHRCb29sZWFu9WtkZWZhdWx0TGlzdIBwZGVmYXVsdFRpbWVzdGFtcMH7AAAAAAAAAABrZGVmYXVsdEJsb2JDYWJja2RlZmF1bHRCeXRlAWxkZWZhdWx0U2hvcnQBbmRlZmF1bHRJbnRlZ2VyCmtkZWZhdWx0TG9uZxhkbGRlZmF1bHRGbG9hdPo/gAAAbWRlZmF1bHREb3VibGX7P/AAAAAAAABqZGVmYXVsdE1hcKBrZGVmYXVsdEVudW1jRk9PbmRlZmF1bHRJbnRFbnVtAWtlbXB0eVN0cmluZ2BsZmFsc2VCb29sZWFu9GllbXB0eUJsb2JAaHplcm9CeXRlAGl6ZXJvU2hvcnQAa3plcm9JbnRlZ2VyAGh6ZXJvTG9uZwBpemVyb0Zsb2F0+gAAAABqemVyb0RvdWJsZfsAAAAAAAAAAP8="
        params: {}
    }
])

operation OperationWithDefaults {
    input := {
        defaults: Defaults
        clientOptionalDefaults: ClientOptionalDefaults
        topLevelDefault: String = "hi" // Client should ignore default values in input shape
        otherTopLevelDefault: Integer = 0
    }

    output := with [DefaultsMixin] {}

    errors: [ValidationException]
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
