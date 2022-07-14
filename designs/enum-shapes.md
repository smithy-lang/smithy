# Enum shapes in Smithy

* **Authors**: Michael Dowling, Jordon Phillips
* **Created**: 2022-02-14
* **Last updated**: 2022-06-22

## Abstract

This proposal introduces an `enum` shape and `intEnum` shape to Smithy to
expand the existing `@enum` trait capabilities.

## Motivation

There are two primary motivations:

1. Smithy does not provide the ability to define an enumerated integer, and
   many serialization formats serialize enums as integers rather than strings.
2. Enum values are similar to members, but defined in a less flexible way that
   requires special casing. Enums in Smithy have enum values that are very
   similar to members, but cannot be targeted by traits or filtered like other
   members. For example, an `@enum` trait supports the value, name,
   documentation, tags, and deprecated properties, of which documentation,
   tags, and deprecated provide functionality that is exactly equivalent to
   similarly named traits. In order to filter enums out of a model, model
   transformations need to write special-cased code to deal with enum traits
   rather than relying on the existing logic used to remove member shapes.

## Proposal

Two new simple shapes will be introduced in Smithy 2.0:

* `enum`: An enumerated type that is represented using string values
* `intEnum`: An enumerated type that is represented using integer values.

### Enum shape

The enum shape is used to represent a fixed set of string values. The following
example defines an enum shape:

```
enum Suit {
    DIAMOND
    CLUB
    HEART
    SPADE
}
```

Each value listed in the enum is a member that implicitly targets
`smithy.api#Unit`. The string representation of an enum member defaults to the
member name. The string representation can be customized by applying the
`@enumValue` trait. We will introduce syntactic sugar to assign the
`@enumValue` trait to enums and intEnums.

```
enum Suit {
    DIAMOND = "diamond"
    CLUB = "club"
    HEART = "heart"
    SPADE = "spade"
}
```

The above enum definition is exactly equivalent to:

```
enum Suit {
    @enumValue("diamond")
    DIAMOND

    @enumValue("club")
    CLUB

    @enumValue("heart")
    HEART

    @enumValue("spade")
    SPADE
}
```

Enums do not support aliasing; all values MUST be unique.

#### enum is  a specialization of string

Enums are considered open, meaning it is a backward compatible change to add
new members. Previously generated clients MUST NOT fail when they encounter an
unknown enum value. Client implementations MUST provide the capability of
sending and receiving unknown enum values.

To facilitate this behavior, the enum type is a subclass of the string type.
Any selector that accepts a string implicitly accepts an enum. For example, an
enum can be used in an `@httpHeader` or `@httpLabel`.

```
structure GetFooInput {
    @httpLabel
    suit: Suit
}
```

#### enums must define at least one member

Every enum shape MUST define at least one member.

#### enum members always have a value

If an enum member doesn't have an explicit `@enumValue` trait, an `@enumValue`
trait will be automatically added to the member where the trait value is the
member's name. This means that enum members that have no `@enumValue` trait
are indistinguishable from enum members that have the `@enumValue` trait
explicitly set.

The following model:

```
enum Suit {
    DIAMOND
    CLUB
    HEART
    SPADE
}
```

Is equivalent to:

```
enum Suit {
    DIAMOND = "DIAMOND"
    CLUB = "CLUB"
    HEART = "HEART"
    SPADE = "SPADE"
}
```

The value of an enum cannot be set to an empty string.

### intEnum shape

An intEnum is used to represent an enumerated set of integer values. The
members of intEnum MUST be marked with the @enumValue trait set to a unique
integer value. The following example defines an intEnum shape:

```
intEnum FaceCard {
    JACK = 1
    QUEEN = 2
    KING = 3
    ACE = 4
    JOKER = 5
}
```

#### intEnum is a specialization of integer

Like enums, intEnums are considered open, meaning it is a backward compatible
change to add new members. Previously generated clients MUST NOT fail when
they encounter an unknown intEnum value. Client implementations MUST provide
the capability of sending and receiving unknown intEnum values.

To facilitate this behavior, the intEnum type is a subclass of the integer
type. Any selector that accepts an integer implicitly accepts an intEnum.

Implementation note: In Smithy’s Java implementation, shape visitors will by
default dispatch to the integer shape when an intEnum is encountered. This
both makes adding enums backward compatible, and allows implementations that
do not support enums at all to ignore them.

#### intEnums must define at least one member

Every intEnum shape MUST define at least one member.

### Smithy taxonomy updates

Both enum and intEnum are considered simple shapes though they have members.

The current definition of aggregate shapes is:

> Aggregate types define shapes that are composed of other shapes. Aggregate
> shapes reference other shapes using members.

This will require updating the description of Aggregate Types to become:

> Aggregate types are shapes that can contain more than one value.

The member type in Smithy is currently classified as an aggregate type. Because
members will now be present in simple shapes, we will add a new shape type
called “member” to go alongside simple, aggregate, and service types.

## Smithy 1.0 → 2.0

The `@enum` trait will be deprecated in IDL 2.0, but not removed. This is to
make it easier to migrate to IDL 2.0.

The reference implementation will not automatically upgrade strings bearing
the `@enum` trait into enum shapes since it isn't possible to convert enum
traits that don't set the name property. Instead, a transformer will be added
that upgrades those shapes that can be upgraded.

Additionally, to make this change backwards compatible for exising code
generators, visitors in the reference implementation will call their parent
shape's visitor methods by default. `enum` shapes in the reference
implementation will always contain an `@enum` trait with properties filled
out based on the enum's members and their traits. To prevent this trait
from being serialized, a synthetic variant will be used.

## FAQ

### Why does the enum type dictate its serialization?

RPC systems require and open-world in order for them to evolve without
breaking end users. Previously generated clients need a way to handle
unknown enum values. Discarding unknown values when a client encounters a newly
introduced enum would be a regression in functionality. By making a distinct
intEnum and enum shape, client implementations can reliably receive and
round-trip unknown enum values because the unknown value is wire-compatible
with an integer or enum.

### Why do enums contain members, but you can’t define the shape they target?

Every value of an enum and intEnum is considered a member so that they have a
shape ID and can have traits, but the shape targeted by each enum member is
meaningless. Because of this, members of an enum defined in the IDL implicitly
target Smithy’s unit type, `smithy.api#Unit`.

When defined in the JSON AST, members MUST be defined to target
`smithy.api#Unit`.

### Why does intEnum require an explicit enumValue on every member?

The order and numbers assigned to enum values are extremely important, and
deriving their ordinals implicitly would not allow filtering members based on
the target audience of the model. It is very common in Smithy models to filter
out members from models based on the target audience (for example, internal
clients know about all enum members, private beta customers might know about
specific members, etc).
