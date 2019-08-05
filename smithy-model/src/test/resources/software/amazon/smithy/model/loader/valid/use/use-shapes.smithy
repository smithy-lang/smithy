$version: "0.4.0"

use smithy.api#String
use smithy.api#Integer
use smithy.api#Long

namespace smithy.example

list MyList {
    member: String,
}

structure Struct {
    a: Integer,
    b: Long,
}
