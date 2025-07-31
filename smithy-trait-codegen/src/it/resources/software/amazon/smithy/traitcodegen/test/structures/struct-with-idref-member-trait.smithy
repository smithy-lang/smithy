$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#StructWithIdrefMember

@StructWithIdrefMember(idRefMemberA: "test.smithy.traitcodegen#a", idRefMemberB: "test.smithy.traitcodegen#b")
structure myStruct {}

structure a {}

structure b {}
