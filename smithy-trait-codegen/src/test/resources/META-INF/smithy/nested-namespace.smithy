$version: "2.0"

namespace test.smithy.traitcodegen.nested

/// A trait that should be generated in a nested namespace
@trait
structure nestedNamespaceTrait {
    nested: NestedNamespaceStruct
}

@private
structure NestedNamespaceStruct {
    field: String
}
