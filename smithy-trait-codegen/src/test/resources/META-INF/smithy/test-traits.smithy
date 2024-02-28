$version: "2.0"

namespace test.smithy.traitcodegen

// ===============
//  Simple traits
// ===============

/// A basic annotation trait
@trait
structure basicAnnotationTrait {}

/// Simple String trait
@trait
string stringTrait

// ===============
//  Number traits
// ===============
@trait
@tags(["filterOut"])
integer HttpCodeInteger

@trait
long HttpCodeLong

@trait
short HttpCodeShort

@trait
float HttpCodeFloat

@trait
double HttpCodeDouble

@trait
byte HttpCodeByte

@trait
bigDecimal HttpCodeBigDecimal

@trait
bigInteger HttpCodeBigInteger

// ===========
// List traits
// ===========
/// A list with only a simple string member
@trait
list StringListTrait {
    member: String
}

@trait
list NumberListTrait {
    member: Integer
}

@trait
list StructureListTrait {
    member: listMember
}

@private
structure listMember {
    a: String
    b: Integer
    c: String
}

// ========================
// Unique List (set) traits
// ========================

@trait
@uniqueItems
list StringSetTrait {
    member: String
}

@trait
@uniqueItems
list NumberSetTrait {
    member: Integer
}

@trait
@uniqueItems
list StructureSetTrait {
    member: listMember
}

// ===========
// Map traits
// ===========
/// Map of only simple strings. These are handled slightly differently than
/// other maps
@trait
map StringStringMap {
    key: String
    value: String
}

@trait
map StringToStructMap {
    key: String
    value: MapValue
}

@private
structure MapValue {
    a: String
    b: Integer
}

// ===============
//  Structure traits
// ===============
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
}

@private
enum NestedB {
    /// An A!
    A
    /// A B!
    B
}

// ===============
//  Enum traits
// ===============
@trait
enum ResponseType {
    /// Positive response
    YES = "yes"

    /// Negative response
    NO = "no"
}

// ===============
//  IntEnum traits
// ===============
@trait
intEnum ResponseTypeInt {
    /// Positive response
    YES = 1

    /// Negative response
    NO = 2
}

// ===============
//  Document traits
// ===============
@trait
document JsonMetadata

@trait
structure structWithNestedDocument {
    doc: nestedDoc
}

@private
document nestedDoc

// ==================
//  Timestamp traits
// ==================

@trait
structure structWithNestedTimestamp {
    time: nestedTimestamp
}

@private
timestamp nestedTimestamp
