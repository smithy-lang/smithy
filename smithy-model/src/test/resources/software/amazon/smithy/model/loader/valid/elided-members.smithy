$version: "2.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
    }
}

structure MyResourceIdentifiers for MyResource {
    $id
}

structure MyResourceIdentifiersWithTraits for MyResource {
    @pattern(".*")
    $id
}

@mixin
structure MixinStructure {
    foo: String
}

structure MixedStructure with [MixinStructure] {
    $foo
}

structure MixedStructureWithTraits with [MixinStructure] {
    @pattern(".*")
    $foo
}

structure MixedResourceStructure for MyResource with [MixinStructure] {
    $id
    $foo
}

structure MixedResourceStructureWithTraits for MyResource with [MixinStructure] {
    @pattern(".*")
    $id

    @pattern(".*")
    $foo
}

@mixin
structure MixinThatDefinesIdentifier {
    @required
    id: String
}

structure MixedStructureWhereMixinDefinesIdentifier for MyResource with [MixinThatDefinesIdentifier] {
    @pattern(".*")
    $id
}

@mixin
union MixinUnion {
    singleton: String
}

union MixedUnion with [MixinUnion] {
    $singleton
}

@mixin
list MixinList {
    member: String
}

list MixedList with [MixinList] {
    $member
}

@mixin
map MixinMap {
    key: String
    value: String
}

map MixedMap with [MixinMap] {
    $key
    $value
}
