$version: "2.0"

namespace test.smithy.traitcodegen.idref

// The following trait check to make sure that Strings are converted to ShapeIds
// when an @IdRef trait is added to a string
@trait
@idRef
string IdRefString
