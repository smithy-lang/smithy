$version: "2.0"

namespace test.smithy.traitcodegen.uniqueitems

@trait
@uniqueItems
list StructureSetTrait {
    member: setMember
}

@private
structure setMember {
    a: String
    b: Integer
    c: String
}
