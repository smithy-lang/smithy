$version: "2.0"

namespace smithy.example

structure RecursiveShape1 {
    @required
    recursiveMember: RecursiveShape2
}

structure RecursiveShape2 {
    // Bad
    @required
    recursiveMember: RecursiveShape1,

    // Ok
    recursiveMember2: RecursiveShape1
}
