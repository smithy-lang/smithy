$version: "2.0"

namespace test.smithy.traitcodegen.lists

@trait
list StructureListTrait {
    member: listMember
}

@private
structure listMember {
    a: String
    b: Integer
    c: String
}
