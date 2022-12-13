$version: "2.0"

namespace smithy.example

intEnum IntEnum {
    @enumValue(1.1)
    FLOAT

    @enumValue([1])
    ARRAY

    @enumValue({"foo": "bar"})
    MAP

    @enumValue(null)
    NULL

    @enumValue(true)
    BOOLEAN
}
