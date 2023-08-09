$version: "2.0"

metadata suppressions = [
    {
        id: "ChangedMemberOrder"
        namespace: "smithy.example"
    }
]

namespace smithy.example

structure Foo {
    a: String
    b: String
    c: String
}
