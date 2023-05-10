$version: "2.0"

metadata selectorTests = [
    {
        selector: "[trait|externalDocumentation|(keys) = Homepage]"
        matches: [
            smithy.example#ServiceA
        ]
    }
    {
        selector: "[trait|enum|(values)|tags|(values) = internal]"
        matches: [
            smithy.example#Ec2Instance
        ]
    }
    {
        selector: "[trait|documentation|(length) < 3]"
        matches: [
            smithy.example#ServiceB
        ]
    }
    {
        selector: "[trait|externalDocumentation|'API Reference']"
        matches: [
            smithy.example#ServiceA
        ]
    }
    {
        selector: "[trait|documentation|invalid|child = Hi]"
        matches: [
        ]
    }
]

namespace smithy.example

@documentation("This is ServiceA")
@externalDocumentation(
    "Homepage": "https://www.example.com/"
    "API Reference": "https://www.example.com/api-ref"
)
service ServiceA {
    version: "2019-06-17"
}

@enum([
    {
        value: "t2.nano",
        name: "T2_NANO",
        tags: ["internal"]
    },
    {
        value: "t2.micro",
        name: "T2_MICRO",
        tags: ["external"]
    },
    {
        value: "m256.mega",
        name: "M256_MEGA",
        deprecated: true
    }
])
string Ec2Instance

@documentation("??")
service ServiceB {

}
