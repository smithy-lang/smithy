$version: "2.0"

namespace test.smithy.traitcodegen


@StringToStructMap(
    one: {
        a: "foo"
        b: 2
    }
    two: {
        a: "bar"
        b: 4
    }
)
structure myStruct {
}
