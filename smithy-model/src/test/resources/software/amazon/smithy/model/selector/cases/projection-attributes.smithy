$version: "2.0"

metadata selectorTests = [
    {
        selector: """
        service
        [trait|smithy.example#allowedTags]
        $service(*)
        ~>
        [trait|tags]
        :not([@: @{trait|tags|(values)} = @{var|service|trait|smithy.example#allowedTags|(values)}])
        """
        matches: [
            smithy.example#OperationD
        ]
    }
    {
        selector: """
        service
        [trait|smithy.example#allowedTags]
        $service(*)
        ~>
        [trait|enum]
        :not([@: @{trait|enum|(values)|tags|(values)}
                 {<} @{var|service|trait|smithy.example#allowedTags|(values)}])
        """
        matches: [
            smithy.example#BadEnum
        ]
    }
]

namespace smithy.example

@trait(selector: "service")
list allowedTags {
    member: String
}

@allowedTags(["internal", "external"])
service MyService {
    version: "2020-04-28"
    operations: [OperationA, OperationB, OperationC, OperationD]
}

operation OperationA {
    input: OperationAInput
}

@tags(["internal"])
operation OperationB {}

@tags(["internal", "external"])
operation OperationC {}

@tags(["invalid"])
operation OperationD {}

@input
structure OperationAInput {
    badValue: BadEnum
    goodValue: GoodEnum
}

@enum([
    {value: "a", tags: ["internal"]}
    {value: "b", tags: ["invalid"]}
])
string BadEnum

@enum([
    {value: "a"}
    {value: "b", tags: ["internal", "external"]}
    {value: "c", tags: ["internal"]}
])
string GoodEnum
