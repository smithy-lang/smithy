$version: "2.0"

namespace smithy.example

use smithy.api#String
use smithy.api#Integer
use smithy.api#Long
use foo.example#Widget

list MyList {
    member: String,
}

list Widgets {
    member: Widget
}

structure Struct {
    a: Integer,
    b: Long,
}

// This does not have a use statement, so it needs to resolve to
// an absolute member of Widget and then deconstruct the root shape
// to apply the member trait.
apply foo.example#Widget$id @deprecated
