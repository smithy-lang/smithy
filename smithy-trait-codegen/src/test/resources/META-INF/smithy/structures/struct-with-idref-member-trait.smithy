$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure StructWithIdrefMember {
    @idRef(selector: "structure")
    idRefMemberA: String
    idRefMemberB: IdRefMemberB
}

@idRef(selector: "structure")
string IdRefMemberB
