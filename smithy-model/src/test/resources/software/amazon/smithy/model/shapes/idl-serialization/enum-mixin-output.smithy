$version: "2.1"

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
