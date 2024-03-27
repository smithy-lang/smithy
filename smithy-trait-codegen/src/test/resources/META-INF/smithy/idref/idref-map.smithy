$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
map IdRefMap {
    key: String
    value: IdRefMapMember
}

@private
@idRef
string IdRefMapMember
