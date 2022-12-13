$version: "2"

namespace smithy.example

@trait(selector: "service")
list allowedTags {
    member: String,
}

@allowedTags(["internal", "external"])
service MyService {
    version: "2020-04-28",
    operations: [OperationA, OperationB, OperationC, OperationD]
}

operation OperationA {
    input: OperationAInput,
}

@tags(["internal"])
operation OperationB {}

@tags(["internal", "external"])
operation OperationC {}

@tags(["invalid"])
operation OperationD {}

structure OperationAInput {
    badValue: BadEnum,
    goodValue: GoodEnum,
}

@enum([
    {value: "a", tags: ["internal"]},
    {value: "b", tags: ["invalid"]},
])
string BadEnum

@enum([
    {value: "a"},
    {value: "b", tags: ["internal", "external"]},
    {value: "c", tags: ["internal"]},
])
string GoodEnum
