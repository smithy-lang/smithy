$version: "2.0"

namespace test.smithy.traitcodegen.mixins

// The following trait checks that mixins are correctly flattened by
// the trait codegen plugin
@trait
list structureListWithMixinMember {
    member: listMemberWithMixin
}

@private
structure listMemberWithMixin with [extras] {
    a: String
    b: Integer
    c: String
}
