$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.names#snake_case_structure

@snake_case_structure(snake_case_member: "stuff")
structure myStruct {}
