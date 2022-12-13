$version: "2"

namespace smithy.example

operation Operation {
    input: OperationInput,
    output: OperationOutput,
    errors: [Error]
}

structure OperationInput {
    map: StringListMap,
}

structure OperationOutput {
    list: StringList,
}

@error("client")
@connected
structure Error {
    foo: MyString1,
}

string MyString1

list StringList {
    member: MyString1
}

map StringListMap {
    key: String,
    value: StringList,
}

@trait
structure notConnected {}

@trait
structure connected {}

string MyString2
