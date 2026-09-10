# 0001. Resolve endpoint rule-set components against the model's classloader

* Status: Accepted
* Date: 2026-08-26
* Deciders: Smithy maintainers

## Context and Problem Statement

Endpoint rule-set functions (for example `aws.partition`), built-in parameters
(for example `AWS::Region`), and auth-scheme validators (for example `sigv4`)
are contributed by `EndpointRuleSetExtension` service providers discovered
through the JDK `ServiceLoader`. The AWS providers live in `smithy-aws-endpoints`,
separate from `smithy-rules-engine`.

`EndpointRuleSet` discovered these providers using
`EndpointRuleSet.class.getClassLoader()`, that is, the classloader that loaded
the rules engine itself. When a caller assembles a model with an explicit
classloader (`Model.assembler(classLoader)`) whose closure contains the
providers, that closure classloader is typically a child of the classloader
that loaded the rules engine. A parent classloader cannot see providers that
exist only in a child. As a result, assembling such a model failed with
`` `aws.partition` is not a valid function `` and
`` `AWS::Region` built-in used is not registered ``, and produced spurious
`Did not find a validator for the `sigv4` auth scheme` warnings.

This affects any consumer that assembles models with a custom classpath: build
tools that isolate a dependency closure, IDEs, application servers, and services
that assemble user-supplied models at runtime.

## Decision Drivers

* Must work out of the box. Smithy is a library; consumers cannot be required to
  learn about a rules-engine classloading quirk or add extra dependencies.
* No global mutable state, and safe under concurrency. Model validation runs on
  a parallel stream over `ForkJoinPool`, so any solution must be correct when
  invoked from pool worker threads.
* Must not pin closure classloaders for the lifetime of the JVM. Long-lived
  processes assemble many models against many closures; retained classloaders
  are a memory leak.
* Backwards compatible. No public method signatures may be removed or changed.

## Considered Options

1. Add `smithy-aws-endpoints` to the consumer's runtime classpath. Rejected: it
   masks the underlying bug, only fixes the AWS providers, and every consumer
   must rediscover and repeat it.

2. Discover providers from the thread context classloader (TCCL). Rejected:
   validators run on `ForkJoinPool.commonPool()` worker threads, which do not
   inherit the submitting thread's TCCL, so the closure classloader would not be
   seen at validation time. This holds even if the caller sets the TCCL around
   the assemble call.

3. Add a static, settable classloader override on `EndpointRuleSet`. Rejected:
   global mutable state is unsafe for a shared library used by concurrent
   callers, and it couples unrelated assemblies.

4. Keep a static `Map<ClassLoader, EndpointComponentFactory>` cache. Rejected:
   the map strongly pins every closure classloader for the life of the JVM. A
   weak-keyed map does not help, because the cached factory holds provider
   instances that were loaded by the key classloader, so the value transitively
   references the key and defeats weak collection.

5. Thread the model's classloader into trait creation and resolve components
   through it, memoizing the resolved factory on the trait instance. Chosen.

## Decision

Thread the classloader that assembled the model down to the point where
endpoint components are resolved, and carry the resolved factory rather than the
raw classloader.

* `TraitService` gains a defaulted
  `createTrait(ShapeId target, Node value, ClassLoader classLoader)` overload.
  The default delegates to the existing two-argument method, so existing
  providers are unaffected.
* `TraitFactory` captures the classloader passed to
  `Model.assembler(ClassLoader)` and supplies it to providers through the new
  overload.
* Both `EndpointRuleSetTrait` and `EndpointBddTrait` build one
  `EndpointComponentFactory` from that classloader and memoize it on the trait
  instance. The factory wraps the classloader and caches the discovered
  components.
* The factory is threaded through rule-set deserialization
  (`EndpointRuleSet.fromNode(Node, EndpointComponentFactory)`, then `Rule`,
  `Condition`, `Endpoint`, and `Expression`/`FunctionNode`, including functions
  nested as arguments), through the equivalent `EndpointBddTrait` deserialization
  path, and through the `RuleSetBuiltInValidator` and
  `RuleSetAuthSchemesValidator` validators, which read it from the trait. The
  factory is also carried on `EndpointRuleSet`, `Cfg`, and `EndpointBddTrait` so
  programmatic transforms that round-trip a rule-set or compile it to a BDD
  re-resolve functions against the same classloader. Every transform that
  rebuilds an `EndpointRuleSet` must carry the factory (through
  `EndpointRuleSet.toBuilder()`, an explicit `componentFactory(...)` on a fresh
  builder, or `fromNode(node, factory)`); a transform that rebuilds via a bare
  `EndpointRuleSet.builder()` silently resets the factory to the default and
  defeats the fix for everything downstream of it. The transforms that carry it
  today are `CfgBuilder`, `CoalesceTransform`, `IsSetBooleanCoalesceTransform`,
  the `TreeMapper`-based transforms, and `EndpointBddTrait.from(Cfg)`; the
  `EndpointPathCollector` reads it from the trait.
* When no distinct classloader is present (none supplied, or one equal to the
  rules engine's own loader), a single shared default factory is reused so
  `ServiceLoader` is not re-run for the common case.

## Consequences

* Positive: correct for any closure classloader, with no consumer changes
  required; no static per-loader retention, so closure classloaders are not
  pinned; thread-safe without locks on a shared cache, because the resolved
  factory travels as data on the trait rather than through thread state; purely
  additive, so no caller breaks.
* Negative: `ServiceLoader` runs once per trait instance instead of once per
  classloader across a whole process. This is acceptable because discovery is
  bounded and the shared default factory covers the common same-classpath case.
* Testing caveat: the classloader-driven resolution is guarded end to end for
  the condition and nested-argument paths of both `@endpointRuleSet` and
  `@endpointBdd` by an integration test that assembles a model under a
  classloader that hides the AWS endpoints extension (see
  `smithy-aws-endpoints/src/it`). Two threaded paths are correct by construction
  but not covered by a test that would fail on the pre-fix code: the
  `Cfg`/`CfgBuilder` re-parse during BDD compilation (the BDD fixture must be
  built with the extension visible, so the hidden-loader case cannot be
  exercised without a synthetic string-returning extension), and functions used
  directly in an endpoint URL, header, or error (the AWS functions return
  records, not strings, so no realistic model exercises them). Adding a synthetic
  `EndpointRuleSetExtension` that contributes a string-returning function would
  close both gaps if they ever warrant a regression test.
* Trade-off accepted on purpose: we chose no classloader retention over
  cross-assembly cache reuse. A process that assembles many models against the
  same non-default closure classloader will run discovery once per model rather
  than sharing a single cached factory. If that ever becomes a measured cost,
  introduce an explicitly scoped cache with a defined lifecycle rather than a
  static map.

## Pattern for future work

A `TraitService` whose node value must resolve components discovered through its
own service-provider interface (functions, validators, or nested extensions)
should follow this pattern so it works with caller-supplied classloaders:

1. Override `createTrait(ShapeId, Node, ClassLoader)` and capture the
   classloader.
2. Build and memoize any discovery factory on the trait instance, not in a
   static field, so it is collected with the model and does not pin the
   classloader.
3. Thread the resolved factory, not a raw `ClassLoader`, through the value's
   deserialization and any validators, so discovery runs once and is reused. If
   the value is rebuilt by transforms, ensure every transform carries the
   factory forward; a single transform that rebuilds via a bare builder resets
   it to the default and silently defeats the fix downstream.
4. Fall back to a shared default factory when the classloader is null or equal
   to the owning class's loader.

Reference implementation: `EndpointRuleSetTrait` and `EndpointBddTrait`
together with `EndpointComponentFactory` in `smithy-rules-engine`.
