$version: "2.0"

namespace test.smithy.traitcodegen

// ==================
//  Mixin tests
// ==================
// The following traits check that mixins are correctly flattened by
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

@trait
structure structWithMixin with [extras] {}

@private
@mixin
structure extras {
    @required
    d: String
}
