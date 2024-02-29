$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
structure IdRefStruct {
    fieldA: IdRefStructember
}

@private
@idRef
string IdRefStructember
