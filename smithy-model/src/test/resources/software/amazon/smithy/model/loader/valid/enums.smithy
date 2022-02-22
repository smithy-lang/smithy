$version: "2.0"

namespace smithy.example

enum EnumWithoutValueTraits {
    FOO
    BAR
    BAZ
}

enum EnumWithValueTraits {
    @enumValue(string: "foo")
    FOO

    @enumValue(string: "bar")
    BAR

    @enumValue(string: "baz")
    BAZ
}

enum EnumWithDefaultBound {
    @enumDefault
    DEFAULT
}

intEnum IntEnum {
    @enumValue(int: 1)
    FOO

    @enumValue(int: 2)
    BAR

    @enumValue(int: 3)
    BAZ
}

intEnum IntEnumWithDefaultBound {
    @enumDefault
    DEFAULT
}
