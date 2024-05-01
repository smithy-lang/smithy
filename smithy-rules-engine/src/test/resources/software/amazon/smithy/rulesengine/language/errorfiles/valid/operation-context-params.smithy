$version: "2.0"

namespace example

use smithy.rules#operationContextParams

service FizzBuzz {
    operations: [Bar]
}

@operationContextParams(
    toplLevelMember: {path: "resourceId" },
    projection: {path: "listOfObjects[*].key"},
    subExpression: {path: "nestedObject.key"},
    keysFunction: {path: "keys(map)"}
)
operation Bar {
    input: BarInput
}

structure BarInput {
    resourceId: ResourceId,
    nestedObject: ObjectMember,
    listOfObjects: ListOfObjects,
    map: Map
}

list ListOfObjects {
    member: ObjectMember
}

structure ObjectMember {
    key: Key,
}

map Map {
    key: Key,
    value: ResourceId
}

string Key
string ResourceId
