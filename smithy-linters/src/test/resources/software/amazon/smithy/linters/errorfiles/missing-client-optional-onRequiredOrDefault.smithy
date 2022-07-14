$version: "2"

metadata validators = [
    {
        name: "MissingClientOptionalTrait",
        configuration: {
            "onRequiredOrDefault": true
        }
    }
]

namespace smithy.example

structure Foo {
    bar: String = ""

    @required
    baz: String
}
