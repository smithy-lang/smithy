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
