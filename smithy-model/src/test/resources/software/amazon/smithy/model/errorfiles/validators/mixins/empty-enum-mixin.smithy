$version: "2.0"

namespace smithy.example

@mixin
enum EmptyEnumMixin {}

enum EmptyEnum with [EmptyEnumMixin] {}
