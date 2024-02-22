$version: "2.0"

namespace test.smithy.traitcodegen.conflicts

/// The names of the members conflict with
/// the imported classes required for traits
@trait
structure hasMembersWithConflictingNames {
    toSmithyBuilder: toSmithyBuilder
    abstractTrait: AbstractTrait
}

//
//// Conflicts with static builder
//@private
//structure Builder {}
//

/// Conflicts with ToSmithyBuilder interface
@private
structure toSmithyBuilder {}

/// Conflicts with AbstractTrait base class
@private
structure AbstractTrait {}
