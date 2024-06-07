$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.mixins#structWithMixin

@structWithMixin(d: "mixed-in")
structure myStruct {}
