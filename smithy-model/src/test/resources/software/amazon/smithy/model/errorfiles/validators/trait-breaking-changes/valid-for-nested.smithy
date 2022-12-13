$version: "2.0"

namespace smithy.example

@trait(breakingChanges: [{path: "/a/b/member/c", change: "any"}])
structure a {
    a: B
}

@private
union B {
    b: CList
}

list CList {
    member: C
}

structure C {
    c: String
}
