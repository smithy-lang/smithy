$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.documents#DocumentTrait

@DocumentTrait({ metadata: "woo", more: "yay" })
structure myStruct {}
