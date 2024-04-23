$version: "2.0"

namespace test.smithy.traitcodegen.names

// ===================
//  Non-java-name test
// ===================
// The following traits check that non-java-style names are
// correctly changed into a useable Java-compatible name
/// Snake cased
@trait
structure snake_case_structure {
    snake_case_member: String
}
