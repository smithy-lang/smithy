$version: "0.2.0"

use trait smithy.api#required
use trait smithy.api#sensitive
use trait smithy.api#deprecated

namespace smithy.example

structure Struct {
    @required
    @sensitive
    @deprecated
    a: String,
}
