$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#StructWithListOfMapTrait

@StructWithListOfMapTrait({
    name: "a"
    items: [{
        b: "c"
    }]
})
structure myStruct {
}
