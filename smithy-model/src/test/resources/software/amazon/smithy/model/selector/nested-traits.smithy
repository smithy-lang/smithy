$version: "2"

namespace smithy.example

@enum([
    {
        value: "t2.nano",
        name: "T2_NANO",
        documentation: """
            T2 instances are Burstable Performance
            Instances that provide a baseline level of CPU
            performance with the ability to burst above the
            baseline.""",
        tags: ["ebsOnly"]
    },
    {
        value: "t2.micro",
        name: "T2_MICRO",
        documentation: """
            T2 instances are Burstable Performance
            Instances that provide a baseline level of CPU
            performance with the ability to burst above the
            baseline.""",
        tags: ["ebsOnly"]
    },
    {
        value: "m256.mega",
        name: "M256_MEGA",
        deprecated: true
    },
    {
        value: "hi",
        name: "bye"
    },
    {
        value: "bye",
        name: "hi"
    },
])
@tags(["foo", "baz"])
string EnumString

@range(min: 1, max: 10)
integer RangeInt1

@range(min: 100, max: 1000)
integer RangeInt2

@externalDocumentation(
    "Homepage": "https://www.example.com/",
    "API Reference": "https://www.example.com/api-ref",
)
@enum([
    {
        value: "m256.mega",
        name: "M256_MEGA",
        tags: ["notEbs"]
    },
    {
        value: "hi",
        name: "hi",
        tags: ["hi", "there"]
    },
])
@nestedTrait(foo: {foo: {bar: "hi"}})
string DocumentedString1

@documentation("Hi")
@externalDocumentation("Foo": "https://www.anotherexample.com/")
@nestedTrait(foo: {foo: {bar: "bye"}})
@enum([{value: "m256.mega", tags: []}])
string DocumentedString2

@trait
structure nestedTrait {
    foo: MoreNesting,
}

structure MoreNesting {
    foo: EvenMoreNesting,
}

structure EvenMoreNesting {
    bar: String,
}

@error("server")
@httpError(500)
structure ErrorStruct1 {}

@error("client")
@httpError(400)
structure ErrorStruct2 {}

@listyTrait([[[{foo: "a"}, {foo: "b"}]], [[{foo: "c"}]]])
service MyService {
    version: "2020-04-21"
}

@trait
list listyTrait {
    member: ListyTraitMember1,
}

list ListyTraitMember1 {
    member: ListyTraitMember2,
}

list ListyTraitMember2 {
    member: ListyTraitStruct,
}

structure ListyTraitStruct {
    foo: String
}

@trait
@recursiveTrait("Wow!")
string recursiveTrait
