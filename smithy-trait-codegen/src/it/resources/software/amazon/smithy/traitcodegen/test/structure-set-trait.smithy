$version: "2.0"

namespace test.smithy.traitcodegen

@StructureSetTrait([
    {
        a: "first"
        b: 1
        c: "other"
    } {
        a: "second"
        b: 2
        c: "more"
    }
])
structure myStruct {
}
