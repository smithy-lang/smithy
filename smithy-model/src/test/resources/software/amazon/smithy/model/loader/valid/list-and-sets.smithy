namespace com.example

string String

// ----- lists ------
list A {
    member: smithy.api#String
}

list B {
    member: String
}

@deprecated
list C {
    member: String
}

@deprecated
@since("1.0")
list D {
    member: String
}

@deprecated
@since("1.0")
list E {
    @internal
    @since("1.1")
    member: String
}

@deprecated
@since("1.0")
list F {
    @internal
    @since("1.1")
    member: String
}

@deprecated
@since("1.0")
list G {
    @internal
    @since("1.1")
    member: String
}

// ---- sets -----
list H {
    member: smithy.api#String
}

list I {
    member: String
}

@deprecated
list J {
    member: String
}

@deprecated
@since("1.0")
list K {
    member: String
}

@deprecated
@since("1.0")
list L {
    @internal
    @since("1.1")
    member: String
}

@deprecated
@since("1.0")
list M {
    @internal
    @since("1.1")
    member: String
}

@deprecated
@since("1.0")
list N {
    @internal
    @since("1.1")
    member: String
}

