$version: "2.0"

namespace smithy.example

list MyList {
    member: String
}

list MyList2 {
    /// Hello
    member: String
}

list MyList3 {
    @length(min: 1)
    member: String
}

list MyList4 {
    // Comment
    member: String
}

map MyMap1 {
    key: String
    value: MyList3
}

map MyMap2 {
    /// Docs 1
    key: String

    /// Docs 2
    value: MyList3
}

structure Empty {}

structure OneMember {
    foo: String
}

structure OneMemberWithDefault {
    foo: String = ""
}

structure TwoMembers {
    foo: String
    bar: String
}

structure TwoMembersWithOneDefault {
    foo: String = ""
    bar: String
}

structure TwoMembersWithDefaults {
    foo: String = ""
    bar: String = ""
}

structure ThreeMembersWithTraits {
    /// foo
    foo: String = ""

    @required
    @deprecated
    bar: String = ""

    /// Docs 1
    /// Docs 2
    @required
    baz: String
}

@mixin
structure MyMixin1 {}

@mixin
structure MyMixin2 {}

structure HasMixins with [MyMixin1, MyMixin2] {
    greeting: String
}

structure HasForResource for MyResource with [MyMixin1] {}

resource MyResource {
    operations: [PutMyResource]
}

operation PutMyResource {
    input := {}
    output: PutMyResourceOutput
}

structure PutMyResourceOutput {}
