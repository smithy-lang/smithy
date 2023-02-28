$version: "2.0"

metadata selectorTests = [
    {
        selector: "[service]"
        matches: [
            smithy.example#ServiceA
            smithy.example#ServiceB
        ]
    }
    {
        selector: "[service = smithy.example#ServiceA]"
        matches: [
            smithy.example#ServiceA
        ]
    }
    {
        selector: "[service|version ^= '2018-']"
        matches: [
            smithy.example#ServiceB
        ]
    }
]

namespace smithy.example

service ServiceA {
    version: "2019-06-17"
}

service ServiceB {
    version: "2018-06-17"
}
