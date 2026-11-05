# Contracts trait

This is a proposal for related additions to Smithy, building on the `@conditions` [contract trait](https://smithy.io/2.0/additional-specs/contract-traits.html):

1. Adding a new `@contracts` trait that expresses preconditions and postconditions over operation calls. It shares all of `@conditions`' JMESPath machinery but constrains operation calls rather than shape values.
2. Adding a `name` field to the `@references` trait's `Reference` structure, to declare a unique symbol for the resource referenced by a structure in JMESPath expressions.
3. Adding two built-in JMESPath functions that make these contracts usable for real services: `resource` and `requires`.

## Motivation

The constraints and behaviors of an API usually live in prose documentation or in service code, out of reach of tooling. `@conditions` and `@contracts` move that knowledge into the model as structured, machine-checkable statements. Structured API semantics make services more legible to coding agents, which can read a precondition rather than infer it. They give service owners a basis for test automation, since a modeled contract can be checked against examples, shadow traffic, and logs. They give SDKs and tooling a shared, precise vocabulary for API behavior. They are also a building block for a broader machine-readable description of API behavior: modeling constraints and behaviors in the canonical Smithy spec is exactly the kind of structured knowledge that automated reasoning can turn into guardrails for AI outputs.

## Background: `@conditions` today

`@conditions` is a [contract trait](https://smithy.io/2.0/additional-specs/contract-traits.html) from the `smithy-contract-traits` package: a `map` from condition names to `Condition` structures, each carrying a JMESPath `expression` that must evaluate to true and required `documentation`. It is distinct from the built-in *constraint traits* (`@range`, `@pattern`, `@length`, and the rest), which are a separate category, each expressing one fixed kind of restriction. `@conditions` instead expresses arbitrary boolean restrictions in an established query language.

Contract traits are validation only. The spec is explicit that they SHOULD NOT change generated type signatures and SHOULD NOT be enforced during serialization by default, since services usually check these contracts outside frameworks in more efficient ways.

The trait's selector is `:not(:test(service, operation, resource))`, so on a data shape a `@conditions` entry is a predicate over the shape's value. This proposal leaves `@conditions` as it is and adds a separate operation-scoped `@contracts` trait, because an operation has no value to constrain. It is a procedure, and what a contract constrains is a call (see below). Extending `@conditions` to also express service and resource state invariants is a possible later step (see Future Work).

## `@contracts` on operations

Smithy treats a `@conditions` entry as a predicate over a *value* of the shape it is attached to. An operation has no value. It is a procedure, a mapping from input to output. What a contract constrains is a *call*: one element of that mapping, a record with the fields `input, output, error, before, after`, where `before` and `after` are world snapshots (see below). Operations carry a distinct `@contracts` trait whose entries are predicates over calls.

`@conditions` and `@contracts` share everything mechanical: the same `map` of named `Condition` structures, the same JMESPath expressions, the same functions (below), and the same data model (below). They differ only in what the predicate ranges over. A value for `@conditions`, a call for `@contracts`. That is exactly the distinction Smithy already draws between value and constraint traits, which apply to data, and operation-scoped behavioral traits such as `@examples` and `@waitable`, which describe calls. The Alternatives section explains why this is a separate trait rather than a widened `@conditions`.

### Preconditions and postconditions on operations

Because an operation call exposes `error`, preconditions and postconditions are ordinary `@contracts` entries, gated on the outcome with the `requires` helper. There is no separate `precondition` or `postcondition` field on the trait.

`requires(@, P)` reads as a necessary condition for success: on any successful call (`error` is null), `P` must hold; on a failing call the requirement is vacuously satisfied. A precondition (`P` over `input` and `before`) and a success postcondition (`P` over `output` and `after`) are both written this way. Passing `@` (the whole call) is what lets `requires` read `error`. See New functions below. An implication that is *not* gated on success is a possible later addition (see the `implies` bullet under Future Work).

### Service state in the JMESPath data model

To model `before` and `after`, the [JMESPath data model section](https://smithy.io/2.0/additional-specs/waiters.html#waiter-jmespath-data-model) is extended with a representation of service state. A **world snapshot** maps each service shape to the *set* of that service's instances. A service can have many instances, for example one per region, so the mapping is to an array. The full set is required so cross-service and cross-instance references, such as ARNs, can be resolved. `before` and `after` are two world snapshots.

Define `JPT: Shape -> JMESPath type`:

- `service` maps to a record whose `resources` field maps each contained resource shape name to an `array` of `JPT(resource)` (the set of that resource's instances).
- `resource` maps to a record with an `identifiers` field (each declared identifier) and a `properties` field (each declared property, typed by `JPT(property)`), plus a nested `resources` field as on `service`.
- an operation call exposes `input: JPT(input)`, `output: JPT(output)`, `error`, and the two world snapshots `before` and `after`.

A small world snapshot with one SQS instance and one KMS instance:

```json
{
  "SQS": [
    {
      "resources": {
        "Queue": [
          { "identifiers": { "queueUrl": "https://sqs.us-east-1.../q1" },
            "properties": {
                "messageRetentionPeriod": 345600,
                "kmsMasterKey": "arn:aws:kms:us-east-1:111122223333:key/k1"
            } }
        ]
      }
    }
  ],
  "KMS": [
    {
      "resources": {
        "Key": [
          { "identifiers": { "keyId": "k1" },
            "properties": { "keyState": "ENABLED" } },
          { "identifiers": { "keyId": "k2" },
            "properties": { "keyState": "DISABLED" } }
        ]
      }
    }
  ]
}
```

Each top-level key is a service shape mapped to the array of its instances; each instance exposes its `resources`; each resource instance is a record with its `identifiers` and `properties`. Here the queue's `kmsMasterKey` ARN points into the `KMS` entry, which is why a world snapshot carries the full set of services rather than only the one handling the call.

Because a world holds a set of instances per service, a reference must say which instance it means. A named `@references` reference without a `service` resolves against the current service instance, the one handling the call ("this service"). With a `service` it resolves against that shape's only or default instance. Selecting an arbitrary non-default instance, such as a specific region, needs explicit coordinates such as an ARN.

### Naming references

`resource(...)` (see New functions below) looks up a resource from a reference handle. To project a `@references` reference into JMESPath as such a handle, the `Reference` structure needs a stable symbol. This proposal adds a `name` member to `Reference`:

- It provides the JMESPath name for the reference from a structure to a resource.
- It disambiguates multiple references to the same resource type from one structure in general.
- It is validated not to collide with member names, so the projected handle does not shadow a real member. S3's `CompleteMultipartUploadRequest` is the motivating case where the natural name would collide; a distinct `name` avoids it. The same check applies to a reference declared directly on a string shape used as a member.

A request declares the reference, and a `@contracts` entry uses it by that name:

```smithy
resource Key {
    identifiers: {
        keyId: String
    }
    properties: {
        keyState: KeyState
        ...
    }
}

@references([
    { resource: Key, ids: { keyId: keyId }, name: key }
])
structure EncryptRequest {
    @required
    keyId: String
    // ...
}
```

Here `input.key` (the reference name, not the `keyId` member) projects to the handle `{ resource: "Key", ids: { keyId: <value of keyId> } }`, which is what the `Encrypt` example below passes to `resource(before, ...)`.

`@references` can also be declared directly on a string shape, which Smithy already allows for a resource with a single identifier. The reference then applies wherever that string is used as a member, and `name` projects the handle the same way. The member keeps its string value, and the handle is reachable under the distinct `name`:

```smithy
@references([{ resource: Key, name: key }])
string KeyId

structure EncryptRequest {
    @required
    keyId: KeyId
    // ...
}
```

Here `input.keyId` is still the string value and `input.key` is the handle, so the two never collide. The `name` is validated to be distinct from the members of any structure that uses the shape.

## New functions for `@contracts`

A small, fixed set of built-in functions makes these contracts usable for real services. In the initial delivery these ship as ordinary built-ins typed by the existing linter, without yet exposing a general user-facing "declare your own function" surface. Their names are reserved so a later user-declared function cannot collide. A general custom-function mechanism and the precise typing of these built-ins are possible later work (see Future Work).

### `resource(world, handle)`

Resolves a resource in a world snapshot. The `handle` is the reference structure a named `@references` reference projects into JMESPath (see "Naming references"):

```
{ service: String (optional), resource: String, ids: { <idName>: <idValue>, ... } }
```

`resource(world, handle)` finds, in `world`, the service instance (`handle.service`, defaulting to the current service instance) and the resource of type `handle.resource` whose identifiers equal `handle.ids`, returning the resource value or `null`. The result type depends on `handle.resource`, so a fixed signature cannot type it precisely; in this increment it is typed coarsely.

The world snapshot argument is explicit and required, so `before` and `after` are always named at the call site.

A bare string is not a handle. `resource(before, input.queueUrl)` does not type-check, because a string carries neither the resource type (which the result type depends on) nor the full identifier map (identifiers may be compound). The handle is reached through a named reference, `resource(before, input.queue)`.

### `requires(@, P)`

A necessary condition for success. `requires(call, P)` is true when the call failed (`error` is non-null), or when `P` is truthy. It reads as: on any successful call, `P` held. `P` is evaluated eagerly, because a necessary precondition reads only pre-call state (`input` and `before`), which is present on both the success and failure paths. Passing `@` (the whole call) is what lets it read `error`.

### Examples

An operation precondition and a success postcondition for KMS' `Encrypt`. `input.key` is a named `@references` handle (see "Naming references" above), and `keyState` is a declared property of the `Key` resource:

```smithy
@readonly
@contracts({
    keyEnabled: {
        expression: "requires(@, resource(before, input.key).properties.keyState == 'ENABLED')"
        documentation: "The key must exist and be ENABLED."
    }
    echoesKeyId: {
        expression: "requires(@, output.keyId == input.keyId)"
        documentation: "On success the response echoes the request's key id."
    }
})
operation Encrypt {
    input: EncryptRequest
    output: EncryptResponse
    errors: [NotFoundException, DisabledException, KMSInvalidStateException]
}
```

A state-changing postcondition (KMS `ScheduleKeyDeletion` drives the key into `PENDING_DELETION`):

```smithy
@contracts({
    keyExists: {
        expression: "requires(@, resource(before, input.key) != `null`)"
        documentation: "The referenced key must exist."
    }
    nowPendingDeletion: {
        expression: "requires(@, resource(after, input.key).properties.keyState == 'PENDING_DELETION')"
        documentation: "On success the key is in PENDING_DELETION."
    }
})
operation ScheduleKeyDeletion { input: ScheduleKeyDeletionRequest, output: ScheduleKeyDeletionResponse, errors: [NotFoundException, KMSInvalidStateException] }
```

## Alternatives

### Apply `@conditions` to operations directly

An alternative is to apply the existing `@conditions` trait directly to operations: drop the selector exclusion, treat an operation's instance as the call tuple `(input, output, error, before, after)`, and use the `requires` and `resource` functions and the `@references` `name` projection inside `@conditions` entries on the operation.

We prefer a separate `@contracts` trait for operations. `@conditions` is defined as restricting the *values* of a shape, and in Smithy a value of a shape is a data value. An operation has no value: it is a procedure, and what a contract constrains is a *call*, one element of the operation's input-to-output relation, not a value. Reusing `@conditions` would either stretch "value" to also mean "call", or require a general "instance of any shape" abstraction that Smithy does not have. Smithy instead expresses operation behavior through dedicated operation-scoped traits such as `@examples`, `@waitable`, and `@paginated`, and `@examples` already treats an operation's calls as first-class without calling them values. A separate `@contracts` trait follows that pattern: `@conditions` stays a value invariant on data shapes, and `@contracts` is a predicate over an operation's calls, sharing the same JMESPath machinery, functions, and data model.

## FAQs

**Q: Do SDKs evaluate these?** No. Like the built-in constraint traits, `@conditions` and `@contracts` are service semantics for analysis and for checking against examples, not client-side gates.

**Q: Do you need quantifiers for invariants?** No. Service state is exposed as finite collections, so existence and universality are filters and `length` over those collections. Dedicated `any` and `all` array functions would make many such invariants read better; they are a possible later addition (see Future Work).

**Q: How do these stay in sync with real service behavior?** Same orthogonal concern as all constraint and contract traits: checking against `@examples`, shadow-mode monitoring, and log analysis. Because the contracts sit next to the operations and shapes, service owners are likely to imitate them on new operations, as they already do with existing constraint traits.

## Appendix A: Trait selectors and value

`@conditions` selector. The selector is `:not(:test(service, operation, resource))`, which admits any shape except services, operations, and resources. This proposal does not change it.

`@contracts` selector. `@contracts` is a new trait with selector `operation`, so it applies only to operations.

Both share the value type: a `map` from condition names to `Condition` structures, where the names give `smithy-diff` per-clause precision and each `Condition` has:

- `expression` (**required**): a JMESPath expression that must evaluate to true. For `@conditions` it is evaluated over a value of the shape; for `@contracts` it is evaluated over an operation call.
- `documentation` (**required**): CommonMark documentation of the condition, used in messages when it is violated.

## Future Work

These are possible future directions that build on this proposal. None is a commitment, and none is a dependency of the changes here.

- **`@conditions` on services and resources.** Dropping the service and resource exclusions from the `@conditions` selector so it also expresses state invariants: a resource instance is its identifiers and properties, a service instance is its resources, and a `@conditions` entry is an invariant that must hold in every reachable state. This reuses the same world-snapshot data model and functions.
- **`implies(condition, &consequent)`.** A logical-implication built-in whose consequent is passed as an expression reference and evaluated only when the condition holds. It expresses implications that are not gated on success, which `requires` cannot. The expression-reference form is correct where the `!condition || consequent` idiom is not, because expression-reference arguments are lazy by spec whereas operator short-circuit is not mandated by JMESPath.
- **Optional runtime contract trait enforcement.** An opt-in option in a target SDK to evaluate these traits at runtime, so a service can enforce its own modeled contracts and a test harness can catch service behavior that has drifted from the model.
- **Custom functions and a precise type system.** A general mechanism for declaring custom JMESPath functions by abstract interpretation, and the richer type lattice it needs, which would also re-type the built-in functions here precisely as a non-breaking refinement.
- **`any` and `all` array functions.** Readability built-ins that let universal and existential invariants read directly, for example ``all(resources.Queue, &(properties.messageRetentionPeriod != `null`))`` instead of the filter-and-`length` idiom.
- **`resource_at_arn` and cross-instance resolution.** A built-in that parses an ARN into a handle, and more generally the rules for selecting a specific non-default service instance (per-region, per-account) rather than the current or default one.
- **Functions as operations.** Whether the built-in functions, and eventually user-declared ones, can be expressed as Smithy operations with bodies declared in JMESPath or a similar language, so the function set and the operations it constrains share a single definition mechanism.
