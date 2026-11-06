$version: "2"

namespace com.example

use foo.baz#A
use foo.baz#B
use foo.baz#C

structure Dependant {
    a: A,
    b: B,
    c: C,
}

