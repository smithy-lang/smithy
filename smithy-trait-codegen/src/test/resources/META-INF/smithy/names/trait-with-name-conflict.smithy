$version: "2.0"

namespace test.smithy.traitcodegen.names

// The following traits check to make sure that name conflicts between shapes and
// java classes used in the generated codegen code are correctly handled
/// Conflicts with AbstractTrait base class
@trait
structure Abstract {}
