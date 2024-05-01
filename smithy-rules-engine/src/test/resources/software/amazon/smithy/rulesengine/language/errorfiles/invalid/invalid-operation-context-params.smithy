$version: "2.0"

namespace example

use smithy.rules#operationContextParams

service FizzBuzz {
    operations: [Bar]
}

@operationContextParams(
    invalidJmesPath: {path: "..." },
    failsLint: {path: "listOfObjects.notAKey"},
    unsupportedFunction: {path: "length(listOfObjects)"},
    unsupportedExpression: {path: "listOfObjects[1]"}
)
operation Bar {
    input: BarInput
}

structure BarInput {
    resourceId: ResourceId,
    listOfObjects: ListOfObjects
}

list ListOfObjects {
    member: ObjectMember
}

structure ObjectMember {
    key: Key,
}

string Key
string ResourceId
