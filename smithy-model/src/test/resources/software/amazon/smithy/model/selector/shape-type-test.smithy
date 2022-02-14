$version: "2.0"

namespace smithy.example

enum Enum {
    FOO
}

string String

intEnum IntEnum {
    @enumValue(int: 1)
    FOO
}

integer Integer
