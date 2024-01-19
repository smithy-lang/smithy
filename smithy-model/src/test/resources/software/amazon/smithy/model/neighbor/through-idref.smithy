$version: "2.0"

namespace com.foo

service FooService {
    version: "2024-01-18"
    operations: [GetFoo]
}

operation GetFoo {
    input := {
        one: One
        two: Two
        three: Three
        four: Four
        five: Five
        six: Six
        seven: Seven
        eight: Eight
        nine: Nine
        ten: Ten
        eleven: Eleven
    }
}

// --
@trait
structure withIdRefOnMember {
    @idRef(failWhenMissing: true)
    ref: String
}

@withIdRefOnMember(ref: Ref1)
structure One {}

structure Ref1 {}

// --
@trait
structure withIdRefOnMemberTarget {
    ref: OnTarget
}

@idRef(failWhenMissing: true)
string OnTarget

@withIdRefOnMemberTarget(ref: Ref2)
structure Two {}

structure Ref2 {}

// --
@trait
structure withIdRefOnNestedStructureMember {
    struct: Nested
}

structure Nested {
    @idRef(failWhenMissing: true)
    member: String
}

@withIdRefOnNestedStructureMember(
    struct: {
        member: Ref3
    }
)
structure Three {}

structure Ref3 {}


// --
@trait
list withIdRefOnListMemberTarget {
    member: OnListMemberTarget
}

@idRef(failWhenMissing: true)
string OnListMemberTarget

@withIdRefOnListMemberTarget([
    Ref4
])
structure Four {}

structure Ref4 {}

// --
@trait
@idRef(failWhenMissing: true)
string withIdRefOnSelf

@withIdRefOnSelf(Ref5)
structure Five {}

structure Ref5 {}

// --
@withIdRefOnSelf(Ref6)
structure Six {}

structure Ref6 {
    ref: RefByRef6
}

structure RefByRef6 {}

// --
@trait
list withIdRefOnNestedStruct {
    member: Nested
}

@withIdRefOnNestedStruct([
    {
        member: Ref7
    }
])
structure Seven {}

structure Ref7 {}

// --
@trait
structure withIdRefThroughMixin with [ThroughMixin] {}

@mixin
structure ThroughMixin {
    @idRef(failWhenMissing: true)
    ref: String
}

@withIdRefThroughMixin(ref: Ref8)
structure Eight {}

structure Ref8 {}

// --
@trait
map withIdRefOnMapValue {
    key: String
    value: OnMap
}

@idRef(failWhenMissing: true)
string OnMap

@withIdRefOnMapValue({
    foo: Ref9
})
structure Nine {}

structure Ref9 {}

// --
@trait
map withIdRefOnNestedMapValue {
    key: String
    value: Nested
}

@withIdRefOnNestedMapValue({
    foo: {
        member: Ref10
    }
})
structure Ten {}

structure Ref10 {}

// --
@trait
map withIdRefOnMapKey {
    key: OnMap
    value: String
}

@withIdRefOnMapKey({
    "com.foo#Ref11": "foo"
})
structure Eleven {}

structure Ref11 {}
