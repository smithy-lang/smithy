# Discriminated Unions

* **Authors**: Manuel Sugawara
* **Created**: 2026-07-02
* **Updated**: 2026-10-27

## Abstract

This proposal introduces a `@discriminated` trait for union shapes in Smithy. When applied, the union is serialized in
JSON as a flat object with a designated discriminator field that identifies the active variant, rather than the standard
externally-tagged encoding. This enables Smithy to model internally-tagged union patterns common in legacy APIs and data
interchange formats.

## Motivation

The existing Smithy union type uses an externally-tagged encoding where the member name acts as the sole key in a JSON
object wrapping the value:

```json
{"circle": {"radius": 5}}
```

Many existing APIs and data formats instead use an internally-tagged pattern where a discriminator field is inlined
alongside the variant's fields:

```json
{"type": "circle", "radius": 5}
```

This pattern appears extensively in production systems including AWS error responses (`__type`), OpenAPI's
`discriminator` construct, and countless existing JSON APIs. Today, Smithy has no way to model these types
directly. Service teams wanting to adopt Smithy for existing APIs that use this pattern must resort to workarounds such
as modeling the union as a structure with a freeform document member, losing type safety and code generation benefits.

Shape-only code generation (generating types without a full SDK for I/O) further motivates this feature. Customers want
to represent legacy data formats as strongly-typed Smithy shapes and serialize/deserialize them without building a
complete client or server. The discriminated union is one of the most common patterns that cannot currently be
expressed.

## Proposal

A new `@discriminated` trait is introduced that can be applied to union shapes. The trait takes a single optional string
value that names the discriminator field used on the wire. When the value is omitted, the discriminator field is named
`__type`, matching the field AWS protocols already use to identify error types.

```smithy
@trait(selector: "union")
@default("__type")
@length(min: 1)
string discriminated
```

The trait is a string rather than a structure so that the common case is concise: `@discriminated("type")` names the
field explicitly, and a bare `@discriminated` is equivalent to `@discriminated("__type")` (see "Why does a bare
`@discriminated` need special handling?"). The value may be any non-empty string, like `@jsonName`: the flat encoding is
used with JSON, where any string is a valid field name, and the legacy APIs this trait targets use names such as `@type`
and `@odata.type`.

The definition above is abbreviated. The trait is introduced as `@unstable` (see "Optional support"), and it conflicts
with `@streaming`, because event streams define their own message framing and do not use this encoding. Adding,
removing, or changing the trait changes the serialized form of the union, so any change to it is a breaking change, as
it is for `@jsonName`.

When a union is marked with `@discriminated`, its serialization changes from the standard tagged-union encoding to a
flat object encoding where the discriminator field identifies the active member by its member name, and the targeted
structure's fields are inlined as siblings.

### Example

```smithy
@discriminated("type")
union Shape {
    circle: CircleData
    rectangle: RectangleData
}

structure CircleData {
    @required
    radius: Integer
}

structure RectangleData {
    @required
    width: Integer
    @required
    height: Integer
}
```

An instance of `Shape` with the `circle` variant active serializes in JSON as:

```json
{"type": "circle", "radius": 5}
```

Rather than the standard union encoding:

```json
{"circle": {"radius": 5}}
```

### Constraints on member targets

All members of a discriminated union MUST target structure shapes. Members MUST NOT target union shapes, simple types,
lists, maps, or the Unit type. This constraint exists because the discriminator field must coexist with the variant's
fields in the same flat object, which is only well defined when the variant is a structure with named fields. A union
has no fields of its own to place alongside the discriminator: inlining a standard union would add its member name as
a sibling key, and inlining a discriminated union would put two discriminators in a single object, each needing its own
collision rules and decoding. To use a union as a variant, wrap it in a structure that has a member targeting the union.
That member is serialized as a nested object according to the union's own rules (see "Can discriminated unions be
nested?").

This restriction is deliberately conservative. Allowing union targets in the future would make additional models valid
without changing the meaning of any existing model.

The `@jsonName` trait has no effect on the members of a discriminated union, because the discriminator value is always
the member name (see "How does this interact with jsonName?"). A validator SHOULD warn when it is present, since a
union that gains `@discriminated` after its members were renamed with `@jsonName` would otherwise change its wire values
silently.

### Discriminator field collision

The discriminator field name MUST NOT collide with the serialized name of any member of any structure targeted by the
union's members. The serialized name of a member is its member name or, in protocols that use `@jsonName` (for example
`restJson1`, but not `awsJson1_0`), the value of that trait. A model is not bound to a single protocol, so both names
are considered.

A collision that holds in every protocol MUST produce a validation error. For example, if the discriminator is
`"type"`, a targeted structure with a member named `type` and no `@jsonName` is invalid. A collision that holds only in
some protocols, for example a member named `type` that is renamed with `@jsonName("x")`, SHOULD produce a validation
event that can be suppressed, because a service that only uses the protocols where it does not apply is not affected.

### Discriminator value

The discriminator value written on the wire is the union member's name. This follows the same convention as the standard
union encoding where the key is the member name. A future extension may allow customizing the discriminator value per
member, but that is out of scope for this proposal.

### Unknown variants

When a deserializer encounters a discriminator value that does not match any known union member, it SHOULD produce an
unknown variant rather than failing. Unknown variants are handled exactly as they are for standard unions: the
discriminator value identifies the unknown variant in the same way the member name does for a standard union, and
whatever an implementation already does to represent a standard union's unknown variant applies unchanged. Being
discriminated introduces no additional unknown-variant handling and no additional data-loss concern.

Whether the unrecognized content itself is preserved (for example, captured as a document so the variant can
round-trip) is a property of how the implementation represents a union's unknown variant in general, not something this
proposal dictates. An implementation MAY preserve it, but this is deliberately left unprescribed, for two reasons.
First, the generated union type is the same regardless of the `@discriminated` trait: the same union can be bound to
several protocols, only some of which use the discriminated encoding, and the union's in-memory representation does not
know which protocol or encoding produced a given value. There is therefore no way to give discriminated unions a richer
unknown variant than standard unions without changing the representation of all unions. Second, changing an existing
unknown variant to carry additional content is likely a breaking change for existing users. If an implementation
chooses to preserve unrecognized content, it SHOULD do so uniformly for all unions rather than conditionally for
discriminated ones.

### Protocol compatibility

The `@discriminated` trait is solely concerned with how a union is serialized and deserialized. It does not change the
semantics of the union itself. For JSON, the trait selects the flat discriminated encoding, for interoperability with
existing systems, in implementations that support it (see "Optional support"). This applies to the JSON serialization
used by the protocols listed below, and equally to standalone types that are serialized to and from JSON without a
protocol, for example types produced by shape-only code generation. For every other protocol or serialization format,
the trait is ignored and the union MUST be serialized and deserialized using the standard tagged union encoding,
behaving identically to a regular union. This is analogous to how `@jsonName` is ignored by non-JSON codecs or
`@xmlFlattened` is ignored by non-XML codecs: each codec reads the schema traits that are relevant to it and disregards
the rest.

The trait is not validated against the protocols of the services that use it. A discriminated union used with a protocol
that does not use the flat encoding is not an error; it is serialized as a regular union. In particular, no validation
event is emitted for `restXml`, `awsQuery` or `ec2Query`, which all define serialization for regular unions. This keeps
the model independent of the protocol: the same union can be bound to different protocols, or a service can change its
protocol, without editing the model. A protocol that does not support unions at all is neither more nor less valid
because of this trait, and whatever validation it applies to unions applies equally to discriminated unions.

Protocols whose JSON serialization the trait applies to, where supported:

1. `aws.protocols#restJson1`
2. `aws.protocols#awsJson1_0`
3. `aws.protocols#awsJson1_1`
4. `smithy.protocols#rpcv2Json`

Protocols where the trait is ignored (standard tagged union encoding):

1. `smithy.protocols#rpcv2Cbor`
2. `aws.protocols#restXml`
3. `aws.protocols#awsQuery`
4. `aws.protocols#ec2Query`

This design supports protocol migration. When a service switches from a JSON protocol to one that ignores the trait,
such as CBOR, both sides share the same model and understand the union's structure without needing the flat
discriminated format on the wire. The `@discriminated` trait requires no changes to any codec that does not use the
flat encoding.

### Optional support

The flat encoding is an optional feature of the JSON serialization, whether that serialization is used by one of the
protocols listed above or by standalone types. Implementations are not required to support it, and the trait is not
added to the `traits` list of those protocols' `@protocolDefinition`, because that list names the traits that every
implementation of a protocol MUST understand, and adding to it would be a breaking change for existing implementations.

An implementation that does not support the trait encodes a discriminated union as a regular union, which does not
interoperate with a peer that uses the flat encoding. A model author SHOULD therefore only use the trait when every
implementation that exchanges data using the model supports it. Implementations that do support it SHOULD support it
consistently, in every component that reads or writes the serialized form.

Protocol compliance tests for the flat encoding are tagged `discriminated-unions` and are defined on a dedicated
operation, so that an implementation that does not support the trait can skip them by tag or by operation. An
implementation that skips them SHOULD reject models that use the trait, rather than silently serialize such unions
with the standard encoding.

Optional features of this kind are not formally described in Smithy today: a protocol cannot declare which optional
features it offers, a model cannot state which ones it relies on, and tooling cannot verify that the parties exchanging
data agree. The trait therefore remains `@unstable` until Smithy can describe and verify optional serialization
features. Until then, the guidance above is not enforced by tooling, and its requirements MAY change when such support
is added.

### Serialization behavior

When serializing a discriminated union to a JSON-based format, the discriminator field SHOULD be written as the first
field in the object. This is not strictly required by the specification since the format is order-independent, but
writing it first enables optimized single-pass deserialization by consumers. Implementations that use third-party
serialization libraries with no control over field ordering are not in violation if the discriminator appears later in
the output.

### Deserialization behavior

Deserializers MUST handle the discriminator field appearing at any position within the object. When the discriminator is
encountered first (the common case when communicating with a compliant serializer), the deserializer can resolve the
target type immediately and deserialize the remaining fields directly into the target structure in a single pass. This
single-pass path needs no scanning or buffering, so it can cost the same as deserializing an equivalent standard
(non-discriminated) union: writing the discriminator first is not merely an optimization hint but the key to making a
discriminated union as cheap to read as a regular one.

When the discriminator is not the first field, the deserializer must recover in some way, since it cannot begin
populating a target structure until it knows which structure that is. Several strategies are possible, in rough order of
implementation complexity:

1. Materialize the object into a generic in-memory representation (a document or map), read the discriminator from it,
   then build the resolved target from that representation. This reuses machinery most implementations already have,
   such as converting a generic document into a typed value, and requires no new abstractions.
2. Buffer the position or contents of each field during an initial scan, then replay the buffered fields into the
   resolved target once the discriminator is found.
3. Scan forward over fields until the discriminator is found, then re-read the object from the start into the resolved
   target.

Strategy 1 is a legitimate and recommended starting implementation. Strategies 2 and 3 can reduce the cost of the
not-first case, but implementers should be aware of two realities before reaching for them:

- Skipping or scanning past a field is not free, and in particular is not free *relative to reading it*. Determining
  where a value ends generally requires the same structural work as parsing it (for nested objects and arrays this
  recurses, and for many scalar encodings the end cannot be found without effectively parsing the value). The only cost
  a buffering strategy genuinely recovers is re-decoding an already-decoded leaf value, not the structural scan itself.
- Because skipping a nested value wastes that same decode work recursively at every level, a buffer that only records
  top-level field spans still re-does the nested work on replay. A buffer that avoids all re-decoding has to preserve
  pre-decoded values at every depth, which converges toward the very in-memory tree that strategy 1 already builds.

The practical guidance is therefore: implement the discriminator-first fast path (it is the common case and needs no
buffering), implement strategy 1 for correctness when the discriminator is not first, and pursue a buffering
strategy only if profiling shows that discriminator-not-first traffic is hot enough to justify the added complexity. The
not-first case is a correctness requirement with a real but bounded cost, not a path that must be made as fast as the
first case.

Regardless of its position, the discriminator is handled as follows:

- The discriminator value must be a string. If the discriminator field is missing, null, or not a string, the variant
  cannot be determined, and the deserializer SHOULD report an error. An empty object is not a special case: like any
  other object without a discriminator, it identifies no variant, and is reported as an error.
- A discriminator value that does not match any member produces an unknown variant (see "Unknown variants").
- Fields that are not members of the resolved variant's structure are unknown members of that structure, and are handled
  like any other unknown structure member. This includes a `__type` field: protocols that ignore an unrecognized
  `__type` member in a union continue to ignore it when the discriminator has a different name, and when the
  discriminator is `__type` (the default) that field is the discriminator.

## Relationship to error polymorphism

AWS protocols currently use a `__type` field (or HTTP headers) to identify error types in responses. While this
mechanism is conceptually similar to a discriminated union, it is a separate concern and will remain so. The
`@discriminated` trait does not replace or formalize the error type resolution mechanism.

However, an implementation that already finds a type-identifying field in an error response may reuse that logic to find
the discriminator field. This is an implementation detail that does not affect the model-level semantics.

## Impact on generated code

In languages that generate typed representations of unions (such as Java's sealed interfaces or Rust's enums), a
discriminated union generates the same type hierarchy as a standard union. The variant types, builder patterns, and
accessor methods are identical, including the unknown variant, which behaves exactly as it does for a standard union
(see "Unknown variants"). The only difference is in how the union is serialized and deserialized.

In implementations whose codecs read traits such as `@jsonName` at runtime, no new serializer or deserializer
interfaces are needed: the codec inspects the union for the `@discriminated` trait and adjusts its behavior
accordingly. Implementations that instead generate protocol-specific serializers generate the flat encoding for
discriminated unions. In both cases the generated types are unchanged.

## Impact on dynamic clients

Clients that work from a model at runtime, without generated types, need no changes beyond supporting the trait in their
JSON serialization. Because the encoding is selected by the `@discriminated` trait in the model, a value such a client
reads from the flat form is presented like any other union value, by the name of its set member and without the
discriminator field, and it is written back in the flat form. The one prerequisite is that the trait is recognized when
the model is loaded (see "Trait registration and model loading").

## Trait registration and model loading

Codecs act on the `@discriminated` trait that they find on the schema at runtime, so the trait must be recognized by the
model loader as a typed trait and not merely retained as generic data. Otherwise a model still loads and the union
carries something named `discriminated`, but a typed lookup for the trait finds nothing and the union is silently
encoded as an ordinary union. Implementations SHOULD therefore test at least once that the trait is loaded from a model
source, because tests that construct the trait directly bypass the model loader and cannot detect this.

## FAQ

### Why not use the existing union with a protocol-level annotation?

A protocol-level annotation would tie the model to specific protocols: the union would have to be annotated for each
protocol that should use the flat encoding, and the union's own definition would no longer say what its data looks
like. The `@discriminated` trait instead declares a property of the union itself: that its variants are structures
whose fields are intended to be inlined with a discriminator. Which protocols honor that property is decided by the
protocol specifications, in the same way that `@jsonName` is a property of a member that only some protocols use.

### Why not allow simple types as member targets?

A discriminated union serializes as a flat object with the discriminator alongside the variant's fields. If the variant
is a simple type like `String`, there are no fields to inline. The result would be something like `{"type": "name",
"value": "hello"}` which requires inventing a synthetic field name for the value. This adds complexity with little
benefit. Services needing this pattern can wrap their simple type in a structure.

### Why does CBOR use the standard tagged encoding?

The `@discriminated` trait exists for interoperability with legacy JSON systems, and the flat encoding is defined only
for JSON wire formats, including the Smithy-defined `rpcv2Json`. The `rpcv2Cbor` protocol is used between peers
generated from the same model, so there is no legacy system expecting a flat form, and its specification already
defines a union as a map keyed by the member name, which this trait does not change. The CBOR codec ignores
`@discriminated` the same way a JSON codec ignores `@xmlFlattened`. The standard tagged encoding
(`{"memberName": {...}}`) also needs no search for a discriminator. A protocol could define a flat CBOR encoding in the
future, but it should do so explicitly in its own specification, so that every implementation of a protocol agrees on
the wire format of a given model.

### How does this interact with jsonName?

The `@jsonName` trait is not used to customize the discriminator value in this proposal. The discriminator value is
always the union member name. This keeps the initial design simple and consistent with the standard union encoding
where the key is also the member name.

However, we recognize that legacy APIs often use discriminator values that do not correspond to natural Smithy member
names (for example, values containing spaces, special characters, or casing conventions that differ from member naming
rules). A future extension might introduce a mechanism to customize the discriminator value per member, whether through
`@jsonName`, a dedicated trait, or another approach. The design of discriminated unions does not preclude this; the
codec simply needs to match against a per-member value instead of the member name. This is explicitly deferred to keep
the initial scope focused.

### Why does a bare `@discriminated` need special handling?

The trait definition declares `@default("__type")`, but `@default` on a trait definition is not applied when the trait
is used, and a trait whose shape is a string requires an explicit value, so a bare `@discriminated` would be rejected as
a missing value. Model loaders therefore have to treat the absent value as `__type` themselves. Code that reads the
trait only ever sees the resolved field name, so codecs and other consumers are unaffected.

### Can discriminated unions be nested?

Yes, through the members of a variant's structure. A member of a discriminated union cannot target another union
directly (see "Constraints on member targets"), but the structure that a variant targets can have members that target
unions, discriminated or standard. Each such member is serialized as a nested object according to the target union's own
rules: a discriminated union appears as an object with its own discriminator field, and a standard union uses the
standard tagged encoding. For example, if a `Team` union (discriminator `sport`) has a `soccer` variant whose structure
has a `mascot` member targeting a `Mascot` union (discriminator `kind`), a soccer team with an animal mascot serializes
as:

```json
{"sport": "soccer", "name": "Rovers", "mascot": {"kind": "animal", "species": "lion"}}
```

The two unions could also share a discriminator name, since each discriminator applies to a different JSON object.
