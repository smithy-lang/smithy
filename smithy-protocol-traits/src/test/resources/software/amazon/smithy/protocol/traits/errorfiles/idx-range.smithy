$version: "2.0"

namespace smithy.example

use smithy.protocols#idx

structure IndexRange {
    @idx(65535)
    index65535: String

    @idx(65536)
    index65536: String
}
