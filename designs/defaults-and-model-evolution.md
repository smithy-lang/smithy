# Defaults and Model Evolution

* **Author**: Michael Dowling
* **Created**: 2022-06-22


## Abstract

This document describes a fundamental change to the way Smithy describes
structure member optionality. By adding a `@default` trait to structure
members, code generators can generate non-optional accessors (or getters) for
members marked as `@default` or `@required` without sacrificing model
evolution (with some caveats for backward compatibility). While this proposal
is not AWS-specific, the practical impact of it is that when implemented, it
will convert thousands of optional property accessors to non-optional in
programming languages that model optionality in their type systems.

This proposal requires breaking changes to Smithy and proposes that the model
move to version 2.0.


## Motivation

Most structure member accessors generated from Smithy 1.0 models return
optional values. As new languages like Rust and Kotlin that explicitly model
optionality in their type systems adopt Smithy, excessive optionality in
generated code becomes burdensome to end-users because they need to call
methods like `.unwrap()` on everything. Generating every structure member as
optional makes it hard for customers to know when it is safe to dereference a
value and when it will result in an error. Adding the ability to control when
a value is optional vs when a value is always present provides an ergonomic and
safety benefit to end users of code generated types.

For example, after this proposal is implemented, the following Rust code:

```rust
println!("Lexicons:");
let lexicons = resp.lexicons.unwrap_or_default();
for lexicon in &lexicons {
    println!(
        "  Name:     {}",
        lexicon.name.as_deref().unwrap_or_default()
    );
    println!(
        "  Language: {:?}\n",
        lexicon
            .attributes
            .as_ref()
            .map(|attrib| attrib
                .language_code
                .as_ref()
                .expect("languages must have language codes"))
            .expect("languages must have attributes")
    );
}
println!("\nFound {} lexicons.\n", lexicons.len());
```

Can be simplified to:

```rust
println!("Lexicons:");
for lexicon in &resp.lexicons {
    println!(" Name:     {}",  lexicon.name);
    println!(" Languages: {:?}\n",
        lexicon.attributes.map(|attrib| attrib.language_code)
    );
}
println!("\nFound {} lexicons.\n", resp.lexicons.len());
```


## Background

Optionality in Smithy IDL 1.0 is controlled in the following ways:

1. Map keys, values in sets, lists with `@uniqueItems`, and unions cannot
   contain optional values. This proposal does not change this behavior.
2. List and map values can only contain optional values when marked with the
   `@sparse` trait. These kinds of collections rarely need to contain optional
   members. This proposal does not change this behavior.
3. The optionality of structure members in Smithy today is resolved using the
   following logic:
   1. If the member is marked with `@box`, it's optional.
   2. If the shape targeted by the member is marked with `@box`, it's optional.
   3. If the shape targeted by the member is a byte, short, integer, long,
      float, double, or boolean, it's non-optional because it has a default
      zero value.
   4. All other members are considered optional.


### Why was the `@required` trait unreliable for codegen?

Removing the `@required` trait from a structure member has historically been
considered backwards compatible in Smithy because it is loosening a restriction.
The `@required` trait in Smithy 1.0 is categorized as a constraint trait and
only used for validation rather than to influence generated types. This gives
service teams the flexibility to remove the required trait as needed without
breaking generated client code. The `@required` trait might be removed if new
use cases emerge where a member is only conditionally required, and more
rarely, it might be added if the service team accidentally omitted the trait
when the service initially launched.

For context, as of May 2021, the `@required` trait has been removed from a
structure member in 105 different AWS services across 618 different members.
Encoding the `@required` trait into generated types would have made changing a
member from required to optional a breaking change to generated code, which is
something most services try to avoid given its frequency and the cost of
shipping a major version of a service and client. While this data is
AWS-specific, it's indicative of how often disparate teams that use Smithy
(or its predecessors within Amazon) sought to update their APIs to make members
optional.


## Goals and non-goals

**Goals**

1. Reduce the amount of optionality in code generated from Smithy models.
2. Allow for similar model evolution guarantees that exist today like being
   able to backward compatibly remove the `@required` trait from a structure
   member without breaking previously generated clients.
3. Maintain Smithy's protocol-agnostic design. Protocols should never influence
   how types are generated from Smithy models; they're only intended to change
   how types are serialized and deserialized. In practice, this means that
   features of a protocol that extend beyond Smithy's metamodel and definitions
   of structure member optionality should not be exposed in code generated
   types that represent Smithy shapes.


## High-level summary

This proposal:

1. Introduces a `@default` trait
2. Introduces a `@clientOptional` trait
3. Removes the `@box` trait
4. Make the optionality of a structure completely controlled by members rather
   than the shape targeted by a member

The `@default` trait initializes a structure member with a value (note that IDL
v2 introduces syntactic sugar to structure members to define a `@default`
trait).

```
structure Message {
    @required
    title: String

    message: String = "Hello"
}
```

In the above example:

- `title` is marked as `@required`, so it is non-optional in generated code.
- `message` is also non-optional in generated code and is assigned a default
   value of `"Hello"` when not explicitly provided. This makes it easier to use
  `title` and `message` in code without having to first check if the value is
  `null`. In many languages, one can call `message.message.size()` without
  first checking if the value is non-optional.

If the `title` member ever needs to be made optional, the `@required` trait
can be replaced with the `@default` trait and `@addedDefault` trait:

```
structure Message {
    @addedDefault
    title: String = ""

    @addedDefault
    message: String = ""
}
```

With the above change, codegen remains the same: both values are non-optional.
However, if `title` is not set in client code, server-side validation for the
type will _not_ fail because `title` has a default value of `""`.

The `@addedDefault` trait is used to indicate that a default trait was added
to a member after initially publishing the member. This metadata can be used
by code generators to make an appropriate decision on whether using the default
value is backward compatible.


## Proposal overview

Introduce a 2.0 of Smithy models that simplifies optionality by moving
optionality controls from shapes to structure members. Smithy IDL 2.0 will:

1. Add a `@default` trait that can target structure members to assign a default
   value. Whether a member has a default value is no longer controlled based on
   the shape targeted by a member, localizing this concern to members. This
   makes optionality of a member easier to understand for both readers and
   writers.
2. Add a `@clientOptional` trait that can target structure members. The primary
   use case for this trait is to apply it to members also marked as `@required`
   to force non-authoritative code generators like clients to treat the member
   as optional. The service reserves the right to remove the `@required` trait
   from a member without replacing it with a `@default` trait without a major
   version bump of the service.
3. Remove the `@box` trait from the Smithy 2.0 prelude and fail to load models
   that contain the `@box` trait.


## `@default` trait

The `@default` trait can be applied to structure members to provide a default
value.

The following example defines a structure with a "language" member that has a
default value:

```
structure Message {
    @required
    title: String

    language: Language = "en"
}

enum Language {
    EN = "en"
}
```

The above example uses syntactic sugar to apply the `@default` trait. It is
semantically equivalent to:

```
structure Message {
    @required
    title: String

    @default("en")
    language: Language
}
```

The default trait can also be applied to root-level shapes to require that
all structure members that target the shape repeat its default value.

```
@default(0)
integer PrimitiveInteger

structure Foo {
    value: PrimitiveInteger = 0 // < repeating the default is required
}
```

This provides the same behavior of primitive root-level shapes in IDL 1.0, but
makes the default value more explicit on structure members, removing
action at a distance.

The default value of a target shape can be removed from a member by setting the
default value of the member to `null`. This indicates that the member has no
default value.

```
structure Baz {
    value: PrimitiveInteger = null
}
```

Note that this is equivalent to the following Smithy IDL 1.0 model:

```
structure Baz {
    @box
    value: PrimitiveInteger
}
```

All of the `Primitive*` shapes in the Smithy prelude now have corresponding
default values set to `0` for numeric types and `false` for `PrimitiveBoolean`.

The `@default` trait is defined in Smithy as:

```
/// Provides a structure member with a default value. When added to root level shapes, requires that every
/// targeting structure member defines the same default value on the member or sets a default of null.
///
/// This trait can currently only be used in Smithy 2.0 models.
@trait(
    selector: ":is(simpleType, list, map, structure > member :test(> :is(simpleType, list, map)))"
)
document default
```


### Default value constraints

The `@default` trait accepts a document type. The value of the trait MUST be
compatible with the shape targeted by the member and adhere to the following
constraints:

* The default value of an enum or intEnum MUST match one of the enum values.
* The default value of a string MUST be compatible with any length, enum, or
  pattern traits.
* The default value on a list or map MUST be compatible with a length trait,
  if present.
* The default value on a numeric type SHOULD be compatible with a range trait,
  if present. It was a common pattern in Smithy IDL 1.0 to define a numeric
  type with a default zero value, but require that the value be greater than
  zero. This specific validation is relaxed in order to not modify these types
  or need to drop the range constraint.

The following shapes have restrictions on their default values:

* enum: can be set to any valid string _value_ of the enum.
* intEnum: can be set to any valid integer _value_ of the enum.
* document: can be set to `true`, `false`, string, numbers, an empty list, or
  an empty map.
* list/set: can only be set to an empty list.
* map: can only be set to an empty map.
* structure: no default value.
* union: no default value.


### Updating default values

The default value of a root-level shape MUST NOT be changed since that would
break any shape that refers to the shape, and could break other models that
refer to a shape defined in a shared model.

The default value of a member that targets a shape with a default value
MUST NOT be removed (by changing the value to `null`) since that would
transition the member from non-optional to optional in generated code.

The default value of a member SHOULD NOT be changed. However, it MAY be
necessary in rare cases to change a default value. Changing default values can
result in parties disagreeing on the default value of a member because they are
using different versions of the same model.


### Readers MUST NOT differentiate from omitted or defaulted

When deserializing a structure, a reader SHOULD set members to their default
value if the member is missing. After deserializing a structure, there is no
discernible difference between an explicitly provided member or a defaulted
member. If such a distinction is needed for readers, then the `@default` trait
is inappropriate, and an optional member should be used instead.


### Optional error correction handling for non-authoritative readers

If a mis-configured server fails to serialize a value for a required member, to
avoid downtime, clients MAY attempt to error-correct the message by filling in
an appropriate default value for the member:

* boolean: false
* numbers: numeric zero
* timestamp: 0 seconds since the Unix epoch
* string and blob: an empty string or bytes
* document: a null document value
* list: an empty list
* map: an empty map
* enum, intEnum, union: The unknown variant. These types SHOULD define an
  unknown variant to account for receiving unknown members.
* union: The unknown variant. Client code generators for unions SHOULD
  define an unknown variant to account for newly added members.
* structure: an empty structure, if possible, otherwise a deserialization
  error.


### Default value serialization

1. All default values SHOULD be serialized. This ensures that messages are
   unambiguous, and ensures that messages do not change during deserialization
   if the default value for a member changes after the message was serialized.
2. To avoid information disclosure, implementations MAY choose to not serialize
   a default values if the member is marked with the `@internal` trait.
3. A member that is both `@default` and `@required` MUST be serialized.


### Impact on API design

The `@default` trait SHOULD NOT be used for partial updates or patch style
operations where it is necessary to differentiate between omitted values and
explicitly set values. To help guide API design, built-in validation will be
added to Smithy to detect and warn when `@default` members are detected in the
top-level input of operations that start with `Update`, operation bound to the
`update` lifecycle of a resource, or operations that use the `PATCH` HTTP
method.

For example, the following model:

```
$version: "2"
namespace smithy.examnple

operation UpdateUser {
    input: UpdateUserInput
}

structure UpdateUserInput {
    username: String = ""
}
```

Would emit a warning similar to the following:

```
WARNING: smithy.example#UpdateUserInput$userName (DefaultValueInUpdate)
     @ /path/to/main.smithy
     |
 4   | operation UpdateUser {
     | ^
     = This update style operation has top-level input members marked with the
       @default trait. It will be impossible to tell if the member was omitted
       or explicitly provided. Affected members: [UpdateUserInput$username].
```


## `@clientOptional` trait

For cases when a service unsure if a member will be required forever, the
member can be marked with the `@clientOptional` trait to ensure that
non-authoritative consumers of the model like clients treat the member as
optional. The `@required` trait can be backward compatibly removed from a
member marked as `@clientOptional` (and not replaced with the `@default`
trait). This causes the `@required` trait to function as server-side validation
rather than something that changes generated code.

Structure members in Smithy are automatically considered optional. For example,
the following structure:

```
structure Foo {
    baz: String
}
```

Is equivalent to the following structure:

```
structure Foo {
    @clientOptional
    baz: String
}
```

The primary use case of the `@clientOptional` trait is to indicate that while a
member is _currently_ defined as `@required`, the service reserves the right to
remove the `@required` trait and make the member optional in the future.

For example, the `@required` trait on `foo` in the following structure is
considered a validation constraint rather than a type refinement trait:

```
structure Foo {
    @required
    @clientOptional
    foo: String
}
```

The `@clientOptional` trait is defined in Smithy as:

```
@trait(selector: "structure > member")
structure clientOptional {}
```

The `@clientOptional` trait applied to a member marked with the `@default`
trait causes non-authoritative generators to ignore the `@default` trait:

```
structure Message {
    @clientOptional
    title: String = ""
}
```


### @input structures and @clientOptional

Members of a structure marked with the `@input` trait are implicitly considered
to be marked with `@clientOptional`. The `@input` trait special-cases a
structure as the input of a single operation that cannot be referenced in any
structures marked with the `@input` have more relaxed backward compatibility
guarantees. It is backward compatible to remove the `@required` trait from
top-level members of structures marked with the `@input` trait, and the
`@required` trait does not need to be replaced with the `@default` trait
(though this is allowed as well). This gives service teams the ability to
remove the `@required` trait from top-level input members and loosen
requirements without risking breaking previously generated clients.

The practical implication of this backward compatibility affordance is that
code generated types for members of an `@input` structure SHOULD all be
considered optional regardless of the use of `@required` or `@default`.
Generators MAY special-case members that serve as resource identifiers to be
non-nullable because those members can never remove the `@required` trait.
Not observing these optionality affordances runs the risk of previously
generated code breaking when a model is updated in the future.

Organizations that want stricter optionality controls over inputs can choose to
not use the `@input` trait.


## Backward compatibility rules

The key principle to consider is if adding or removing a trait will change the
optionality of a member in generated code. If it does, then the change is not
backward compatible. Backward compatibility rules of the `@default`,
`@required`, and `@clientOptional` traits are as follows:

- The `@default` trait can never be removed from a member.
- The value of the `@default` trait on a root-level shape MUST NOT be changed.
- The value of the `@default` trait on a member SHOULD NOT be changed unless
  absolutely necessary.
- The `@default` trait can only be added to a member if the member was
  previously marked as `@required` or `@clientOptional`. This ensures that
  generated code for the member remains non-optional.
- The `@addedDefault` trait SHOULD be added to structure members any time a
  `@default` trait is added to give more metadata to code generators so that
  they can generate backward compatible code. For example, if a generator only
  honors defaults that are set to the zero value of a type and do not use
  the `@required` trait to inform optionality, then adding a `@default` trait
  would introduce backward compatible type changes. These generators can use
  the `@addedDefault` trait to know to ignore the `@default` trait.
- The `@required` trait can only be removed under the following conditions:
  - It is replaced with the `@default` trait
  - The containing structure is marked with the `@input` trait.
  - The member is also marked with the `@clientOptional` trait.
- The `@required` trait can only be added to a member if the member is also
  marked with the `@clientOptional` trait. This is useful to correct a model
  that errantly omitted the `@required` trait, but the member is actually
  required by the service. Adding the `@required` trait to a member but
  omitting the `@clientOptional` trait is a breaking change because it
  transitions the member from optional to non-optional in generated code.
- The `@clientOptional` trait can only be removed from members that are not
  marked as `@required` or `@default`.

For example, if on _day-1_ the following structure is released:

```
structure Message {
    @required
    title: String

    message: String
}
```

On _day-2_, the service team realizes Message does not require a `title`, so
they add the `@default` trait and remove the `@required` trait. The member will
remain non-optional because clients, servers, and any other deserializer will
now provide a default value for the `title` member if a value is not provided by
the end user.

```
structure Message {
    title: String = ""

    message: String
}
```

Backward compatibility guarantees of the `@default` and `@required` traits
SHOULD be enforced by tooling. For example, smithy-diff in Smithy's reference
implementation will be updated to be aware of these additional backward
compatibility rules.


## Guidance on code generation

Code generated types for structures SHOULD use the `@default` trait and
`@required` trait to provide member accessors that always return non-optional
values based on the following ordered rules:

1. Accessors for members of a structure marked with the `@input` SHOULD be
   optional.
2. Accessors for members marked as `@clientOptional` MUST be optional.
3. Accessors for members marked as `@required` SHOULD always return a
   non-optional value.
4. Accessors for members marked with the `@default` trait SHOULD always return
   a non-optional value by defaulting missing members.
5. All other structure member accessors are considered optional.

**Note**: Smithy implementations in languages like TypeScript that do not
provide a kind of constructor or builder to create structures may not be able
to set default values, precluding them from being able to treat `@required`
and `@default` members as non-optional.


## AWS specific changes

Smithy comes from a long line of service frameworks built inside Amazon dating
back nearly 20 years. Until this proposal, the required trait was only treated
as server-side input validation. This causes two major issues that impacts the
AWS SDK team's ability to fully use the required trait for code generation:

1. Many teams did not consider the required trait as something that impacts
   output. This means that there are many members in AWS that should be
   marked as required but are not, and there are members that are required in
   input, but not always present in output.
2. Most teams designed their APIs under the guidance that the required trait
   can be removed without breaking their end users.

We will work to improve the accuracy and consistency of the required trait in
AWS API models where possible, but it will take significant time and effort
from hundreds of different teams within AWS. To accommodate this shift in
modeling changes, we will use the `@clientOptional` trait to influence code
generation and provide AWS SDK specific code generation recommendations.

`@clientOptional`, `@default`, and `@required` traits will be backfilled onto
AWS models as needed. Services with a history of frequently adding or removing
the required trait will apply the `@clientOptional` to every `@required`
member. Other AWS models will only apply the `@clientOptional` trait to members
that target structures or unions. Over time, as models are audited and
corrected, we can remove the `@clientOptional` trait and release improved
AWS SDKs.

Applying the `@clientOptional` trait on AWS models is admittedly be an
inconvenience for developers, but it is less catastrophic than previously
generated client code failing at runtime when deserializing the response of an
operation, and it matches the current reality of how AWS was modeled. While
it's true client code might start failing anyways if a service stops sending
output members that were previously marked as required, it would only fail when
the client explicitly attempts to dereference the value, and this kind of
change is generally only made when a client opts-into some new kind of
functionality or workflow. If an SDK fails during deserialization because a
previously required output member is missing, then customer applications would
be completely broken until they update their SDK, regardless of if the member
is used in client code.


## Alternatives and trade-offs

### Don't do anything

We could keep things as-is, which means we avoid a major version bump in the
Smithy IDL, and we don't require any kind of campaign to apply missing
`@required` traits. This option is low-risk, but does mean that we'll continue
to expose an excessive amount of optional values in Smithy codegen. This may
become more problematic as teams are beginning to use Smithy to model data types
outside of client/server interactions.


### Make everything optional

Another alternative for Smithy is to remove the `@required` trait and make
every structure member optional. This is essentially how many Smithy code
generators function today, and removing the `@required` trait would codify this
in the specification. However, the `@required` trait still provides value to
services because they have perfect knowledge of what is and is not required,
and therefore can automatically enforce that required properties are sent from
a client. It also provides value in documentation because it defines at that
point in time which members a caller must provide and which members they can
expect in a response.


### Only support default zero values instead of custom defaults

Only supporting the default zero value for structure members has some distinct
advantages, but also disadvantages. If we only supported default zero values,
then it would be possible to omit far more default values on the wire, because
any time a required member is missing, a deserializer can safely assume the
member transitioned from `@required` to `@default` and fill in the default
zero value. This helps to avoid unintended information disclosure and can
reduce payload sizes.

However, only supporting default zero values means that service teams will need
to add confusing enum values like `""` and `0` to enums and intEnums. It also
doesn't match the reality that default values are already ubiquitous; for
example, there likely more than 1,000 members in AWS that currently have
default values that are only captured in API documentation strings. If default
values are explicitly modeled, then they are easier to audit in API review
processes to ensure that a service team understands the implications of
changing a default value.


### Omit default values or implement presence tracking

If clients omit default values, then the default value for a member becomes
the complete discretion of servers. This allows services to change default
values for members and see the change take effect immediately with all
previously generated clients. This can be achieved by always omitting the
serialization of default values or through presence tracking.

If clients simply omit the serialization of default values, then it could be
problematic if a client actually wants to use whatever it thinks the current
default value for a member is instead of an updated default value on the
service. For example, if the client wants to use the default value of `10`,
but the service has since updated the default value to `11`, the client will
omit the serialization of `10` because it thinks that is the default, and the
server will use the newly updated default value of `11`. Until the client
updates to the latest version of the client, it is impossible for the client
to use the previous default value with the service.

Presence tracking is the act of tracking which structure members were set
explicitly to a value vs which structure members were set by a default value.
If a member was set by a default value, the client can omit the serialization
of the member and delegate the default value to the server. If the member was
explicitly set to a value, including the default value, the client will send
the default value to the server, ensuring that even if the default is changed,
the server will honor the client's choice. The major issue with presence
tracking is that it has steep complexity and size tradeoffs because it
requires tracking internal state changes on what could otherwise be stateless
types. Implementations are free to use presence tracking, though it isn't a
requirement of integrating with Smithy's default values.


## FAQ


### What's the biggest risk of this proposal?

Service teams not understanding the traits, and then accidentally making
breaking changes to Smithy generated SDKs. Or they potentially need to make a
breaking change in SDKs because they did not understand the traits. We can
mitigate this somewhat through validation and backward compatibility checks.


### Is the `@default` trait only used to remove the `@required` trait?

No. It can be used any time a member has a reasonable default value.


### When should the `@default` trait not be used?

The `@default` trait should not be used if the value used for a member is
dynamic (that is derived from other members, generated from other context,
etc).


### Why not allow deeply nested default values?

The `@default` trait does not provide a default value for structures and unions,
does not allow the default values for lists/sets/maps to be anything other than
empty lists/sets/maps, and does not allow non-empty lists or maps for document
types. This limitation is primarily to simplify code generation. For example,
constant values do not need to be created for default complex types and copied
each time a structure is created.

It is rare that a default other than an empty list or map is needed. In the
case of a structure or union, it's added complexity for little gain; if a
structure could potentially define a sensible default value, then just set the
member to that sensible value. Unions can easily add a new variant to represent
the lack of a value:

```
union MyUnion {
    None: Unit
}
```


### I need to add a member to output that is always present. How?

Even if the structure member is only ever seen by clients in output, it
is still a breaking change to add a member with the `@required` trait. For
example, previously written unit tests that create this type would now fail to
build if a new required member is added.

In these cases, use the `@default` trait even if the member is initialized with
a kind of zero value that will never be observed by clients because the server
will provide an actual non-zero value.


### How will this impact smithy-openapi?

We will start using the `default` property of OpenAPI and JSON Schema. For
example:

```
{
  "components": {
    "schemas": {
      "Cat": {
          {
            "type": "object",
            "properties": {
              "huntingSkill": {
                "type": "string",
                "default": ""
              }
            },
            "required": [
              "huntingSkill"
            ]
          }
        ]
      }
    }
  }
}
```

Because Amazon API Gateway does not support the `default` property (as of
June, 2022), it is automatically stripped when generating OpenAPI models for
Amazon API Gateway. If `default` support is added, it could be something
enabled via an API Gateway specific opt-in flag.


### How many AWS members used in output marked as required?

As of March 17, 2021, 4,805 members.


### How often has the `@required` trait been removed from members in AWS?

The required trait has been removed from a structure member 618 different times
across 105 different services. Of those removals, 75 targeted structures, 350
targeted strings, and the remainder targeted lists, maps, and other simple
shapes.


### How often has the `@required` trait been added to members in AWS?

The required trait has been added to a member 9 different times across 5
different services.


### What are the `@input` and `@output` traits?

See https://github.com/awslabs/smithy/blob/main/designs/operation-input-output-and-unit-types.md
