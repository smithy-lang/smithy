// Parse error at line 17, column 41 near `for`: Expected: '{', but found 'f'
$version: "2.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
    }
}

@mixin
structure MixinStructure {
    foo: String
}

structure Invalid with [MixinStructure] for MyResource {
    $id
    $foo
}
