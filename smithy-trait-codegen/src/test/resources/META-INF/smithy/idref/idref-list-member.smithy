$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
list IdRefMemberListTrait {
    @idRef
    member: String
}
