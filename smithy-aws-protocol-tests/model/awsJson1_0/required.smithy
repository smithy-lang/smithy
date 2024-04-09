$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpResponseTests

apply OperationWithRequiredMembers @httpResponseTests([
    {
        id: "AwsJson10ClientErrorCorrectsWhenServerFailsToSerializeRequiredValues"
        documentation: "Client error corrects when server fails to serialize required values."
        appliesTo: "client"
        tags: ["defaults", "error-correction"]
        protocol: awsJson1_0
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: "{}"
        params: {
            requiredString: ""
            requiredBoolean: false
            requiredList: []
            requiredTimestamp: 0
            requiredBlob: ""
            requiredByte: 0
            requiredShort: 0
            requiredInteger: 0
            requiredLong: 0
            requiredFloat: 0.0
            requiredDouble: 0.0
            requiredMap: {}
        }
    }
])

operation OperationWithRequiredMembers {
    output := with [RequiredMembersMixin] {
    }
}

apply OperationWithRequiredMembersWithDefaults @httpResponseTests([
    {
        id: "AwsJson10ClientErrorCorrectsWithDefaultValuesWhenServerFailsToSerializeRequiredValues"
        documentation: "Client error corrects with default values when server fails to serialize required values."
        appliesTo: "client"
        tags: ["defaults", "error-correction"]
        protocol: awsJson1_0
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.0"}
        body: "{}"
        params: {
            requiredString: "hi"
            requiredBoolean: true
            requiredList: []
            requiredTimestamp: 1
            requiredBlob: "{}"
            requiredByte: 1
            requiredShort: 1
            requiredInteger: 10
            requiredLong: 100
            requiredFloat: 1.0
            requiredDouble: 1.0
            requiredMap: {}
            requiredEnum: "FOO"
            requiredIntEnum: 1
        }
    }
])

operation OperationWithRequiredMembersWithDefaults {
    output := with [RequiredMembersWithDefaultsMixin] {
    }
}

@mixin
structure RequiredMembersMixin {
    @required
    requiredString: String

    @required
    requiredBoolean: Boolean

    @required
    requiredList: RequiredStringList

    @required
    requiredTimestamp: Timestamp

    @required
    requiredBlob: Blob

    @required
    requiredByte: Byte

    @required
    requiredShort: Short

    @required
    requiredInteger: Integer

    @required
    requiredLong: Long

    @required
    requiredFloat: Float

    @required
    requiredDouble: Double

    @required
    requiredMap: RequiredStringMap
}

@mixin
structure RequiredMembersWithDefaultsMixin {
    @required
    requiredString: String = "hi"

    @required
    requiredBoolean: Boolean = true

    @required
    requiredList: RequiredStringList = []

    @required
    requiredTimestamp: Timestamp = 1

    @required
    requiredBlob: Blob = "{}"

    @required
    requiredByte: Byte = 1

    @required
    requiredShort: Short = 1

    @required
    requiredInteger: Integer = 10

    @required
    requiredLong: Long = 100

    @required
    requiredFloat: Float = 1.0

    @required
    requiredDouble: Double = 1.0

    @required
    requiredMap: RequiredStringMap = {}

    @required
    requiredEnum: RequiredEnum = "FOO"

    @required
    requiredIntEnum: RequiredIntEnum = 1
}

list RequiredStringList {
    member: String
}

map RequiredStringMap {
    key: String
    value: String
}

enum RequiredEnum {
    FOO
    BAR
    BAZ
}

intEnum RequiredIntEnum {
    ONE = 1
    TWO = 2
}
