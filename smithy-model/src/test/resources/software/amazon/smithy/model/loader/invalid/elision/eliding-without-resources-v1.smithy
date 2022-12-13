// Members can only elide targets in IDL version 2 or later
$version: "1.0"

namespace smithy.example

// To be valid, this should actually be inheriting from some mixin. But if we
// put the "with" syntax in then that validation would trigger before this
// validation does.
structure Invalid {
    $foo
}
