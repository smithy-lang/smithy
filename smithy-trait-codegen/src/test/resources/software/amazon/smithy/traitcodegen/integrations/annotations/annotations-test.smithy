$version: "2"

namespace com.example.annotations

@trait
structure HasSmithyGeneratedClass {
    nested: HasSmithyGeneratedNested
}

structure HasSmithyGeneratedNested {
    field: String
}

@trait
@deprecated
structure DeprecatedStructure {
    @deprecated
    deprecatedMember: String,

    /// Has docs in addition to deprecated
    @deprecated
    deprecatedWithDocs: String

    @deprecated
    deprecatedMap: MemberMap
}

map MemberMap {
    key: String
    value: Integer
}

@trait
@unstable
structure UnstableStructure {
    @unstable
    unstableMember: String,

    /// Has docs in addition to unstable
    @unstable
    unstableWithDocs: String
}

@trait
enum EnumWithAnnotations {
    @deprecated
    DEPRECATED,
    @unstable
    UNSTABLE
}

@trait
@deprecated
list DeprecatedList {
    member: Integer
}

@trait
@deprecated
map DeprecatedMap {
    key: String
    value: Integer
}
