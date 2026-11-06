$version: "2"

namespace ns.foo

intEnum IntEnum with [IntMixin] {}

@mixin
intEnum IntMixin {
    FOO = 1
    BAR = 2
}

enum StringEnum with [StringMixin] {}

@mixin
enum StringMixin {
    BAZ = "baz"
}
