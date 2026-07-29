namespace smithy.protocols

@trait(selector: "member")
integer idx

structure Indexed {
    @idx(2)
    value: String
}
