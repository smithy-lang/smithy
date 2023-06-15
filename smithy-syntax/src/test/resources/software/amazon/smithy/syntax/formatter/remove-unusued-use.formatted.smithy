$version: "2.0"

namespace smithy.example

use smithy.api#Integer
use smithy.api#String
use smithy.api#range
use smithy.api#required
use smithy.api#sensitive

structure Foo {
    @required
    // this is strange, but resolve to the full ID
    @documentation(Integer)
    a: String

    @tags([sensitive, range$min, smithy.api#Long, smithy.api#Short])
    b: String
}
