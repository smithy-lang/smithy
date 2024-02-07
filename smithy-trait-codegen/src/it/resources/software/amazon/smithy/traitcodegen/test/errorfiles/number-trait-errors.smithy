$version: "2.0"

namespace test.smithy.traitcodegen

// Should fail to use string input for number traits
@HttpCodeInteger("bad")
@HttpCodeFloat("bad")
@HttpCodeLong("bad")
@HttpCodeShort("bad")
@HttpCodeDouble("bad")
structure structWithInvalidStringInput {}


