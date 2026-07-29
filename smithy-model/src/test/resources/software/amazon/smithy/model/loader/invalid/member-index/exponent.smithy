// Member indexes must match [1-9][0-9]*
$version: "2.1"

namespace smithy.example

structure Invalid {
    1e2. member: String
}
