$version: "2.0"

metadata wrongShapeType = {
    __type__: "smithy.api#String"
}

metadata wrongMemberType = {
    __type__: "test#TestMetadata"
    foo: "bar"
}

namespace test

structure TestMetadata {
    foo: Integer
}
