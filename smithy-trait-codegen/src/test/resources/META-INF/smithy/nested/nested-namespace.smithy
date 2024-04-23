$version: "2.0"

namespace test.smithy.traitcodegen.nested

// =======================
//  Nested namespace tests
// =======================
// The following traits check to make sure that traits within a nested smithy
// namespace are mapped to a nested java namespace
/// A trait that should be generated in a nested namespace
@trait
structure nestedNamespaceTrait {
    nested: NestedNamespaceStruct
}

@private
structure NestedNamespaceStruct {
    field: String
}
