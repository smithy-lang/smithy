# Smithy Client Guidance

This guide provides advice on how to build clients to interact with Smithy
services. In particular, it provides advice on how to design the generated
client and the components that make it up.

While topics in this guide may briefly discuss code generation, this guide
does not describe how to build a code generator. To learn how to build a code
generator, see
[Creating a Code Generator](project:../building-codegen/index.rst).

## Goals of this guide

- Give advice on how to design client components.
- Provide guidance on how to avoid coupling components to particular transport
  protocols or to AWS-specific features.
- Provide guidance on how to make clients extensible.

## Non-goals of this guide

- Provide guidance on how to design and implement code generators.
- Force specific implementation details. This guide is non-normative, you are
  free to deviate from its advice.

## Tenets for Smithy clients

These are the tenets of Smithy clients (unless you know better ones):

1. **Smithy implementations adhere to the spec**. The Smithy spec and model
   are the contract between clients, servers, and other implementations.
   A Smithy client written in any programming language should be able to
   connect to a Smithy server written in any programming language without
   either having to care about the programming language used to implement
   the other.
2. **Smithy clients are familiar to developers**. Language idioms
   and developer experience factor in to how developers and companies
   choose between Smithy and alternatives.
3. **Components, not monoliths**. We write modular components that
   developers can compose together to meet their requirements. Our
   components have clear boundaries: a client that uses an AWS protocol is
   not required to use AWS credentials.
4. **Our code is maintainable because we limit public interfaces**. We
   limit the dependencies we take on. We don't expose overly open
   interfaces that hinder our ability to evolve the code base.
5. **No implementation stands alone**. Test cases, protocol tests, code
   fixes, and missing abstractions have a greater impact if every Smithy
   implementation can use them rather than just a single implementation.
6. **Service teams don't need to know the details of every client
   that exists or will ever exist**. When modeling a service,
   service teams only need to consider if the model is a valid Smithy
   model; the constraints of any particular programming language should
   not be a concern when modeling a service. Smithy is meant to work
   with any number of languages, and it is an untenable task to attempt
   to bubble up every constraint, reserved word, or other limitation to
   modelers.
