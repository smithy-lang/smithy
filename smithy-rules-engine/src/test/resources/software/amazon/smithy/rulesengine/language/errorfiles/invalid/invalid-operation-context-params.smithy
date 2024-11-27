$version: "2.0"

namespace example

use smithy.rules#operationContextParams

service FizzBuzz {
    operations: [Bar]
}

@operationContextParams(
    invalidJmesPath: {path: "..." },
    arrayAsObject: {path: "listOfObjects.notAKey"},
    incorrectKey: {path: "nested.bar"},
    projectionOnScalar: {path: "nested.foo[*]"},
    unsupportedFunction: {path: "length(listOfObjects)"},
    unsupportedExpression: {path: "listOfObjects[1]"},
    projectionAndUnsupportedExpression: {path: "listOfObjects[*].listOfStrings[0]"}
)
operation Bar {
    input: BarInput
}

structure BarInput {
    nested: Nested,
    listOfObjects: ListOfObjects
}

list ListOfObjects {
    member: ObjectMember
}

structure ObjectMember {
    key: Key,
    listOfStrings: ListOfStrings
}

structure Nested {
    foo: String,
}

list ListOfStrings {
    member: String
}

string Key
