$version: "2.0"

namespace smithy.example

@mixin
structure A {}

@mixin
structure B {}

structure C with [
   A ,,,


   ,    B ,,, ,    ] {}
