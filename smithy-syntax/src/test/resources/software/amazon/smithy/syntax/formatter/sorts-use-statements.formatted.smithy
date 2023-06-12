$version: "2.0"

namespace smithy.example

use smithy.api#Integer
use smithy.api#Long
use smithy.api#String
use smithy.api#required

structure UseThem {
    @required
    string: String

    integer: Integer

    long: Long
}
