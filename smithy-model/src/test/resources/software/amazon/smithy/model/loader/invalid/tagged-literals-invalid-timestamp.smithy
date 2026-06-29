// Invalid RFC 3339 timestamp
$version: "2.1"

namespace smithy.example

structure Foo {
    @default(#timestamp "not-a-date")
    when: Timestamp
}
