$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
structure IdRefMemberStruct {
    @idRef
    fieldA: String
}
