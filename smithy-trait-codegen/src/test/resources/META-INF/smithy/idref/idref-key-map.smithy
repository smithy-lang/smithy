$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
map IdRefKeyMap {
    @idRef
    key: String

    value: String
}
