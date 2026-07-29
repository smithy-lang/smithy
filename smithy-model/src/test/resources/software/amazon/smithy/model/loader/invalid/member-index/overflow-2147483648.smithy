// Member indexes must be less than or equal to 2147483647
$version: "2.1"

namespace smithy.example

structure Invalid {
    2147483648. member: String
}
