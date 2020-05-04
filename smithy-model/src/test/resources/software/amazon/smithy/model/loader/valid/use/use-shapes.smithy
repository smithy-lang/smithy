$version: "1.0"

namespace smithy.example

use smithy.api#String
use smithy.api#Integer
use smithy.api#Long

list MyList {
    member: String,
}

structure Struct {
    a: Integer,
    b: Long,
}
