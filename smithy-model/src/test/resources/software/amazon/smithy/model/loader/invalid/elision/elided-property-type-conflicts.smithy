// smithy.example#Invalid$foo: Member conflicts with an inherited mixin member: `smithy.example#MixinStructure$foo`
$version: "2.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
    }
    properties: {
        foo: Integer
    }
    create: CreateMyResource
}

operation CreateMyResource {
    input := {
        foo: Integer
    }
    output := { }
}

@mixin
structure MixinStructure {
    foo: String
}

structure Invalid for MyResource with [MixinStructure] {
    $foo
}
