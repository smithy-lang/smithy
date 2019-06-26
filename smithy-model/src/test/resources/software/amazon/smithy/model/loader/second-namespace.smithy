$version: "0.2.0"

use shape smithy.example1#Foo
use shape smithy.example1#Baz
use shape smithy.example1#Bar
use trait smithy.example1#trait1
use trait smithy.example1#trait2
use trait smithy.example1#trait3

namespace smithy.example2

@trait1
@trait2
@trait3
structure MyStruct {
    foo: Foo,
    baz: Baz,
    bar: Bar,
}
