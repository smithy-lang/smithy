$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.maps#NestedStringUniqueItemMapTrait

@NestedStringUniqueItemMapTrait({
    a: [
        {
            b: "c"
        }
    ]
})
structure myStruct {}
