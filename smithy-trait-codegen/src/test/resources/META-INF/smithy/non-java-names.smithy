$version: "2.0"

namespace test.smithy.traitcodegen.names

/// Snake cased
@trait
structure snake_case_structure {
    snake_case_member: String
}

/// A camel-snake hybrid chimera
@trait
structure snake_camelStructure {
    camel_snakeMember: String
}
