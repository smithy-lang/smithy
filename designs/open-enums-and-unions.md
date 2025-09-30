# Enums and unions allowed to be closed

## Abstract

In the Smithy 2.0 specification enums and unions are treated as strictly **open**: adding a member is always treated as a backward‑compatible change and clients are expected to handle unknown members at runtime. This RFC proposes to update the spec and loosen the strict requirements around open enums and unions, acknowledging the fact that for some use cases and protocols it may not be desireable. It will also align the spec closer to reality, as smithy4s do not follow the current spec.

## Motivation

It has been identified by smithy users that open by default enums and unions are problematic for several reasons:

- **Exhaustiveness and type safety**. Many target languages (like Scala) encourage type safety and exhaustive matches on enums and algebraic data types. Default open enums/unions forces default, catch-all branches, weakening compiler checks forcing implementations to either provide a catch-all default (see point above for issues with it) or weaken exhaustivity of the union encoding.  
- **Data integrity**. Treating unexpected variants as acceptable can mask producer errors and defer the discovery of such bugs. Closed enums and unions force modelers to be explicit if they want to opt-in and make it open for cases where forward compatibility is truly required.
- **Protocol‑agnostic friction** - Unknown members of open unions are inherently protocol‑specific (e.g., JSON or binary) - it’s difficult to guarantee a single, portable “unknown” representation across protocols and therefore strictly follow the current spec. 
- **Explicitness**: Unexpected variants can become explicit design decisions enabled on a per protocol basis.

At the same time, open unions are valuable in certain domains (for example, event streams that evolve by adding event types). 

## Proposal

* The strict requirements for open enums and unions are loosened.
* Protocol level opt-ins are outside of the scope of what the `smithy.api` would provide.

## Smithy4s example

[Smithy4s](https://disneystreaming.github.io/smithy4s/) chooses to model unions and enums as closed by default. Below a quick summary of how it achieves the opt-in openness.

### Enums

Enums are closed by default, but [as an opt-in](https://disneystreaming.github.io/smithy4s/docs/codegen/customisation/open-enums) can be opened. This is done via custom trait, `alloy#openEnum`:

```smithy
use alloy#openEnum

@openEnum
enum Shape {
  SQUARE, CIRCLE
}

@openEnum
intEnum IntShape {
  SQUARE = 1
  CIRCLE = 2
}

```

Presence of such annotation will cause the codegen also generate an `Unknown` variant, capable of capturing the values which do not match to the set of known enum cases.

### Unions

[Unions] are also closed by default and like enums can be [opened](https://disneystreaming.github.io/smithy4s/docs/codegen/unions/#open-unions). Smithy4s outputs protocol agnostic code, and it would not be easy to provide a single “unknown” representation that works well across protocols. 

In the context of the json based protocols, unions can be opened, like so:

```smithy
use alloy#jsonUnknown

union Shape {
  square: Square
  @jsonUnknown other: Document
}

structure Square {
  @required side: Integer
}
```

Such addition will make encoders and decoders aware about the unknown member - for example when deserializing an unknown variant, the whole json node (including a discriminator or tag) will be captured into such a `Document`. 


