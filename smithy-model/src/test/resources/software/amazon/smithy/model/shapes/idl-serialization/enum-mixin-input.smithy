$version: "2.0"

namespace ns.foo

@mixin
intEnum IntMixin {
    FOO = 1
    BAR = 2
}

intEnum IntEnum with [IntMixin] {
    FOO = 1
    BAR = 2
}

@mixin
enum StringMixin {
    BAZ = "baz"
}

enum StringEnum with [StringMixin] {
    BAZ = "baz"
}
