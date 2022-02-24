$version: "2.0"

namespace ns.foo

intEnum IntEnum {
    @enumValue(1)
    FOO
    @enumValue(2)
    BAR
}

enum StringEnum {
    FOO
    BAR
}

enum StringEnumWithExplicitValues {
    @enumValue("foo")
    FOO
    @enumValue("bar")
    BAR
}
