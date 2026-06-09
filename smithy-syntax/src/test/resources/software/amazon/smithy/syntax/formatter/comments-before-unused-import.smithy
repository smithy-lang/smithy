$version: "2.0"

namespace smithy.example

use smithy.api#Boolean

// This comment leads the removed import and is dropped with it.
use smithy.api#Integer

// This comment leads the shape and is kept.
structure Foo {
    bar: Boolean
}
