$version: "2.0"

namespace smithy.example

use smithy.example1#trait1

@trait
@tags(["filter"])
structure filteredTrait {}

@trait
@tags(["unfiltered"])
structure unfilteredTrait {}

@trait(selector: "resource")
structure resourceTrait {}

// RESOURCES
@filteredTrait
@unfilteredTrait
resource ResourceWithMixin with [ ResourceMixin ]  {}

@mixin
@resourceTrait
resource ResourceMixin {}

// OPERATIONS
@filteredTrait
@unfilteredTrait
operation OperationWithMixin with [ OperationMixin ] {
    input := {
        @required
        @httpLabel
        myInputField: String,
        other: String
    }
}

@mixin
operation OperationMixin {
    errors: [
        ThrottlingException
        ValidationException
    ]
}

@error("client")
structure ThrottlingException {
    @required
    message: String
}

@error("client")
structure ValidationException{
    @required
    message: String
}

// STRUCTURES
@filteredTrait
@unfilteredTrait
structure StructureWithMixin with [ StructureMixin ] {
    data: String
}

@mixin
structure StructureMixin {
    field: String
}

// UNIONS
@filteredTrait
@unfilteredTrait
union UnionWithMixin with [ UnionMixin ] {
    data: String
}

@mixin
union UnionMixin {
    field: Integer
}

// MAPS
@filteredTrait
@unfilteredTrait
map MapWithMixin with [MapMixin]{
    key: String
    value: String
}

@mixin
@length(min: 1, max: 2)
map MapMixin {
    key: String
    value: String
}

// LISTS
@filteredTrait
@unfilteredTrait
list ListWithMixin with [ListMixin]{
    member: String
}

@mixin
@length(min: 1, max: 2)
list ListMixin {
    member: String
}

// STRINGS
@filteredTrait
@unfilteredTrait
string StringWithMixin with [ StringMixin ]

@pattern("^$")
@mixin
string StringMixin

