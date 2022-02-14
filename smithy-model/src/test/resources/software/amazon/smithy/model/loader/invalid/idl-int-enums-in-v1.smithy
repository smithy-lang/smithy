// intEnum shapes may only be used with Smithy version 2 or later.
$version: "1.0"

namespace ns.foo

intEnum IntEnum {
    @enumValue(int: 1)
    FOO

    @enumValue(int: 2)
    BAR
}
