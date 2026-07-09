$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
map IdRefNestedKeyMap {
    key: String
    value: IdRefKeyInnerMap
}

map IdRefKeyInnerMap {
    @idRef
    key: String
    value: String
}
