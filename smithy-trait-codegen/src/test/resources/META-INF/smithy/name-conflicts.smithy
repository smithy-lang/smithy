$version: "2.0"

namespace test.smithy.traitcodegen

/// The names of the members conflict with
/// the imported classes required for traits
@trait
structure hasMembersWithConflictingNames {
    staticBuilder: Builder
    toSmithyBuilder: toSmithyBuilder

}

// Conflicts with static builder
@private
structure Builder {}

// Conflicts with ToSmithyBuilder interface
@private
structure toSmithyBuilder {}
