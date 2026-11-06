// A version that omits the minor version resolves to the latest known minor
// version of that major version, so "1" loads as 1.0.
//
// Note that when serializing the AST, this library always serializes the
// version of the tool that produced the AST rather than echoing back the
// version declared here. This is required because multiple IDL models may
// have been loaded all using different, but compatible version numbers.
$version: "1"
