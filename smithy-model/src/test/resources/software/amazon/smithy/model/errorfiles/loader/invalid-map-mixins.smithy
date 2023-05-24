$version: "2.0"

namespace com.test

@mixin
map ValidMixin {
    key: String
    value: String
}

map ExtraMemberAndValidMixin with [ValidMixin] {
    foo: String
}

@mixin
map MissingMemberMixin {
    key: String
}

map ValidWithMissingMemberMixin with [MissingMemberMixin] {
    key: String
    value: String
}

map InvalidWithMissingMemberMixin with [MissingMemberMixin] {
    value: String
}

@mixin
map NoMemberMixin {

}

map ValidWithNoMemberMixin with [NoMemberMixin] {
    key: String
    value: String
}

map InvalidWithNoMemberMixin with [NoMemberMixin] {
    key: String
}

@mixin
map ExtraMemberMixin {
    key: String
    value: String
    foo: String
}

// This shape doesn't produce its own error for now
map WithExtraMemberMixin with [ExtraMemberMixin] {
    $key
    $value
}

map ElidingMemberFromNoMemberMixin with [NoMemberMixin] {
    $key
    $value
}
