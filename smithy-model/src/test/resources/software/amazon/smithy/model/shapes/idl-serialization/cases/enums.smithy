$version: "2.0"

namespace ns.foo

intEnum IntEnum {
    @enumValue(
        int: 1
    )
    FOO
    @enumValue(
        int: 2
    )
    BAR
}

enum StringEnum {
    FOO
    BAR
}

enum StringEnumWithExplicitValues {
    @enumValue(
        string: "foo"
    )
    FOO
    @enumValue(
        string: "bar"
    )
    BAR
}
