// Expected COLON(':') but found DOT('.')
$version: "2.1"

namespace smithy.example

structure Invalid {
    abc. member: String
}
