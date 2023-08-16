$version: "2.0"

namespace smithy.example

@blobExt("blob content")
@booleanExt(true)
@stringExt("string content")
@byteExt(64)
@shortExt(16384)
@integerExt(1073741824)
@longExt(4611686018427387904)
@floatExt(1.07846)
@doubleExt(57.64123)
@bigIntegerExt(46116860184273879045678)
@bigDecimalExt(0.1234567890123456789)
@timestampExt("2023-02-27T13:01:57Z")
@documentExt({
    "a": "b",
    "c": ["d"]
})
@enumExt("first")
@intEnumExt(3)
@listExt(["a", "b", "c"])
@mapExt("a": 15, "b": 18)
@structureExt(
    stringMember: "first field"
    integerMember: 17
)
@unionExt(string: "string variant")
string Name

structure Input {
    name: Name
    language: String
}

@http(method: "PUT", uri: "/")
operation Operation {
    input: Input
}

@aws.protocols#restJson1
service Service {
    operations: [Operation]
}
