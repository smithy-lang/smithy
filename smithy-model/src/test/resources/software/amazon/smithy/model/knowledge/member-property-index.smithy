$version: "2.0"

namespace com.example

resource Resource1 {
    identifiers: {
        id: String
    }
    properties: {
        property: String
    }
    operations: [ChangeResource]
    update: UpdateResource
    read: GetResource
}

service Service {
    resources: [Resource1]
}

operation ChangeResource {
    input := {
        @required
        id: String

        @nestedProperties
        nested: ResourceDescription
    }
    output := {
        /// Model fails validation because the following member:
        /// 1) matches an identifier name, not a property name
        /// 2) is not an implicit or explicit identifier (no @required)
        /// 3) PropertyBindingIndex must report that the member should map to a property 'id' 
        ///    which it does not. Inex test ensures the index gives the proper answer over checking
        ///    the specific validation exception that end users will observe.
        /// Note: This test case also ensures that the index does not qualify ID in the output
        ///       as an identifier due to the proper usage in the input.
        id: String
    }
}

operation UpdateResource {
    input: ResourceStructure_1
    output: ResourceStructure_2
}

structure ResourceDescription {
    property: String
}

@readonly
operation GetResource {
    input := {
        @required
        id: String
    }
    output := {
        @nestedProperties
        resource: ResourceDescription
    }
}

structure ResourceStructure_1 {
    property: String

    @idempotencyToken
    token: String

    @required
    id: String

    @notProperty
    spurious: String
}

structure ResourceStructure_2 {
    @nestedProperties
    nested: ResourceDescription
}

