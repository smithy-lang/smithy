// Expected DOT('.') but found SPACE(' ')
$version: "2.1"

namespace smithy.example

structure Invalid {
    1 . member: String
}
