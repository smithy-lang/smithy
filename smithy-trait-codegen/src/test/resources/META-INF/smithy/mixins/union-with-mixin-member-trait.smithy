$version: "2.0"

namespace test.smithy.traitcodegen.mixins

@trait
union UnionWithMixinMember with [MyMixinUnion] {
    stringVariant: String

    integerVariant: Integer
}

@private
@mixin
union MyMixinUnion {
    unitVariant: Unit
}
