$version: "1.0"

metadata validators = [
        {name: "CamelCase",
        id: "DefaultCamelCase"}
]

namespace smithy.example

service Example {
    version: "2020-09-21",
    operations: [GetLatestService],
}

operation GetLatestService {
    input: GetLatestServiceRequest,
    output: GetLatestServiceResponse,
    errors: [],
}

structure GetLatestServiceRequest {
    UpperCamel: MyString,
    SecondUpperCamel: MyString,
    ThirdUpperCamel: MyString,
    FourthUpperCamel: MyStructure
}

structure GetLatestServiceResponse {
    lowerCamel: MyString
}

structure MyStructure {
    snake_case: MyString
}

string MyString
