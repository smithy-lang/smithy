$version: "0.1.0"

use shape smithy.api#String
use shape smithy.api#Integer
use shape smithy.api#Long

namespace smithy.example

list MyList {
    member: String,
}

structure Struct {
    a: Integer,
    b: Long,
}
