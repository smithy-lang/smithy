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

// This uses a forward reference to a shape in another
// namespace brought in via a use statement, applies a
// trait to its member. Further, Widget was previously
// built and included in the model being built, and updating
// its member requires deconstructing the root built shape.
apply Widget$id @deprecated
