// smithy.example#Invalid$id: Member conflicts with an inherited mixin member: `smithy.example#MixinStructure$id`
$version: "2.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
    }
}

@mixin
structure MixinStructure {
    id: Integer
}

structure Invalid for MyResource with [MixinStructure] {
    $id
}
