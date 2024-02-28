$version: "2.0"

namespace test.smithy.traitcodegen.conflicts

// ====================
//  Name conflict tests
// ====================
// The following traits check to make sure that name conflicts between shapes and
// java classes used in the generated codegen code are correctly handled

/// The names of the members conflict with
/// the imported classes required for traits
@trait
structure hasMembersWithConflictingNames {
    toSmithyBuilder: toSmithyBuilder
    builder: Builder
    static: static
}

/// Conflicts with static `builder` name that is a reserved word
@private
structure Builder {}

/// Conflicts with java `static` keyword
@private
structure static {}

/// Conflicts with ToSmithyBuilder interface
@private
structure toSmithyBuilder {}

/// Conflicts with AbstractTrait base class
@trait
structure Abstract {}

