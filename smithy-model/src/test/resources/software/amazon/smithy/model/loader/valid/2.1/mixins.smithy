$version: "2.1"

namespace smithy.example

@mixin
structure BaseMixin {
    id: String
    name: String
}

structure UsesBase with [BaseMixin] {
    description: String
}

@mixin
string StringMixin

string MixedString with [StringMixin]
