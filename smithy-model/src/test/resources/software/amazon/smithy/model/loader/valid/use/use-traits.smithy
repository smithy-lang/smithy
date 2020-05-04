$version: "1.0"

namespace smithy.example

use smithy.api#required
use smithy.api#sensitive
use smithy.api#deprecated

structure Struct {
    @required
    @sensitive
    @deprecated
    a: String,
}
