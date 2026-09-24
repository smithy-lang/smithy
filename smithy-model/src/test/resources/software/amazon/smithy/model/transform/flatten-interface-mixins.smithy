$version: "2"
namespace smithy.example

// Simple interface mixin
@mixin(interface: true)
structure HasName {
    name: String
}

// Normal (non-interface) mixin
@mixin
structure NormalMixin {
    age: Integer
}

// Concrete structure using both
structure Concrete with [HasName, NormalMixin] {
    id: String
}

// Non-interface mixin that extends an interface mixin (transitive case)
@mixin
structure NonInterfaceChild with [HasName] {
    extra: String
}

// Uses non-interface child, should get HasName transitively
structure TransitiveUser with [NonInterfaceChild] {
    userId: String
}

// Interface mixin hierarchy: HasFullName extends HasName (both interface)
@mixin(interface: true)
structure HasFullName with [HasName] {
    lastName: String
}

// Concrete using the interface hierarchy
structure FullNameUser with [HasFullName] {
    email: String
}

// Another interface mixin extending HasName for diamond test
@mixin(interface: true)
structure HasAge with [HasName] {
    personAge: Integer
}

// Diamond: uses both HasFullName and HasAge, which both extend HasName
structure DiamondUser with [HasFullName, HasAge] {
    employeeId: String
}
