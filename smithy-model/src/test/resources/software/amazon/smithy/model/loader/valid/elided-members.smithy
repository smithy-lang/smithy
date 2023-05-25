$version: "2.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
        key: String
    }
    properties: {
        property: String
        member: String
        value: String
    }
    operations: [ThrowAway]
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

structure MixedResourceWithPropertiesStructure for MyResource with [MixinStructure] {
    $id
    $foo
    $property
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

union MixedUnionWithTraits with [MixinUnion] {
    @pattern(".*")
    $singleton
}

union ResourceUnion for MyResource {
    $id
    $property
}

union MixedResourceUnion for MyResource with [MixinUnion] {
    $id
    $singleton
    $property
}

union MixedResourceUnionWithTraits for MyResource with [MixinUnion] {
    @pattern(".*")
    $id

    @pattern(".*")
    $property

    @pattern(".*")
    $singleton
}

@mixin
list MixinList {
    member: String
}

list MixedList with [MixinList] {
    $member
}

list MixedListWithTraits with [MixinList] {
    @pattern(".*")
    $member
}

list ResourceList for MyResource {
    $member
}

list MixedResourceList for MyResource with [MixinList] {
    $member
}

list MixedResourceListWithTrait for MyResource with [MixinList] {
    @pattern(".*")
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

map MixedMapWithTraits with [MixinMap] {
    @pattern(".*")
    $key

    @pattern(".*")
    $value
}

map ResourceMap for MyResource {
    $key
    $value
}

map MixedResourceMap for MyResource with [MixinMap] {
    $key
    $value
}

map MixedResourceMapWithTraits for MyResource with [MixinMap] {
    @pattern(".*")
    $key

    @pattern(".*")
    $value
}

/// Operation needed to utilize property for validity
operation ThrowAway {
    input := {
        @required
        id: String
        @required
        key: String
        property: String
        member: String
        value: String
    }
    output := {}
}

