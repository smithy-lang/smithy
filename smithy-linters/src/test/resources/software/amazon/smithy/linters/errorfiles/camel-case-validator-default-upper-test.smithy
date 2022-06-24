$version: "2.0"

metadata validators = [
        {name: "CamelCase",
        id: "DefaultCamelCase"}
]

namespace smithy.example

service UpperService {
    version: "2020-09-21",
    operations: [UpperOperation],
}

operation UpperOperation {
    input: UpperOperationRequest,
    output: UpperOperationResponse,
    errors: [],
}

structure UpperOperationRequest {
    UpperCamel: MyString,
    SecondUpperCamel: MyString,
    ThirdUpperCamel: MyString,
    FourthUpperCamel: MyStructure
}

structure UpperOperationResponse {
    lowerCamel: MyString
}

service LowerService {
    version: "2020-09-21",
    operations: [LowerOperation],
}

operation LowerOperation {
    input: LowerOperationRequest,
    output: LowerOperationResponse,
    errors: [],
}

structure LowerOperationRequest {
    lowerCamel: MyString,
    secondLowerCamel: MyString,
    thirdLowerCamel: MyString,
    fourthLowerCamel: MyStructure
}

structure LowerOperationResponse {
    UpperCamel: MyString
}

structure MyStructure {
    snake_case: MyString
}

string MyString
