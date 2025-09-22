$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.mixins#UnionWithMixinMember

@UnionWithMixinMember({unitVariant:""})
structure myStruct {}
