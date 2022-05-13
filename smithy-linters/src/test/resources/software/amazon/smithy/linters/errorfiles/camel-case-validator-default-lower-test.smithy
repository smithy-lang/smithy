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
    lowerCamel: MyString,
    secondLowerCamel: MyString,
    thirdLowerCamel: MyString,
    fourthLowerCamel: MyStructure
}

structure GetLatestServiceResponse {
    UpperCamel: MyString
}

structure MyStructure {
    snake_case: MyString
}

string MyString
