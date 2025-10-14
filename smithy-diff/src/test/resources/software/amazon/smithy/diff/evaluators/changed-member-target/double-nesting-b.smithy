$version: "2.0"

namespace smithy.example


list A {
    member: NewB
}

list NewB {
    member: NewC
}

list NewC {
    @pattern("foo:.*")
    member: String
}
