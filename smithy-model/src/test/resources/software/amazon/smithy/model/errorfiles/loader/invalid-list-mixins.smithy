$version: "2.0"

namespace com.test

@mixin
list ValidMixin {
    member: String
}

list ExtraMemberAndValidMixin with [ValidMixin] {
    foo: String
}

@mixin
list NoMemberMixin {

}

list ValidWithNoMemberMixin with [NoMemberMixin] {
    member: String
}

list InvalidWithNoMemberMixin with [NoMemberMixin] {

}

@mixin
list ExtraMemberMixin {
    member: String
    foo: String
}

// This shape doesn't produce its own error for now
list WithExtraMemberMixin with [ExtraMemberMixin] {
    $member
}

list ElidingMemberFromNoMemberMixin with [NoMemberMixin] {
    $member
}
