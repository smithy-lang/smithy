// intEnum shapes cannot be used in Smithy version 1.0
$version: "1.0"

namespace ns.foo

intEnum IntEnum {
    @enumValue(1)
    FOO

    @enumValue(2)
    BAR
}
