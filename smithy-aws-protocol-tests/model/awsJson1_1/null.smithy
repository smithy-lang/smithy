$version: "1.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringMap
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "AwsJson11StructuresDontSerializeNullValues",
        documentation: "Null structure values are dropped",
        protocol: awsJson1_1,
        body: "{}",
        bodyMediaType: "application/json",
        params: {
            string: null
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "AwsJson11MapsSerializeNullValues",
        documentation: "Serializes null values in maps",
        protocol: awsJson1_1,
        body: """
            {
                "stringMap": {
                    "foo": null
                }
            }""",
        bodyMediaType: "application/json",
        params: {
          stringMap: {
              "foo": null
          }
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "AwsJson11ListsSerializeNull",
        documentation: "Serializes null values in lists",
        protocol: awsJson1_1,
        body: """
            {
                "stringList": [
                    null
                ]
            }""",
        bodyMediaType: "application/json",
        params: {
          stringList: [null]
        },
        method: "POST",
        uri: "/",
    }
])
@httpResponseTests([
    {
        id: "AwsJson11StructuresDontDeserializeNullValues",
        documentation: "Null structure values are dropped",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "string": null
            }""",
        bodyMediaType: "application/json",
        params: {},
    },
    {
        id: "AwsJson11MapsDeserializeNullValues",
        documentation: "Deserializes null values in maps",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "stringMap": {
                    "foo": null
                }
            }""",
        bodyMediaType: "application/json",
        params: {
          stringMap: {
              "foo": null
          }
        },
    },
    {
        id: "AwsJson11ListsDeserializeNull",
        documentation: "Deserializes null values in lists",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "stringList": [
                    null
                ]
            }""",
        bodyMediaType: "application/json",
        params: {
          stringList: [null]
        },
    }
])
operation NullOperation {
    input: NullOperationInputOutput,
    output: NullOperationInputOutput,
}

structure NullOperationInputOutput {
    string: String,
    stringList: StringList,
    stringMap: StringMap,
}
