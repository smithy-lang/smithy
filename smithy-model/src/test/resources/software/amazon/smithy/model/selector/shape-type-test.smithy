$version: "2.0"

namespace smithy.example

enum Enum {
    FOO
}

string String

intEnum IntEnum {
    @enumValue(1)
    FOO
}

integer Integer
