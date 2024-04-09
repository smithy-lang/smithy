$version: "2.0"

metadata validators = [
    {
        name: "InputOutputStructureReuse",
        id: "CustomInputOutputStructureReuse"
        severity: "DANGER",
    }
]

namespace smithy.example

operation GetFoo {
    input: GetFooInput,
    output: GetFooOutput
}

structure GetFooInput {}

structure GetFooOutput {}

operation GetBaz {
    input: GetBazInput,
    output: GetBazOutput
}

@input
structure GetBazInput {}

@output
structure GetBazOutput {}
