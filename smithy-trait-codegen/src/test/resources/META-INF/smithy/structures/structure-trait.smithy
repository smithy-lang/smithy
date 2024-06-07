$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure structureTrait {
    @required
    @pattern("^[^#+]+$")
    fieldA: String

    /// Some member documentation
    fieldB: Boolean

    @documentation("More documentation")
    fieldC: NestedA

    /// Documentation includes preformatted text that should not be messed with. This sentence should still be partially wrapped.
    /// For example:
    /// <pre>
    /// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    /// </pre>
    ///
    /// <ul>
    ///     <li> Lorem ipsum dolor sit amet, consectetur adipiscing elit Lorem ipsum dolor sit amet, consectetur adipiscing elit </li>
    ///     <li> Lorem ipsum dolor sit amet, consectetur adipiscing elit Lorem ipsum dolor sit amet, consectetur adipiscing elit </li>
    /// </ul>
    fieldD: ListD

    fieldE: MyMap

    fieldF: BigDecimal

    fieldG: BigInteger
}

@private
list ListD {
    member: String
}

@private
map MyMap {
    key: String
    value: String
}

@private
structure NestedA {
    @required
    fieldN: String

    fieldQ: Boolean

    fieldZ: NestedB

    fieldAA: NestedC
}

@private
enum NestedB {
    /// An A!
    A

    /// A B!
    B
}

@private
intEnum NestedC {
    /// An A!
    A = 1

    /// A B!
    B = 2
}
