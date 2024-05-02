$version: "2.0"

namespace example

use smithy.rules#operationContextParams

service FizzBuzz {
    operations: [Bar]
}

@operationContextParams(
    toplLevelMember: {path: "resourceId" },
    projection: {path: "listOfObjects[*].key"},
    subExpression: {path: "nested.nested.bar"},
    recursive: {path: "nested.nested.recursiveMember.foo"}
    keysFunction: {path: "keys(map)"}
)
operation Bar {
    input: BarInput
}

structure BarInput {
    resourceId: String,
    nested: Nested1,
    listOfObjects: ListOfObjects,
    map: Map
}

list ListOfObjects {
    member: ObjectMember
}

structure ObjectMember {
    key: String,
}

map Map {
    key: String,
    value: String
}

structure Nested1 {
    foo: String,
    nested: Nested2
}

structure Nested2 {
    bar: String,
    recursiveMember: Nested1
}

