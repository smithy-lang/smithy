$version: "2.0"

namespace smithy.example

use smithy.protocols#idx

@mixin
structure InvalidMixin {
    @idx(1)
    member: String
}

list InvalidList {
    @idx(1)
    member: String
}

map InvalidMap {
    @idx(1)
    key: String

    @idx(2)
    value: String
}
