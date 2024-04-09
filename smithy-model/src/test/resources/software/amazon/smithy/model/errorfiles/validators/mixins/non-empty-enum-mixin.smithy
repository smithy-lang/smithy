$version: "2.0"

namespace smithy.example

@mixin
enum NonEmptyEnumMixin {
    VALUE = "value"
}

enum NonEmptyEnum with [NonEmptyEnumMixin] {}
