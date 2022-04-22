namespace smithy.example

structure RecursiveShape1 {
    @required
    recursiveMember: RecursiveShape2
}

structure RecursiveShape2 {
    @required
    recursiveMember: RecursiveShape1
}
