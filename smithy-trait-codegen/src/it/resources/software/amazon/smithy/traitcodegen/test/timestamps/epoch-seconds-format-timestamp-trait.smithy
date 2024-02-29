$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.timestamps#epochSecondsTimestampTrait

@epochSecondsTimestampTrait(1515531081.123)
structure myStruct {}
