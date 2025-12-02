$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
map IdRefMemberMap {
    key: String

    @idRef
    value: String
}
