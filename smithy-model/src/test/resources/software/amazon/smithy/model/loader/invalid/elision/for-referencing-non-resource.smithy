// The target of the `for` production must be a resource shape, but found a structure shape: smithy.example#NotAResource
$version: "2.0"

namespace smithy.example

@mixin
structure NotAResource {
    id: Integer
}

structure Invalid for NotAResource {
    $id
}
