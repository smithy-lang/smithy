$version: "1.0"

namespace smithy.example

use smithy.api#required
use smithy.api#internal
use smithy.api#deprecated

structure Struct {
    @required
    @internal
    @deprecated
    a: String,
}
