$version: "2"

metadata validators = [
    {
        name: "MissingClientOptionalTrait",
        configuration: {
            "onRequiredStructureOrUnion": true
        }
    }
]

namespace smithy.example

structure Foo {
    bar: String = "",

    @required
    baz: String,

    @required
    bam: Bam,

    @required
    boo: Boo
}

structure Bam {}

union Boo {
    test: String
}
