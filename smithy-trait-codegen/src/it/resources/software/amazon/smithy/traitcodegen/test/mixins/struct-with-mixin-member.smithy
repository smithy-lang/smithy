$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.mixins#structureListWithMixinMember

@structureListWithMixinMember([
    {
        a: "first"
        b: 1
        c: "other"
        d: "mixed-in"
    }
    {
        a: "second"
        b: 2
        c: "more"
        d: "mixins are cool"
    }
])
structure myStruct {}
