$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.timestamps#httpDateTimestampTrait

@httpDateTimestampTrait("Tue, 29 Apr 2014 18:30:38 GMT")
structure myStruct {}
