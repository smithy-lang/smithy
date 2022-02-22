$version: "2.0"

namespace ns.foo

enum StringEnum {
    IMPLICIT_VALUE

    @enumValue(string: "explicit")
    EXPLICIT_VALUE

    @enumValue(string: "")
    EMPTY_STRING

    @enumDefault
    DEFAULT_VALUE

    @enumValue(int: 1)
    INT_VALUE

    @enumValue(string: "explicit")
    DUPLICATE_VALUE

    undesirableName
}

@enum([{
    name: "FOO"
    value: "FOO"
}])
enum EnumWithEnumTrait {
    BAR
}

enum MultipleDefaults {
    @enumDefault
    DEFAULT1

    @enumDefault
    DEFAULT2
}

enum DefaultWithExplicitValue {
    @enumDefault
    @enumValue(string: "foo")
    DEFAULT
}

intEnum IntEnum {
    IMPLICIT_VALUE

    @enumValue(int: 1)
    EXPLICIT_VALUE

    @enumValue(int: 0)
    ZERO

    @enumDefault
    DEFAULT_VALUE

    @enumValue(string: "foo")
    STRING_VALUE

    @enumValue(int: 1)
    DUPLICATE_VALUE

    @enumValue(int: 99)
    undesirableName
}
