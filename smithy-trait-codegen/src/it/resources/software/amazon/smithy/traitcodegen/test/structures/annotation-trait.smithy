$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#basicAnnotationTrait

@basicAnnotationTrait
structure myStruct {
    fieldA: String
}
