$version: "2.0"

namespace test.smithy.traitcodegen

structure myStruct {
    @stringTrait("Testing String Trait")
    fieldA: String
}