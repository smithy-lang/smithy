$version: "2.0"

namespace test.smithy.traitcodegen

/// A basic annotation trait
@trait(selector: "structure > :test(member > string)")
structure basicAnnotationTrait {}

/// Simple String trait
@trait(selector: "member")
string stringTrait

// ===============
//  Number traits
// ===============
@trait
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

// ==================
//  Deprecation tests
// ==================
/// Checks that a deprecated annotation is added to deprecated traits
@deprecated(since: "a long long time ago", message: "because you should stop using it")
@trait
string DeprecatedStringTrait

// ==================
//  Deprecation tests
// ==================
/// The following traits check to make sure that Strings are converted to ShapeIds
/// when an @IdRef trait is added to a string
@trait
@idRef
string IdRefString

@trait
list IdRefList {
    member: IdRefmember
}

@trait
map IdRefMap {
    key: String
    value: IdRefmember
}

@trait
structure IdRefStruct {
    fieldA: IdRefmember
}

@trait
structure IdRefStructWithNestedIds {
    @required
    idRefHolder: NestedIdRefHolder

    idList: NestedIdList

    idMap: NestedIdMap
}

@private
structure NestedIdRefHolder {
    @required
    id: IdRefmember
}

@private
list NestedIdList {
    member: IdRefmember
}

@private
map NestedIdMap {
    key: String
    value: IdRefmember
}

@private
@idRef
string IdRefmember
