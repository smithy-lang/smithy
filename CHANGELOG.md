# Smithy Changelog

## 1.60.3 (2025-06-27)

### Bug Fixes

* Fixed incorrect operation name in URI for `NonQueryCompatibleOperation`. ([#2684](https://github.com/smithy-lang/smithy/pull/2684/))
* Fixed incorrect property name in protocol test bodies. ([#2686](https://github.com/smithy-lang/smithy/pull/2686))

## 1.60.2 (2025-06-26)

### Bug Fixes

* Added model discovery args to smithy select. ([#2680](https://github.com/smithy-lang/smithy/pull/2680))
* Fixed `NonQueryCompatible` to remove the bogus empty JSON body and media-type. ([#2681](https://github.com/smithy-lang/smithy/pull/2681))
* Reduced query error uniqueness validation severity. ([#2682](https://github.com/smithy-lang/smithy/pull/2682))

## 1.60.1 (2025-06-25)

### Bug Fixes

* Fixed body of awsQueryCompatible test. ([#2677](https://github.com/smithy-lang/smithy/pull/2677))

## 1.60.0 (2025-06-23)

### Bug Fixes

* Added ASM to the relocated dependencies. ([#2676](https://github.com/smithy-lang/smithy/pull/2676))

### Features

* Added protocol tests for `@awsQueryCompatible`. ([#2672](https://github.com/smithy-lang/smithy/pull/2672))
* Added validation to ensure query errors are unique. ([#2674](https://github.com/smithy-lang/smithy/pull/2674))

## 1.59.0 (2025-06-16)

### Bug Fixes

* Fixed generation of nested lists/maps in trait code generation. ([#2647](https://github.com/smithy-lang/smithy/pull/2647))
* Fixed generation of boolean collections in trait code generation. ([#2652](https://github.com/smithy-lang/smithy/pull/2652))
* Fixed enum generation in docgen. ([#2653](https://github.com/smithy-lang/smithy/pull/2653))

### Features

* Added a warning when mixin members are removed. ([#2644](https://github.com/smithy-lang/smithy/pull/2644))
* Added a tag to identify service-specific protocol tests. ([#2655](https://github.com/smithy-lang/smithy/pull/2655))

### Documentation

* Made several improvements to the landing pages. ([#2656](https://github.com/smithy-lang/smithy/pull/2656))
* Added guidance about generating unknown members for unions. ([#2657](https://github.com/smithy-lang/smithy/pull/2657))
* Updated references to `awslabs` to `smithy-lang` where relevant. ([#2662](https://github.com/smithy-lang/smithy/pull/2662))

### Other

* Added additional protocol tests for `restJson1`. ([#2641](https://github.com/smithy-lang/smithy/pull/2641))

## 1.58.0 (2025-05-13)

### Bug Fixes

* Fixed `Node` serialization and deserialization of rules engine endpoint values. ([#2616](https://github.com/smithy-lang/smithy/pull/2616))
* Fixed null pointer exceptions when serializing endpoints traits to nodes. ([#2629](https://github.com/smithy-lang/smithy/pull/2629))
* Made `smithy.rules#endpointTests` have an explicit dependency on `smithy.rules#endpointRuleSet`. ([#2637](https://github.com/smithy-lang/smithy/pull/2637))

### Features

* Added hierarchical IDs for `ChangedOperation` diff events. ([#2607](https://github.com/smithy-lang/smithy/pull/2607))
* Removed `@unstable` from the following traits: `@standardRegionalEndpoints`, `@standardPartitonalEndpoints`, and `@dualStackOnlyEndpoints`. ([#2608](https://github.com/smithy-lang/smithy/pull/2608))
* Made `Dynamic` `Part`s of the rules engine public. ([#2614](https://github.com/smithy-lang/smithy/pull/2614))
* Made validation of IAM resource names case-insensitive. ([#2615](https://github.com/smithy-lang/smithy/pull/2615))
* Added several static utility methods to the rules engine. ([#2617](https://github.com/smithy-lang/smithy/pull/2617), [#2618](https://github.com/smithy-lang/smithy/pull/2618))
* Made `arnNamespace` optional in IAM traits that support specifying condition keys. ([#2619](https://github.com/smithy-lang/smithy/pull/2619))
* Added validation to ensure that the value for any condition key may only be supplied by one member in operation input. ([#2620](https://github.com/smithy-lang/smithy/pull/2620))
* Added additional validation for the `endpointsTests` trait. ([#2622](https://github.com/smithy-lang/smithy/pull/2622))
* Added `UnknownMember` to the event ID for node validation. ([#2630](https://github.com/smithy-lang/smithy/pull/2630))

### Documentation

* Added more links to OpenAPI APIGateway config. ([#2605](https://github.com/smithy-lang/smithy/pull/2605))
* Fixed bad links in javadocs. ([#2612](https://github.com/smithy-lang/smithy/pull/2612))
* Documented `SUPPRESSED` as a valid value for `--severity` in validate command. ([#2638](https://github.com/smithy-lang/smithy/pull/2638))

### Other

* Improved performance of `CleanOperationStructures`. ([#2609](https://github.com/smithy-lang/smithy/pull/2609))
* Improved performance of several rules engine functions. ([#2633](https://github.com/smithy-lang/smithy/pull/2633), [#2634](https://github.com/smithy-lang/smithy/pull/2634), [#2635](https://github.com/smithy-lang/smithy/pull/2635), [#2636](https://github.com/smithy-lang/smithy/pull/2636))

## 1.57.1 (2025-04-21)

### Bug Fixes

- Fixed an issue where `FileManifest::writeJson` would return a relative path instead of an absolute one ([#2602](https://github.com/smithy-lang/smithy/pull/2602))

## 1.57.0 (2025-04-21)

### Features

- Added `aws_recommended` as a partitional endpoint pattern type ([#2575](https://github.com/smithy-lang/smithy/pull/2575))
- Increased validation event severity for input name-value validation for the endpoint tests trait ([#2593](https://github.com/smithy-lang/smithy/pull/2593))
- Added nascent document type support for RPC v2 CBOR ([#2595](https://github.com/smithy-lang/smithy/pull/2595))
- Enabled AWS query compatibility for RPC v2 CBOR ([#2579](https://github.com/smithy-lang/smithy/pull/2579))

### Bug Fixes

- Updated `restXml` protocol tests to align with other XML tests ([#2583](https://github.com/smithy-lang/smithy/pull/2583))

### Documentation

- Fixed waiter examples that included wrong members ([2594](https://github.com/smithy-lang/smithy/pull/2594))


## 1.56.0 (2025-03-27)

### Features

- Added `FlattenAndRemoveMixins` transform to list of provided build transforms ([#2552](https://github.com/smithy-lang/smithy/pull/2552))
- Added `Since` suffix to timestamp linter for better timestamp validation ([#2554](https://github.com/smithy-lang/smithy/pull/2554))
- Improved performance by preferring `ShapeId` for `hasTrait` lookups instead of class-based lookups ([#2562](https://github.com/smithy-lang/smithy/pull/2562))
- Improved `CleanClientDiscoveryTraitTransformer` implementation by adding short ciruit if ClientDiscovery traits are not applied ([#2559](https://github.com/smithy-lang/smithy/pull/2559))
- Make IDL serialization clearer by skipping to serialize default boolean values ([#2553](https://github.com/smithy-lang/smithy/pull/2553))
- Optimized `ModelTransformPlugin` and `ResourceIdentifierBindingValidator` to use fewer intermediate objects and streams.([#2561](https://github.com/smithy-lang/smithy/pull/2561))
- Added `breakingChanges` property to the removal of sigv4 and sigv4a traits ([#2567](https://github.com/smithy-lang/smithy/pull/2567))
- Relaxed constraints on `httpPrefixHeaders` trait to have `NOTE` severity during validation when the prefix is set to empty string ([#2565])(https://github.com/smithy-lang/smithy/pull/2565))

### Bug Fixes

- Relaxed on `TaggableResource` instance validation by lowering the severity from `ERROR` to `DANGER` when a resource does not have instance operations for manipulating tags and service level tagging operations are not present ([#2566](https://github.com/smithy-lang/smithy/pull/2566))
- Fixed OpenAPI conversion by using `ShapeId` instead of name, reducing unnecessary object creation ([#2560](https://github.com/smithy-lang/smithy/pull/2560))


### Documentation

- Added TypeScript quickstart pages to provide tutorial for users to generate clients and SDKs with Smithy TypeScript ([#2536](https://github.com/smithy-lang/smithy/pull/2536))
- Added documentation for OpenAPI `enumStrategy` setting to clarify configuration options ([#2551](https://github.com/smithy-lang/smithy/pull/2551))
- Added new Smithy landing page for improved user experience ([#2543](https://github.com/smithy-lang/smithy/pull/2543))


## 1.55.0 (2025-02-27)

### Features

* Added support for `oneOf` enumStrategy in `smithy-jsonschema` ([#2504](https://github.com/smithy-lang/smithy/pull/2504))
* Updated `TaggableApiConfig` builder methods visibility to public ([#2506](https://github.com/smithy-lang/smithy/pull/2506))
* Added `flattenAndRemoveMixins` build transform ([#2516](https://github.com/smithy-lang/smithy/pull/2516))
* Added ability to model more complex ARN templates in the `@arn` trait ([#2527](https://github.com/smithy-lang/smithy/pull/2527))
* Expanded the list of allowed member names in tag shapes ([#2528](https://github.com/smithy-lang/smithy/pull/2528))
* Added `primaryIdentifier` field to the `cfnResource` trait to indicate an unconventional primary identifier ([#2539](https://github.com/smithy-lang/smithy/pull/2539))

### Bug Fixes

* Fixed malformed CBOR body in `rpcv2Cbor` test ([#2502](https://github.com/smithy-lang/smithy/pull/2502))
* Fixed serialization order of resource properties in the IDL ([#2513](https://github.com/smithy-lang/smithy/pull/2513))
* Fixed `restXml` protocol test to be consistent with other tests and be less confusing ([#2520](https://github.com/smithy-lang/smithy/pull/2520))
* Fixed validation of shape ids for resource identifier bindings ([#2526](https://github.com/smithy-lang/smithy/pull/2526))
* Fixed bug where null was being returned instead of empty collection in trait-codegen ([#2530](https://github.com/smithy-lang/smithy/pull/2530))
* Fixed conversion of `oneOf` errors so that they are treated as untagged unions ([#2532](https://github.com/smithy-lang/smithy/pull/2532))
* Fixed bug where documentation was being applied twice with dynamic documentation trait in the idl-serializer ([#2544](https://github.com/smithy-lang/smithy/pull/2544))

### Documentation

* Added links to SigV4a spec ([#2503](https://github.com/smithy-lang/smithy/pull/2503))
* Fixed `cfnDefaultValue` trait selector documentation to match what is defined in code ([#2509](https://github.com/smithy-lang/smithy/pull/2509))
* Added a Smithy Java quickstart guide ([#2517](https://github.com/smithy-lang/smithy/pull/2517), [#2521](https://github.com/smithy-lang/smithy/pull/2521), [#2525](https://github.com/smithy-lang/smithy/pull/2525))
* Fixed incorrect examples and typos in mixin specification ([#2518](https://github.com/smithy-lang/smithy/pull/2518))
* Added a Smithy Java client user-guide ([#2522](https://github.com/smithy-lang/smithy/pull/2522), [#2531](https://github.com/smithy-lang/smithy/pull/2531), [#2533](https://github.com/smithy-lang/smithy/pull/2533))
* Added documentation placeholders for other languages ([#2534](https://github.com/smithy-lang/smithy/pull/2534))


## 1.54.0 (2025-01-08)

### Features
* Added multiselect and filter to supported `operationContextParams` paths ([#2442](https://github.com/smithy-lang/smithy/pull/2442))
* Integrated Spotless formatter into build logic to automatically format Java and Kotlin code ([#2485](https://github.com/smithy-lang/smithy/pull/2485))
* Added help text to `ResourceOperationInputOutput` event ([#2489](https://github.com/smithy-lang/smithy/pull/2489))
* Added `smithy-docgen` package that enables the generation of a service documentation site from a smithy model ([#2488](https://github.com/smithy-lang/smithy/pull/2488))
* Updated `ShouldHaveUsedTimestampValidator` to reduce false positives ([#2480](https://github.com/smithy-lang/smithy/pull/2480))
* Added service ID to tagging validator error messages to aid debugging ([#2483](https://github.com/smithy-lang/smithy/pull/2483))

### Bug Fixes
* Corrected variable expansion logic in CLI to support multiple variables ([#2495](https://github.com/smithy-lang/smithy/pull/2495))
* Added missing getters to CloudFormation `ResourceSchema` ([#2486](https://github.com/smithy-lang/smithy/pull/2486))
* Converted blob default values to Base64 for protocol tests ([#2474](https://github.com/smithy-lang/smithy/pull/2474))
* Fixed smoke test validator to correctly enforce unique test case ids ([#2482](https://github.com/smithy-lang/smithy/pull/2482))

### Documentation
* Updated Smithy logo in documentation ([#2478](https://github.com/smithy-lang/smithy/pull/2478), [#2479](https://github.com/smithy-lang/smithy/pull/2479))

### Other
* Updated build logic to now require JDK17+ for development ([#2487](https://github.com/smithy-lang/smithy/pull/2487))
* Migrated Gradle build logic to use conventions plugins ([#2484](https://github.com/smithy-lang/smithy/pull/2484))

## 1.53.0 (2024-11-18)

### Features

* Added a transform to mark required idempotency tokens client optional ([#2466](https://github.com/smithy-lang/smithy/pull/2466))
* Updated the IDL serializer to write metadata to a separate file ([#2464](https://github.com/smithy-lang/smithy/pull/2464))
* Expanded the `title` trait to be applicable to any non-member shape ([#2461](https://github.com/smithy-lang/smithy/pull/2461))
* Added a pagination flattening transform ([#2454](https://github.com/smithy-lang/smithy/pull/2454))
* Added transforms to remove deprecated shapes ([#2452](https://github.com/smithy-lang/smithy/pull/2452))
* Relaxed the pattern on the `@defineConditionKeys` trait's keys to enable inferring the `service` to be the service's `arnNamespace` ([#2450](https://github.com/smithy-lang/smithy/pull/2450))
* Added the `useInlineMaps` JSONSchema setting to allow users to configure JSON Schema conversion to inline converted map shapes instead of creating references ([#2449](https://github.com/smithy-lang/smithy/pull/2449))
* Updated the CFN resource schema generation to fill in the permissions field of the tagging configuration for resources ([#2446](https://github.com/smithy-lang/smithy/pull/2446))
* Added `resourceDelimiter` and `reusable` properties to the `arn` trait ([#2440](https://github.com/smithy-lang/smithy/pull/2440))
* Added a validator for the xmlFlattened trait that checks if the member's target is a list that has a member with xmlName, and that xmlName doesn't match the name of the xmlFlattened member ([#2439](https://github.com/smithy-lang/smithy/pull/2439))

### Bug Fixes

* Updated CloudFormation resource schema conversion to be round-trippable ([#2445](https://github.com/smithy-lang/smithy/pull/2445/))
* Fixed for tagsProperty in CFN schema creation ([#2444](https://github.com/smithy-lang/smithy/pull/2444))
* Deferred the `scrubTraitDefinitions` call inside JSON Schema deconflicting to happen only when the model is in a state that would have an avoidable conflict ([#2435](https://github.com/smithy-lang/smithy/pull/2435))
* Updated the `ChangedMemberTarget` diff evaluator to properly check changes to map keys and values the same way it checks changes to list members ([#2434](https://github.com/smithy-lang/smithy/pull/2434))

### Documentation

* Fixed a typo in event stream content-type documentation ([#2458](https://github.com/smithy-lang/smithy/pull/2458))
* Fixed a broken README link ([#2457](https://github.com/smithy-lang/smithy/pull/2457))

### Other

* Updated blob defaults for protocol tests ([#2467](https://github.com/smithy-lang/smithy/pull/2467))
* Downgraded a noisy log statement ([#2451](https://github.com/smithy-lang/smithy/pull/2451))
* Fixed the CBOR type for blobs in RPCv2 CBOR protocol tests ([#2447](https://github.com/smithy-lang/smithy/pull/2447))
* Updated protocol tests to use lower-cased headers ([#2437](https://github.com/smithy-lang/smithy/pull/2437))
* Updated server protocol tests to assert serialization of empty headers ([#2433](https://github.com/smithy-lang/smithy/pull/2433))
* Lowered the severity of `UnboundTestOperation` to `WARNING` ([#2432](https://github.com/smithy-lang/smithy/pull/2432))

## 1.52.1 (2024-10-22)

### Bug Fixes

* Fixed several minor issues with IAM trait class implementations ([#2427](https://github.com/smithy-lang/smithy/pull/2427))
* Fixed an issue where several protocol test operations were not bound to services ([#2426](https://github.com/smithy-lang/smithy/pull/2426))
* Fixed the name of XML root nodes in the protocol tests ([#2423](https://github.com/smithy-lang/smithy/pull/2423))

## 1.52.0 (2024-10-16)

### Features

* Added validator for identifiers missing references ([#2418](https://github.com/smithy-lang/smithy/pull/2418))
* Added checksum algorithm to model enum  ([#2419](https://github.com/smithy-lang/smithy/pull/2419))
* Updated the `httpChecksum` trait spec ([#2413](https://github.com/smithy-lang/smithy/pull/2413))
* Added `breakingChanges` to the `httpChecksum` trait ([#2398](https://github.com/smithy-lang/smithy/pull/2398))
* Use the most common suffix for IDL inline IO ([#2397](https://github.com/smithy-lang/smithy/pull/2397))

### Bug Fixes

* Fixed the name of XML root nodes in the protocol tests ([#2423](https://github.com/smithy-lang/smithy/pull/2423))
* Fixed trait codegen to properly support lists of enums ([#2420](https://github.com/smithy-lang/smithy/pull/2420))
* Update RPC v2 CBOR spec to require an Accept header for requests ([#2417](https://github.com/smithy-lang/smithy/pull/2417))
* Changed prefix header tests to allow for empty headers ([#2415](https://github.com/smithy-lang/smithy/pull/2415))
* Fixed OpenAPI plugin to properly support effective documentation precedence ([#2402](https://github.com/smithy-lang/smithy/pull/2402))
* Fixed a bug in the `SigV4` diff logic that assumed that the a service exists in the old operation ([#2405](https://github.com/smithy-lang/smithy/pull/2405))
* Added `NodeValidationVisitor` checks for invalid nulls ([#2393](https://github.com/smithy-lang/smithy/pull/2393))

### Documentation

* Updated the `httpChecksum` trait spec to clarify the expected behavior and added the supported algorithms ([#2413](https://github.com/smithy-lang/smithy/pull/2413))
* Fixed the `httpChecksum` algorithm list in spec ([#2394](https://github.com/smithy-lang/smithy/pull/2394))
* Fixed a broken link to smithy-rs design document in Code Gen documentation. ([#2416](https://github.com/smithy-lang/smithy/pull/2416))
* Fixed resource type typo in AST docs ([#2410](https://github.com/smithy-lang/smithy/pull/2410))
* Fixed small typos in full stack tutorial ([#2389](https://github.com/smithy-lang/smithy/pull/2389))

## 1.51.0 (2024-09-03)

### Features

* Added the `:recursive` selector function to enable recursively traversing relationships. ([#2353](https://github.com/smithy-lang/smithy/pull/2353))
* Added validation of `intEnum` shapes to the `NodeValidationVisitor`. ([#2357](https://github.com/smithy-lang/smithy/pull/2357))
* Added various protocol tests. ([#2333](https://github.com/smithy-lang/smithy/pull/2333), [#2342](https://github.com/smithy-lang/smithy/pull/2342))
* Added elliptic curve cryptography module to the Smithy CLI for communicating with certain package managers. ([#2379](https://github.com/smithy-lang/smithy/pull/2379)) 
* Added a warning when the `@idempotencyToken` trait is applied where it would be ignored. ([#2358](https://github.com/smithy-lang/smithy/pull/2358))
* Added validation for the `@httpChecksum` trait's `responseAlgorithms` property. ([#2371](https://github.com/smithy-lang/smithy/pull/2371))
* Updated list of supported `@httpCheckum` algorithms. ([#2386](https://github.com/smithy-lang/smithy/pull/2386))
* Improved the performance of the `BottomUpIndex`. ([#2367](https://github.com/smithy-lang/smithy/pull/2367))

### Bug Fixes

* Fixed formatting of `resource` shape `identifiers` and `properties` fields. ([#2377](https://github.com/smithy-lang/smithy/pull/2377))
* Fixed issue with parsing different types of documentation comments. ([#2390](https://github.com/smithy-lang/smithy/pull/2390))
* Fixed issue that would cause shape conflicts when loading the same model twice that uses `apply` on a member from a
  mixin. ([#2378](https://github.com/smithy-lang/smithy/pull/2378))
* Fixed issue with generating CFN resource schema handler permissions for `@noReplace` resources. ([#2383](https://github.com/smithy-lang/smithy/pull/2383))
* Fixed several classes of bugs in the `AwsTagIndex`. ([#2384](https://github.com/smithy-lang/smithy/pull/2384))
* Fixed various issues with protocol tests. ([#2336](https://github.com/smithy-lang/smithy/pull/2336), [#2340](https://github.com/smithy-lang/smithy/pull/2340)
  [#2341](https://github.com/smithy-lang/smithy/pull/2341))

### Documentation

* Added full-stack application tutorial. ([#2362](https://github.com/smithy-lang/smithy/pull/2362))
* Added documentation to several places in the `smithy-aws-endpoints` trait definitions. ([#2374](https://github.com/smithy-lang/smithy/pull/2374))
* Added note on `smithy-build.json` supporting `//` based comments. ([#2375](https://github.com/smithy-lang/smithy/pull/2375))
* Clarified documentation for resolving auth schemes from endpoint rule sets. ([#2382](https://github.com/smithy-lang/smithy/pull/2382))
* Fixed various documentation issues. ([#2346](https://github.com/smithy-lang/smithy/pull/2346), [#2363](https://github.com/smithy-lang/smithy/pull/2363))

## 1.50.0 (2024-06-18)

### Features

* Added a `required` property to IAM trait condition key definitions. ([#2288](https://github.com/smithy-lang/smithy/pull/2288))
* Added `syncCorsPreflightIntegration` configuration option to APIGateway conversion, which updates CORS preflight templates with all possible content types. ([#2290](https://github.com/smithy-lang/smithy/pull/2290))
* Added validator for duplicate names in the `iamResource` trait. ([#2293](https://github.com/smithy-lang/smithy/pull/2293))
* Added `operationContextParams` support to `RulesetParameterValidator`. ([#2295](https://github.com/smithy-lang/smithy/pull/2295))
* Enabled the application of example traits to service-level errors. ([#2307](https://github.com/smithy-lang/smithy/pull/2307))
* Added IDL serializer option to coerce inline IO. ([#2316](https://github.com/smithy-lang/smithy/pull/2316))
* Added a function to writer delegators to check out writers with a symbol. ([#2328](https://github.com/smithy-lang/smithy/pull/2328))
* Added defaults tests for restJson1. ([#2280](https://github.com/smithy-lang/smithy/pull/2280))
* Added float16 upcast tests for RPCv2 CBOR. ([#2291](https://github.com/smithy-lang/smithy/pull/2291))
* Added protocol tests for content-type parameters. ([#2296](https://github.com/smithy-lang/smithy/pull/2296))
* Added protocol tests asserting servers reject empty unions. ([#2300](https://github.com/smithy-lang/smithy/pull/2300))
* Added protocol tests for malformed media types. ([#2309](https://github.com/smithy-lang/smithy/pull/2309))
* Added protocol tests for missing content types. ([#2310](https://github.com/smithy-lang/smithy/pull/2310))
* Added several content-type and HTTP payload protocol tests. ([#2314](https://github.com/smithy-lang/smithy/pull/2314), [#2315](https://github.com/smithy-lang/smithy/pull/2315), [#2322](https://github.com/smithy-lang/smithy/pull/2322), [#2331](https://github.com/smithy-lang/smithy/pull/2331))

### Bug Fixes

* Fixed formatter to correctly convert invalid doc comments. ([#2277](https://github.com/smithy-lang/smithy/pull/2277))
* Added missing node mapper for document types. ([#2313](https://github.com/smithy-lang/smithy/pull/2313))
* Fixed issues with S3 dot segment tests. ([#2304](https://github.com/smithy-lang/smithy/pull/2304))
* Fixed several issues in RPCv2 CBOR protocol tests. ([#2319](https://github.com/smithy-lang/smithy/pull/2319), [#2320](https://github.com/smithy-lang/smithy/pull/2320))
* Updated protocol tests to use floating point values representable exactly in IEEE representation. ([#2321](https://github.com/smithy-lang/smithy/pull/2321))
* Fixed EC2 request ID casing. ([#2329](https://github.com/smithy-lang/smithy/pull/2329))

### Documentation

* Fixed typos in RPCv2 CBOR spec. ([#2278](https://github.com/smithy-lang/smithy/pull/2278))
* Fixed typo in AddDefaultConfigSettings. ([#2285](https://github.com/smithy-lang/smithy/pull/2285))
* Fixed errors in IAM trait docs. ([#2287](https://github.com/smithy-lang/smithy/pull/2287))
* Clarified RPCv2 response event stream behavior. ([#2297](https://github.com/smithy-lang/smithy/pull/2297))
* Replaced references to outdated RFCs with references to their replacements. ([#2298](https://github.com/smithy-lang/smithy/pull/2298))
* Clarified `httpResponseCode` value range. ([#2308](https://github.com/smithy-lang/smithy/pull/2308))

## 1.49.0 (2024-05-08)

### Features

* Added list and map shapes to directed codegen. ([#2273](https://github.com/smithy-lang/smithy/pull/2273))
* Added support for defaults on trait member values in trait-codegen. ([#2267](https://github.com/smithy-lang/smithy/pull/2267))
* Added support for string-arrays for as parameters in rules-engine. ([#2266](https://github.com/smithy-lang/smithy/pull/2266))
* Added new trait (`@operationContextParams`) for binding array parameters to nested operation inputs in the rules-engine. ([#2264](https://github.com/smithy-lang/smithy/pull/2264))

### Bug Fixes

* Fixed bug in the formatter where comments in operation errors were not being handled correctly. ([#2283](https://github.com/smithy-lang/smithy/pull/2283))
* Fixed bug in the formatter where certain documentation comments were being converted to line-comments. ([#2277](https://github.com/smithy-lang/smithy/pull/2277))
* Fixed incorrect status code in restXml response protocol test. ([#2272](https://github.com/smithy-lang/smithy/pull/2272))
* Fixed ec2query protocol test for empty list serialization. ([#2269](https://github.com/smithy-lang/smithy/pull/2269))
* Fixed Javadoc integration for documentation in members and enum variants in trait-codegen. ([#2265](https://github.com/smithy-lang/smithy/pull/2265)) 
* Fixed timing issue in the delay calculation for waiter retries. ([#2259](https://github.com/smithy-lang/smithy/pull/2259))

### Documentation

* Corrected a typo in RPCv2 CBOR specificaiton. ([#2278](https://github.com/smithy-lang/smithy/pull/2278))
* Updated casing of aggregate shape names in shape type table. ([#2271](https://github.com/smithy-lang/smithy/pull/2271))

## 1.48.0 (2024-04-24)

### Features

* Updated HTTP binding validation to allow for specificity routing.
  ([#2220](https://github.com/smithy-lang/smithy/pull/2220))
* Added support for the `deprecated` trait in OpenAPI conversion.
  ([#2221](https://github.com/smithy-lang/smithy/pull/2221),
  [#2222](https://github.com/smithy-lang/smithy/pull/2222))
* Added protocol tests for nested XML maps with XML names.
  ([#2236](https://github.com/smithy-lang/smithy/pull/2236))
* Added diff validation for services that migrate from sigv4 to sigv4a.
  ([#2245](https://github.com/smithy-lang/smithy/pull/2245))
* Emit a `NOTE` validation event on ignored duplicate shapes.
  ([#2247](https://github.com/smithy-lang/smithy/pull/2247))
* Added strongly typed properties for `TypedPropertiesBag`.
  ([#2248](https://github.com/smithy-lang/smithy/pull/2248))
* Updated trait code generation to use strongly typed property bags.
  ([#2254](https://github.com/smithy-lang/smithy/pull/2254))
* Added a Smithy Diff test runner.
  ([#2250](https://github.com/smithy-lang/smithy/pull/2250))
* Added captializing formatter for use in trait code generation and normalized
  symbol references to reduce false positive duplicates.
  ([#2255](https://github.com/smithy-lang/smithy/pull/2255))

### Bug Fixes

* Removed incorrect `Content-Type` from no-body XML payload protocol test.
  ([#2218](https://github.com/smithy-lang/smithy/pull/2218))
* Fixed header expectations in RPCv2 protocol tests.
  ([#2246](https://github.com/smithy-lang/smithy/pull/2246))
* Fixed `ModifiedTrait` validation for traits with breaking change rules.
  ([#2249](https://github.com/smithy-lang/smithy/pull/2249))

### Documentation

* Fixed Gradle plugin version in documentation.
  ([#2226](https://github.com/smithy-lang/smithy/pull/2226))
* Updated the Gradle migration guide caption.
  ([#2227](https://github.com/smithy-lang/smithy/pull/2227))
* Updated installation instructions for the Smithy CLI.
  ([#2229](https://github.com/smithy-lang/smithy/pull/2229))
* Added missing commas in JSON AST docs.
  ([#2230](https://github.com/smithy-lang/smithy/pull/2230))
* Fixed incorrect reference to the `traitValidators` trait in the protocol
  definition trait. ([#2241](https://github.com/smithy-lang/smithy/pull/2241/))

## 1.47.0 (2024-03-28)

### Features
* Added the `smithy-trait-codegen` package. This package provides a new `trait-codegen` plugin that 
  can be used to generate Java implementations of Smithy traits, removing the need to hand-write 
  most trait implementations. ([#2074](https://github.com/smithy-lang/smithy/pull/2074))
* Added the `@smithy.protocols#rpcv2Cbor` protocol trait. Smithy RPC v2 CBOR is an RPC-based protocol over HTTP that
  sends requests and responses with [CBOR](https://www.rfc-editor.org/rfc/rfc8949.html) payloads. This trait is 
  available in the new `smithy-protocol-traits` package, with protocol tests available in the new
  `smithy-protocol-tests` package. ([#2212](https://github.com/smithy-lang/smithy/pull/2212))
* Updated several protocol tests around the `@sparse` trait to ease implementation. ([#2206](https://github.com/smithy-lang/smithy/pull/2206)) 
* Remove content-type from no XML body protocol test. ([#2218](https://github.com/smithy-lang/smithy/pull/2218))

### Bug Fixes

* Fixed a bug where all shapes would fail to load in a file if a mixin was missing. ([#2214](https://github.com/smithy-lang/smithy/pull/2214))

### Documentation

* Clarified HTTP protocol compliance test `params` field docs. ([#2202](https://github.com/smithy-lang/smithy/pull/2202))

## 1.46.0 (2024-03-19)

### Features

* Added protocol tests for `null` values in unions. ([#2180](https://github.com/smithy-lang/smithy/pull/2180))
* Updated `ec2QueryName` tests to reflect usage. ([#2186](https://github.com/smithy-lang/smithy/pull/2186))
* Updated model validation and protocol tests to use the new Gradle plugins. ([#2176](https://github.com/smithy-lang/smithy/pull/2176))
* Added data-shape-only visitor class. ([#2168](https://github.com/smithy-lang/smithy/pull/2168))
* Added protocol tests for S3 when dots are part of a key segment. ([#2166](https://github.com/smithy-lang/smithy/pull/2166))
* Added the `@traitValidators` trait that can constrain shape closures. ([#2156](https://github.com/smithy-lang/smithy/pull/2156))
* Added logic to infer default inline I/O suffixes in the IDL serializer. ([#2122](https://github.com/smithy-lang/smithy/pull/2122))
* Added annotation processor base class for executing Smithy-Build plugins. ([#2073](https://github.com/smithy-lang/smithy/pull/2073))

### Bug Fixes

* Fixed a bug when creating `@standardRegionalEndpoints` nodes. ([#2179](https://github.com/smithy-lang/smithy/pull/2179))
* Fixed Windows CI errors by removing no-cone on git add command. ([#2168](https://github.com/smithy-lang/smithy/pull/2168))
* Fixed the `RestJsonZeroAndFalseQueryValues` protocol test. ([#2167](https://github.com/smithy-lang/smithy/pull/2167))
* Properly fix multi-mixin members in shape build. ([#2157](https://github.com/smithy-lang/smithy/pull/2157))
* Fixed text block incidental whitespace handling. ([#2147](https://github.com/smithy-lang/smithy/pull/2147))
* Fixed issues with `AbstractCodeWriter` state stacks. ([#2142](https://github.com/smithy-lang/smithy/pull/2142))

### Documentation

* Remove incorrect `jsonName` protocol info. ([#2187](https://github.com/smithy-lang/smithy/pull/2187))
* Fixed minor formattting. ([#2175](https://github.com/smithy-lang/smithy/pull/2175))
* Added CLI and Groovy examples to guides. ([#2165](https://github.com/smithy-lang/smithy/pull/2165))
* Updated quickstart example to use terse input/output syntax. ([#2163](https://github.com/smithy-lang/smithy/pull/2163))
* Updated OpenApi guide to use new gradle plugin and cli. ([#2161](https://github.com/smithy-lang/smithy/pull/2161))
* Updated Gradle quickstart example to include empty `smithy-build.json`. ([#2149](https://github.com/smithy-lang/smithy/pull/2149))
* Multiple updates of the Gradle plugin after the 0.8.0 release. ([#2140](https://github.com/smithy-lang/smithy/pull/2140), [#2146](https://github.com/smithy-lang/smithy/pull/2146), [#2139](https://github.com/smithy-lang/smithy/pull/2139), [#2148](https://github.com/smithy-lang/smithy/pull/2148))

## 1.45.0 (2024-02-14)

### Features

* Added new option to CLI to configure the format (`text` or `csv`) of validation output. ([#2133](https://github.com/smithy-lang/smithy/pull/2133))
* Added options to CLI to hide or show validation events for specified validators. ([#2127](https://github.com/smithy-lang/smithy/pull/2127))
* Added protocol tests for verifying serialization/deserialization behavior for maps with document values. ([#2125](https://github.com/smithy-lang/smithy/pull/2125))
* Changed `UnreferencedShape` to be an opt-in linter instead of on-by-default to reduce friction when defining common shapes that are not connected to a service shape. Added a validator that allows you to configure what shape to check connectedness (defaults to service shape). ([#2119](https://github.com/smithy-lang/smithy/pull/2119))

### Bug Fixes

* Fixed headers when printing validation event output in csv format. ([#2136](https://github.com/smithy-lang/smithy/pull/2136))
* Fixed `RestJsonZeroAndFalseQueryValues` protocol test to correctly include a params value for servers. ([#2132](https://github.com/smithy-lang/smithy/pull/2132))
* Fixed regression in model validation that incorrectly allowed suppression of ERROR events. ([#2130](https://github.com/smithy-lang/smithy/pull/2130))

### Documentation

* Fixed typo in @pattern validation example. ([#2126](https://github.com/smithy-lang/smithy/pull/2126))


## 1.44.0 (2024-01-25)

### Features

* Add AWS smoke test model package. ([#2113](https://github.com/smithy-lang/smithy/pull/2113))
* Add more traits to protocol test services ([#2117](https://github.com/smithy-lang/smithy/pull/2117))
* Enable custom inline suffixes in IDL serializer ([#2121](https://github.com/smithy-lang/smithy/pull/2121))
* Keep trailing doc comment spaces in IDL serializer ([#2116](https://github.com/smithy-lang/smithy/pull/2116))
* Expand protocol tests for default values. ([#2049](https://github.com/smithy-lang/smithy/pull/2049))
* Add protocol tests for 0/false in query params. ([#2070](https://github.com/smithy-lang/smithy/pull/2070))
* Change line break formatting in brackets. ([#2072](https://github.com/smithy-lang/smithy/pull/2072))
* Add backticks to diff messages for trait changes. ([#2075](https://github.com/smithy-lang/smithy/pull/2075))
* Support internal trait when building synthetic enum trait. ([#2106](https://github.com/smithy-lang/smithy/pull/2106))
* Add "critical" validation phase to validation. ([#2098](https://github.com/smithy-lang/smithy/pull/2098))
* Deprecated IAM Action traits that are now formally superseded by `@iamAction`. ([#2095](https://github.com/smithy-lang/smithy/pull/2095))
* Added ability to override AWS endpoints partitions when needed. ([#2092](https://github.com/smithy-lang/smithy/pull/2092))

### Bug Fixes

* Fail when duplicate members are found in enum/intEnum shapes. ([#2112](https://github.com/smithy-lang/smithy/pull/2112))
* Check references via idRef when looking for unreferenced shapes. ([#2105](https://github.com/smithy-lang/smithy/pull/2105))
* Remove service renames after flattening namespaces. ([#2109](https://github.com/smithy-lang/smithy/pull/2109))
* Fixed issue where endpoint modifier traits without a valid shape definition were being indexed. ([#2096](https://github.com/smithy-lang/smithy/pull/2096))

### Documentation

* Upgrade sphinx for docs. ([#2100](https://github.com/smithy-lang/smithy/pull/2100))
* Updated several of the guide sections and tidied up the layout. ([#2097](https://github.com/smithy-lang/smithy/pull/2097))


## 1.43.0 (2024-01-05)

### Features

* Updated `RemovedShape` diff event severity from `ERROR` to `WARNING` for scalar shapes. ([#2037](https://github.com/smithy-lang/smithy/pull/2037))
* Made `parameterizedTestSource` public, allowing users to use a customized suite as a source for JUnit parameterized tests. ([#2087](https://github.com/smithy-lang/smithy/pull/2087))
* Refactored `ReplaceShapes` transform to improve efficiency. ([#2082](https://github.com/smithy-lang/smithy/pull/2082))
* Added validation for endpoint patterns used by `standardRegionalEndpoints` and `standardPartitionalEndpoints`. ([#2069](https://github.com/smithy-lang/smithy/pull/2069))
* Added support for CLI dependency resolution via proxy. ([#2076](https://github.com/smithy-lang/smithy/pull/2076))
* Improved efficiency of `ReplaceShapes` transform by only building container shapes once when multiple members are changed. ([#2081](https://github.com/smithy-lang/smithy/pull/2081))
* Moved `allowOptionalNull` to `NodeValidationVisitor.Feature`. ([#2080](https://github.com/smithy-lang/smithy/pull/2080))
* Added rules engine built-in for `AccountIdEndpointMode`. ([#2065](https://github.com/smithy-lang/smithy/pull/2065))
* Added [JReleaser](https://jreleaser.org/) config. ([#2059](https://github.com/smithy-lang/smithy/pull/2059))
* Added ability to find all operations for which a shape is used as an input, output, or error. ([#2064](https://github.com/smithy-lang/smithy/pull/2064))
* Split InputOutput shapes into separate request and response shapes for `restXml` protocol tests. ([#2063](https://github.com/smithy-lang/smithy/pull/2063))

### Bug Fixes

* Fixed an issue where `@iamAction` wasn't reflected in CFN resource schema creation. ([#2091](https://github.com/smithy-lang/smithy/pull/2091)) 
* Fixed tree node start and end locations. ([#2084](https://github.com/smithy-lang/smithy/pull/2084))
* Fixed several minor build warnings. ([2089](https://github.com/smithy-lang/smithy/pull/2089))
* Fixed protocol test service signing name for `awsJson1_1` protocol. ([#2089](https://github.com/smithy-lang/smithy/pull/2089))
* Updated member removal for `ReplaceShapes` transform to ensure enum and intEnum members are correctly removed. ([#2082](https://github.com/smithy-lang/smithy/pull/2082))
* Corrected erroneous outer tags in `restXml` protocol tests ([#2071](https://github.com/smithy-lang/smithy/pull/2071))

### Documentation

*  Added documentation for configuring CLI dependency resolution via proxy. ([#2083](https://github.com/smithy-lang/smithy/pull/2083))

## 1.42.0 (2023-12-07)

### Features

* Added the `@aws.auth#sigv4a` auth trait. ([#2032](https://github.com/smithy-lang/smithy/pull/2032))
* Added the `timestampFormat` and `httpChecksumRequired` traits to protocols. ([#2054](https://github.com/smithy-lang/smithy/pull/2054), [#2061](https://github.com/smithy-lang/smithy/pull/2061))

### Bug Fixes

* Fixed conversion of root `intEnum` shape to IDL 1.0 when the shape doesn't have a default value of 0. ([#2053](https://github.com/smithy-lang/smithy/pull/2053))
* Fixed equality of `@examples` traits by overriding the `equals` method in `ExampleTrait.ErrorExample`. ([#2052](https://github.com/smithy-lang/smithy/pull/2052))

### Documentation

* Updated documentation for auth traits. ([#2051](https://github.com/smithy-lang/smithy/pull/2051))
* Added documentation for smoke tests. ([#2057](https://github.com/smithy-lang/smithy/pull/2057))

## 1.41.1 (2023-11-16)

### Features

* Added support for `sdkId`s with a single character ([#2043](https://github.com/smithy-lang/smithy/pull/2043))

### Bug Fixes

* Fixed `toShapeId` call in `EndpointModifierIndex`  ([#2044](https://github.com/smithy-lang/smithy/pull/2044))
* Fixed removal of applied non-prelude meta traits ([#2042](https://github.com/smithy-lang/smithy/pull/2042))
* Fixed `recommended` trait provider to avoid discarding `reason` ([#2041](https://github.com/smithy-lang/smithy/pull/2041))

### Documentation

* Updated the javadoc of `ValidatedResult.unwrap` to note that `DANGER` events also throw a validation event ([#2040](https://github.com/smithy-lang/smithy/pull/2040))

## 1.41.0 (2023-11-08)

### Features

* Added new member to `@aws.iam#iamResource` for disabling condition key inheritance. ([#2036](https://github.com/smithy-lang/smithy/pull/2036))
* Added new trait for defining IAM actions, which consolidates and deprecates several older IAM traits. ([#2034](https://github.com/smithy-lang/smithy/pull/2034)) 
* Added convenience method, `expectIntEnumShape`, to `GenerateIntEnumDirective` to get an `IntEnumShape` ([#2033](https://github.com/smithy-lang/smithy/pull/2033))
* Added plugin to `NodeValidationVisitor` to ensure collections with `@uniqueItems` trait have unique-ness enforced. ([#2031](https://github.com/smithy-lang/smithy/pull/2031))
* Added new member to `@aws.iam#iamResource` and `@aws.iam#defineConditionKeys` traits for defining a relative URL path of documentation. ([#2027](https://github.com/smithy-lang/smithy/pull/2027))
* Migrated IAM traits JSON file to IDL file in `smithy-aws-iam-traits`. ([#2026](https://github.com/smithy-lang/smithy/pull/2026))
* Added protocol test for verifying behavior when handling unknown union members in the `restJson1` protocol. ([#2022](https://github.com/smithy-lang/smithy/pull/2022))
* Enabled `aws.iam#disableConditionKeyInference` trait to be applicable to service shapes. ([#2019](https://github.com/smithy-lang/smithy/pull/2019))
* Updated `partitions.json` with two new entries, `aws-iso-e` and `aws-iso-f` to be consistent with SDKs. ([#2018](https://github.com/smithy-lang/smithy/pull/2018))
* Added event-id subparts to `ClientEndpointDiscoveryValidator` to clarify validation events. ([#2017](https://github.com/smithy-lang/smithy/pull/2017))
* Added configuration for plugin integrations in `smithy-build.json`. ([#2014](https://github.com/smithy-lang/smithy/pull/2014))
* Added protocol tests for verifying behavior of default values in the `awsJson1_1` protocol. ([#2002](https://github.com/smithy-lang/smithy/pull/2002))
* Added several new traits for modelling declarative endpoints. ([#1987](https://github.com/smithy-lang/smithy/pull/1987))

### Documentation

* Added basic website analytics so that engagement can be measured. ([#2025](https://github.com/smithy-lang/smithy/pull/2025))
* Added documentation for new traits added for declarative endpoints. ([#2013](https://github.com/smithy-lang/smithy/pull/2013))

### Bug Fixes

* Fixed handling and deconflicting of duplicate apply statements targetting mixed-in members ([#2030](https://github.com/smithy-lang/smithy/pull/2030))
* Fixed an NPE in the `PluginContext.toBuilder` method in `PluginContext`. ([#2028](https://github.com/smithy-lang/smithy/pull/2028))
* Fixed a trait parse error for shape IDs. [#2023](https://github.com/smithy-lang/smithy/pull/2023))
* Fixed several major issues with how neighbors and model graph traversal was implemented. ([#2020](https://github.com/smithy-lang/smithy/pull/2020))
* Added expect-check to mitigate NSE exception in `PrivateAccessValidator`. ([#2015](https://github.com/smithy-lang/smithy/pull/2015))
* Fixed equality of `@examples` traits by overriding the `equals` method in `ExampleTrait`. ([#2009](https://github.com/smithy-lang/smithy/pull/2009))

## 1.40.0 (2023-10-16)

### Features

* Added new protocol tests for the `restXml` protocol, which assert request/response behaviors for string payloads. ([#2007](https://github.com/smithy-lang/smithy/pull/2007))
* Added new package, `smithy-smoke-test-traits`, which defines the traits for smoke tests. This package contains the smithy model definitions of said traits, their java implementations, and unit tests. ([#2005](https://github.com/smithy-lang/smithy/pull/2005))
* Added auth-scheme validator that runs for SigV4 sub-schemes as part of the AWS rule-set in `smithy-rules-engine`. ([#2000](https://github.com/smithy-lang/smithy/pull/2000))
* Added `AccountId` and `CredentialScope` parameters for AWS-specific endpoint rules in `smithy-rules-engine`. ([#1993](https://github.com/smithy-lang/smithy/pull/1993))

### Documentation

* Added traits anchors for a few traits that were previously missing. ([#2008](https://github.com/smithy-lang/smithy/pull/2008))
* Added `Smithy Examples` embedding to the [smithy.io](https://smithy.io) sidebar under `Project`. ([#2006](https://github.com/smithy-lang/smithy/pull/2006))
* Added important notice for the `@contextParam` trait to clarify expected behavior of clients when `@required` is used on the same member.([#1999](https://github.com/smithy-lang/smithy/pull/1999))

### Bug Fixes

* Fixed missing source-locations in emitted events by `smithy-diff`. Previously, `N/A` would be displayed instead of the real location. ([#2001](https://github.com/smithy-lang/smithy/pull/2001))
* Added missing method override in `smithy-rules-engine`. ([#1998](https://github.com/smithy-lang/smithy/pull/1998))
* Fixed bug where properties of resource shapes were not being serialized in the IDL serializer. ([#1996](https://github.com/smithy-lang/smithy/pull/1996))
* Fixed an issue with OpenAPI conversion not allowing multiple errors for a single status code with an opt-in that uses `oneOf` to de-conflict the errors. ([#1995](https://github.com/smithy-lang/smithy/pull/1995))

## 1.39.1 (2023-09-26)

### Bug Fixes

* Fix several issues with validating `authSchemes` configurations for endpoints in the
  `smithy-rules-engine` package for both AWS and non-AWS issues. ([#1990](https://github.com/smithy-lang/smithy/pull/1990))

## 1.39.0 (2023-09-25)

### Features

* Refactored `smithy-rules-engine` significantly in an effort to improve validation, separate 
  AWS and non-AWS concerns, add a specification, and more. General notes are provided in 
  individual commit messages. The format of the rules documents have not changed, meaning a 
  successful migration to the refactored codebase will involve no changes to code generated 
  for an SDK client ([#1855](https://github.com/smithy-lang/smithy/pull/1855))
* Added `Sha1` checksum to `ResolvedArtifacts` ([#1979](https://github.com/smithy-lang/smithy/pull/1979))
* Relaxed `Content-Length` in unset union payloads protocol tests ([#1984](https://github.com/smithy-lang/smithy/pull/1984))

### Documentation

* Updated example by keeping operation list from previous examples ([#1981](https://github.com/smithy-lang/smithy/pull/1981))

## 1.38.0 (2023-09-14)

### Features

* Updated auto-formatting to use line breaks for some properties ([#1939](https://github.com/smithy-lang/smithy/pull/1939))
* Updated JSON-based AWS protocols to ignore the `__type` field when deserializing `union`s ([#1945](https://github.com/smithy-lang/smithy/pull/1945))
* Added metadata key to `RemovedMetadata` diff events ([#1940](https://github.com/smithy-lang/smithy/pull/1940))
* Improved equality comparison for `NumberNode` instances ([#1955](https://github.com/smithy-lang/smithy/pull/1955), [#1965](https://github.com/smithy-lang/smithy/pull/1965))
* Added `--aut` as a shortcut for `--allow-unknown-traits` in the Smithy CLI ([#1950](https://github.com/smithy-lang/smithy/pull/1950))
* Added a `--show` option to the Smithy CLI to include extra information like type, source location, and captured
  variables. This deprecates the `--show-vars` option ([#1953](https://github.com/smithy-lang/smithy/pull/1953))
* Added validation to emit warnings when a member has an HTTP trait applied in a context where it is ignored ([#1962](https://github.com/smithy-lang/smithy/pull/1962),
  [#1969](https://github.com/smithy-lang/smithy/pull/1969))
* Added validation to check the consistency of IAM resource names and ARN resource names ([#1954](https://github.com/smithy-lang/smithy/pull/1954))
* Added a `RemoveInvalidDefaults` transform to remove `@default` traits when their values conflict with applied `@range`
  traits ([#1964](https://github.com/smithy-lang/smithy/pull/1964))
* Added an `allowConstraintErrors` property to the `@examples` trait for relaxing content validation requirements ([#1949](https://github.com/smithy-lang/smithy/pull/1949),
  [#1968](https://github.com/smithy-lang/smithy/pull/1968))
* Added several protocol tests for `@restXml` ([#1909](https://github.com/smithy-lang/smithy/pull/1909), [#1908](https://github.com/smithy-lang/smithy/pull/1908),
  [#1574](https://github.com/smithy-lang/smithy/pull/1574))
* Added several protocol tests for `@restJson1` ([#1908](https://github.com/smithy-lang/smithy/pull/1908))

### Documentation

* Clarified how trait values are provided in the IDL ([#1944](https://github.com/smithy-lang/smithy/pull/1944))
* Added the `@length` trait to the specification's trait index ([#1952](https://github.com/smithy-lang/smithy/pull/1952))
* Improved the ability to link to certain sections of the specification ([#1958](https://github.com/smithy-lang/smithy/pull/1958))
* Clarified behavior of `@sigv4` and `@optionalAuth` ([#1963](https://github.com/smithy-lang/smithy/pull/1963), [#1971](https://github.com/smithy-lang/smithy/pull/1971))

### Bug Fixes

* Fixed diff event messages for `ChangedNullability` events ([#1972](https://github.com/smithy-lang/smithy/pull/1972))
* Fixed an NPE when auto-formatting certain types of trait values ([#1942](https://github.com/smithy-lang/smithy/pull/1942))
* Fixed an issue where exceptions thrown when creating traits were not emitted as validation events ([#1947](https://github.com/smithy-lang/smithy/pull/1947))
* Fixed an issue validating timestamp members in nodes where a `@timestampFormat` trait was involved ([#1948](https://github.com/smithy-lang/smithy/pull/1948))
* Fixed an issue where the `FlattenAndRemoveMixins` transform would not remove unused mixins ([#1951](https://github.com/smithy-lang/smithy/pull/1951))
* Fixed a malformed request test for the `@restJson1` protocol ([#1959](https://github.com/smithy-lang/smithy/pull/1959))
* Fixed an issue where `NonInclusiveTerms` validation events would be identical for different text paths. ([#1975](https://github.com/smithy-lang/smithy/pull/1975))

## 1.37.0 (2023-08-22)

### Features
* Formatted operation errors onto multiple lines ([#1933](https://github.com/smithy-lang/smithy/pull/1933))
* Added support for creating specific `TreeType` to smithy-syntax ([#1925](https://github.com/smithy-lang/smithy/pull/1925))
* Added validator for services with noAuth trait ([#1929](https://github.com/smithy-lang/smithy/pull/1929))
* Added ServiceIndex method for noAuth scheme ([#1924](https://github.com/smithy-lang/smithy/pull/1924))
* Added warning on addition of required trait ([#1923](https://github.com/smithy-lang/smithy/pull/1923))
* Added versioning for API Gateway defaults ([#1916](https://github.com/smithy-lang/smithy/pull/1916))
* Added support for enum map keys with OpenApi 3.1.0 ([#1905](https://github.com/smithy-lang/smithy/pull/1905))
* Added support for suppressions to smithy-diff ([#1861](https://github.com/smithy-lang/smithy/pull/1861))
* Added `specificationExtension` trait for OpenAPI extensions ([#1609](https://github.com/smithy-lang/smithy/pull/1609))
* Added `conditionKeyValue` and `conditionKeysResolvedByService` traits ([#1677](https://github.com/smithy-lang/smithy/pull/1677))

### Documentation
* Updated `getAuthSchemes` javadoc ([#1930](https://github.com/smithy-lang/smithy/pull/1930))
* Clarified `default` and `clientOptional` traits ([#1920](https://github.com/smithy-lang/smithy/pull/1920))
* Fixed version numbers in smithy-build.json examples ([#1918](https://github.com/smithy-lang/smithy/pull/1918))
* Clarified ordering of auth schemes in ServiceIndex ([#1915](https://github.com/smithy-lang/smithy/pull/1915))
* Included prelude in spec ([#1913](https://github.com/smithy-lang/smithy/pull/1913))

### Bug Fixes
* Fixed assembler addTraits for some resource models ([#1927](https://github.com/smithy-lang/smithy/pull/1927))

## 1.36.0 (2023-08-03)

### Features
* Allowed disabling format on integers when converting to OpenAPI ([#1904](https://github.com/smithy-lang/smithy/pull/1904))
* Added intEnum support when converting to OpenAPI ([#1898](https://github.com/smithy-lang/smithy/pull/1898))
* Added support for overriding validation severity ([#1890](https://github.com/smithy-lang/smithy/pull/1890))
* Added `disableDefaultValues` option when converting to OpenAPI ([#1887](https://github.com/smithy-lang/smithy/pull/1887))
* Updated brew workflow to use new smithy tap ([#1897](https://github.com/smithy-lang/smithy/pull/1897))
* Added progress tracker and message for CLI while cloning a template ([#1888](https://github.com/smithy-lang/smithy/pull/1888))
* Updated init command to honor quiet setting ([#1889](https://github.com/smithy-lang/smithy/pull/1889))
* Updated appearance of smithy init list output ([#1901](https://github.com/smithy-lang/smithy/pull/1901))
* Added exceptions for invalid paths in template definition ([#1907](https://github.com/smithy-lang/smithy/pull/1907))
* Added Cache template directory in init command ([#1896](https://github.com/smithy-lang/smithy/pull/1896))
* Check for existing directory when creating template with init ([#1885](https://github.com/smithy-lang/smithy/pull/1885))

### Documentation
* Clarified constraint trait enforcement ([#1902](https://github.com/smithy-lang/smithy/pull/1902))
* Better document trait merging ([#1895](https://github.com/smithy-lang/smithy/pull/1895))
* Updated docs to use new smithy-lang tap ([#1893](https://github.com/smithy-lang/smithy/pull/1893))
* Added naming recommendations ([#1892](https://github.com/smithy-lang/smithy/pull/1892))

### Bug Fixes
* Fixed NPE when docId is null when ServiceTrait.equals is called ([#1903](https://github.com/smithy-lang/smithy/pull/1903))
* Fixed off-by-one issues in TokenTree and TreeCursor ([#1891](https://github.com/smithy-lang/smithy/pull/1891))
* Fixed snapshot dependency resolution ([#1884](https://github.com/smithy-lang/smithy/pull/1884))

### Other
* Use standard output for regular messages ([#1894](https://github.com/smithy-lang/smithy/pull/1894))

## 1.35.0 (2023-07-27)

### Features
* Enabled support for SNAPSHOT dependencies ([#1853](https://github.com/smithy-lang/smithy/pull/1853), [#1857](https://github.com/smithy-lang/smithy/pull/1857), [#1884](https://github.com/smithy-lang/smithy/pull/1884))
* Enabled default mode for `smithy diff` rather than failing when not set ([#1856](https://github.com/smithy-lang/smithy/pull/1856))
* Added warning to mis-cased standard HTTP verbs ([#1862](https://github.com/smithy-lang/smithy/pull/1862))
* Relaxed type constraints for `pageSize` property of the `@paginated` trait ([#1866](https://github.com/smithy-lang/smithy/pull/1866))
* Improved message for invalid `.errors` entries ([#1867](https://github.com/smithy-lang/smithy/pull/1867))
* Added `docId` property to `aws.api#service` trait ([#1863](https://github.com/smithy-lang/smithy/pull/1863), [#1872](https://github.com/smithy-lang/smithy/pull/1872), [#1881](https://github.com/smithy-lang/smithy/pull/1881), [#1882](https://github.com/smithy-lang/smithy/pull/1882))
* Improved validation for http binding protocols ([#1873](https://github.com/smithy-lang/smithy/pull/1873))
* Expanded valid targets of `@httpPayload` ([#1876](https://github.com/smithy-lang/smithy/pull/1876))

### Documentation
* Updated documentation around `timestamp` and added more specificity to the definition ([#1858](https://github.com/smithy-lang/smithy/pull/1858)) 

### Bug Fixes
* Removed unrecognized models from sources ([#1851](https://github.com/smithy-lang/smithy/pull/1851), [#1860](https://github.com/smithy-lang/smithy/pull/1860))
* Updated the content type of list & map shapes with the `@httpPayload` trait to document content type ([#1840](https://github.com/smithy-lang/smithy/pull/1840))
* Fixed IDL serializer which would write emtpy `apply` statements to mixed in members of `enums` ([#1865](https://github.com/smithy-lang/smithy/pull/1865))
* Fixed indentation when formatting text blocks ([#1875](https://github.com/smithy-lang/smithy/pull/1875))
* Added resource files to source jars ([#1877](https://github.com/smithy-lang/smithy/pull/1877), [#1880](https://github.com/smithy-lang/smithy/pull/1880))
* Fixed a potential resource leak by using a try with resources ([#1878](https://github.com/smithy-lang/smithy/pull/1878))

### Other

* Migrated to using Gradle 8.2.1 to build Smithy. This should have no impactful downstream effects ([#1849](https://github.com/smithy-lang/smithy/pull/1849))
* Moved repository into `smithy-lang` organization and updated resources accordingly ([#1852](https://github.com/smithy-lang/smithy/pull/1852), [#1854](https://github.com/smithy-lang/smithy/pull/1854))

## 1.34.0 (2023-07-10)

### Features

* Added a default template for the `smithy init` command, making specifying templates optional ([#1843](https://github.com/awslabs/smithy/pull/1843))
* Updated the model loader to skip unrecognized non-Smithy JSON files ([#1846](https://github.com/awslabs/smithy/pull/1846))

### Bug Fixes

* Fixed basic HTTP authentication when resolving dependencies in the Smithy CLI ([#1838](https://github.com/awslabs/smithy/pull/1838))
* Fixed a bug when deduping `ChangedNullability` events ([#1839](https://github.com/awslabs/smithy/pull/1839))

### Documentation

* Replaced implementation docs with the awesome-smithy repository ([#1845](https://github.com/awslabs/smithy/pull/1845))
* Removed support for fractional seconds from the `http-date` timestamp format ([#1847](https://github.com/awslabs/smithy/pull/1847))
* Rephrased optional fractional precision and no UTC offset support for the `date-time` timestamp format ([#1835](https://github.com/awslabs/smithy/pull/1835))

## 1.33.0 (2023-06-21)

### Features
* Extended event ids for `AddedOperationError`, `RemovedOperationError`, `AddedEntityBinding` and `RemovedEntityBinding` diff events ([#1797](https://github.com/awslabs/smithy/pull/1797), [#1803](https://github.com/awslabs/smithy/pull/1803))
* Added enum values to ids for `ChangedEnumTrait` diff events ([#1807](https://github.com/awslabs/smithy/pull/1807))
* Added `init` command to Smithy CLI ([#1802](https://github.com/awslabs/smithy/pull/1802), [#1825](https://github.com/awslabs/smithy/pull/1825), [#1832](https://github.com/awslabs/smithy/pull/1832))
* Added `smithy-syntax` package and `smithy format` command to Smithy CLI ([#1830](https://github.com/awslabs/smithy/pull/1830))

### Bug Fixes
* Fixed duplicated events for `ChangedNullability` alongside the `AddedInputTrait / RemovedInputTrait` ([#1806](https://github.com/awslabs/smithy/pull/1806))
* Updated request compression trait protocol tests with regard to HTTP bindings and respective specification ([#1831](https://github.com/awslabs/smithy/pull/1831))

### Documentation
* Added `smithy-dafny` to code generators table ([#1813](https://github.com/awslabs/smithy/pull/1813))
* Updated stale docs around `MissingPaginatedTrait` ([#1814](https://github.com/awslabs/smithy/pull/1814))
* Fixed grammar rendering ([#1815](https://github.com/awslabs/smithy/pull/1815))
* Updated recommendation for HTTP status code ([#1818](https://github.com/awslabs/smithy/pull/1818))
* Fixed selector example ([#1824](https://github.com/awslabs/smithy/pull/1824))
* Added note about how constraint traits affect backward compatibility ([#1826](https://github.com/awslabs/smithy/pull/1826))
* Added guide on disabling authentication ([#1791](https://github.com/awslabs/smithy/pull/1791))

## 1.32.0 (2023-06-06)

### Features
* Refactor parsing and validation of `list` and `map` shapes. This improved validation output when unexpected members were present in these shapes ([#1782](https://github.com/awslabs/smithy/pull/1782)) 
* Updated smithy-build to output projection failures only after all plugins finish running (failed or otherwise) ([#1762](https://github.com/awslabs/smithy/pull/1762))
* Added new pluggable validation-event decorator capability. This allows for customizing of validation events through a service provider interface ([#1774](https://github.com/awslabs/smithy/pull/1774))
* Added new diff-evaluator to emit events for when the `@required` trait is added to existing structures without a default ([##1781](https://github.com/awslabs/smithy/pull/1781))
* Improved validation output for `@default` collisions ([#1780](https://github.com/awslabs/smithy/pull/1780))
* Updated `@httpQuery` trait validation to prevent query-literal and query-param conflicts ([#1786](https://github.com/awslabs/smithy/pull/1786))
* Updated default pagination flags to improve missing-pagination validation ([#1764](https://github.com/awslabs/smithy/pull/1764))
* Updated `SdkServiceIdValidator` to emit `DANGER` events instead of `ERROR` events ([#1772](https://github.com/awslabs/smithy/pull/1772))
* Updated `ChangedEnumTrait` evaluator to include specific ids, in order to differentiate specific events ([#1787](https://github.com/awslabs/smithy/pull/1787))
* Added protocol tests to validate http-label escaping in the `restXml` protocol ([#1759](https://github.com/awslabs/smithy/pull/1759))
* Added support for `@externalDocs` trait when converting operations in the OpenAPI converter ([#1767](https://github.com/awslabs/smithy/pull/1767))
* Updated a handful of specifications in the smithy-grammar to improve parsability ([#1788](https://github.com/awslabs/smithy/pull/1788), [#1790](https://github.com/awslabs/smithy/pull/1790), [#1792](https://github.com/awslabs/smithy/pull/1792), [#1793](https://github.com/awslabs/smithy/pull/1793), [#1800](https://github.com/awslabs/smithy/pull/1800))

### Bug Fixes
* Fixed `migrate` CLI command to properly upgrade 1/1.0 models to 2/2.0 ([#1579](https://github.com/awslabs/smithy/pull/1579), [#1769](https://github.com/awslabs/smithy/pull/1769))
* Fixed application of enum-mixins on empty enums ([#1794](https://github.com/awslabs/smithy/pull/1794))
* Fixed handling of dangling doc-comments in structures ([#1776](https://github.com/awslabs/smithy/pull/1776))
* Fixed several smithy-grammar typos and consistency issues ([#1783](https://github.com/awslabs/smithy/pull/1783))

### Documentation
* Added a warning about the limitations of request-validation in API-Gateway ([#1765](https://github.com/awslabs/smithy/pull/1765))
* Updated CLI installation guide for Windows to be more idiomatic ([#1757](https://github.com/awslabs/smithy/pull/1757))
* Updated protocol documentation pages to indicate support for the `@requestCompression` trait ([#1763](https://github.com/awslabs/smithy/pull/1763))

## 1.31.0 (2023-04-25)

### Features
* Added `@requestCompression` trait which indicates whether an operation supports compressed requests ([#1748](https://github.com/awslabs/smithy/pull/1748))
* Improved IDL parser and added basic error recovery ([#1733](https://github.com/awslabs/smithy/pull/1733))
* Added restJson1 protocol test for a list of structures missing a required key ([#1735](https://github.com/awslabs/smithy/pull/1735))
* Added ability to order the output of the IDL serializer ([#1727](https://github.com/awslabs/smithy/pull/1727))
 
### Bug Fixes
* Updated conversion from string shape with `@enum` trait to enum shape to convert `internal` tag to `@internal` trait ([#1739](https://github.com/awslabs/smithy/pull/1739))

### Documentation
* Added documentation for changeStringEnumsToEnumShapes transformation ([#1740](https://github.com/awslabs/smithy/pull/1740))

## 1.30.0 (2023-04-10)

### Features
* Updated smithy-diff and smithy-build to use pretty validation output and color theming options ([#1712](https://github.com/awslabs/smithy/pull/1712))
* Added --mode flag to smithy diff command with support for `aribtrary`, `project`, and `git` modes 
  ([#1724](https://github.com/awslabs/smithy/pull/1724), [#1721](https://github.com/awslabs/smithy/pull/1721), [#1718](https://github.com/awslabs/smithy/pull/1718)) 
* Added --flatten flag to AST command which flattens and removes mixins from the model ([#1723](https://github.com/awslabs/smithy/pull/1723))
* Expose functions to make ruleEvaluator more flexible to support coverage checking ([#1681](https://github.com/awslabs/smithy/pull/1681))

### Bug Fixes
* Updated mixins to allow multiple mixins to override the same member if they all target the same shape ([#1715](https://github.com/awslabs/smithy/pull/1715))
* Fixed an issue where source file names impacted the ordering of metadata ([#1716](https://github.com/awslabs/smithy/pull/1716))
* Fixed error messages for invalid operation input/output bindings ([#1728](https://github.com/awslabs/smithy/pull/1728))
* Fixed bugs in smithy-rules-engine boolEquals and stringEquals which could cause unexpected results when visitors are 
  invoked ([#1681](https://github.com/awslabs/smithy/pull/1681))
* Remove unnecessary member from `aws.iam#actionName` ([#1726](https://github.com/awslabs/smithy/pull/1726))

### Documentation
* Added guide on how to install the Smithy CLI ([#1697](https://github.com/awslabs/smithy/pull/1697))
* Added examples of how smithy validators can be used to prevent common bugs and enforce common style ([#1702](https://github.com/awslabs/smithy/pull/1702))
* Added clarification on meaning and use of `@httpApiKeyAuth` `scheme` property ([#1714](https://github.com/awslabs/smithy/pull/1714))
* Reduced IDL ambiguity by replacing *SP with [SP] ([#1711](https://github.com/awslabs/smithy/pull/1711))

## 1.29.0 (2023-04-03)

### Features
* Added EnumTrait validation protocol test ([#1679](https://github.com/awslabs/smithy/pull/1679))
* Added process based plugins to Smithy build ([#1672](https://github.com/awslabs/smithy/pull/1672))
* Added GenerateOperationDirective to generate operation shapes separate from resources and services ([#1676](https://github.com/awslabs/smithy/pull/1679))
* Added :root and :in selectors ([#1690](https://github.com/awslabs/smithy/pull/1690))
* Added --show-traits to select command ([#1692](https://github.com/awslabs/smithy/pull/1692))
* Added includePreludeShapes in model plugin ([#1693](https://github.com/awslabs/smithy/pull/1693))
* Added aws.iam#actionName trait to override using the API operation name ([#1679](https://github.com/awslabs/smithy/pull/1665))
* Improved resource property validation and error messages ([#1694](https://github.com/awslabs/smithy/pull/1694))
* Improved CLI outputs for validation commands ([#1695](https://github.com/awslabs/smithy/pull/1695))
* Optimized identity and neighbor selectors ([#1691](https://github.com/awslabs/smithy/pull/1691))
* Refactored CLI to remove --severity from some commands ([#1700](https://github.com/awslabs/smithy/pull/1700))
* Removed unused positional [<MODEL>] from diff command ([#1703](https://github.com/awslabs/smithy/pull/1703))

### Bug Fixes
* Ensured that the ValidationEvent listener gets all events when batch inclusions are used ([#1698](https://github.com/awslabs/smithy/pull/1698))
* Fixed cp-R for linux, xcopy for windows in smithy-cli installers ([#1686](https://github.com/awslabs/smithy/pull/1686))
* Fixed allowUnknownTraits for projection with import ([#1685](https://github.com/awslabs/smithy/pull/1685))
* Fixed reversed parameters in diff message for RemovedOperationError ([#1689](https://github.com/awslabs/smithy/pull/1689))
* Fixed hierarchical event ids lost when specifying a custom linter validator id or severity level ([#1705](https://github.com/awslabs/smithy/pull/1705))
* Improved handling additionalSchema targeting an invalid shape ([#1708](https://github.com/awslabs/smithy/pull/1708))
* Reduced IDL ambiguity by replacing *WS with [WS] ([#1699](https://github.com/awslabs/smithy/pull/1699))

### Documentation
* Added compatibility note to evolving models ([#1669](https://github.com/awslabs/smithy/pull/1669))
* Fixed mixins usage examples in style guide ([#1670](https://github.com/awslabs/smithy/pull/1670))
* Fixed type in primitive root-level example ([#1687](https://github.com/awslabs/smithy/pull/1687))
* Removed recommendation to implement presence tracking when handling default values ([#1682](https://github.com/awslabs/smithy/pull/1682))
* Removed OperationBody indefinite repetition in IDL ([#1707](https://github.com/awslabs/smithy/pull/1707))

## 1.28.1 (2023-03-09)

### Features
* Added a suite of compliance tests for selectors ([#1643](https://github.com/awslabs/smithy/pull/1643))

### Bug Fixes
* Fixed an issue with generating CloudFormation Resource Schemas when using the `@nestedProperties` trait ([#1641](https://github.com/awslabs/smithy/pull/1641))
* Fixed an issue where `enum` shapes could not be used as `resource` identifiers ([#1644](https://github.com/awslabs/smithy/pull/1644))
* Fixed an issue when comparing event ids for deprecated shapes ([#1640](https://github.com/awslabs/smithy/pull/1640))
* Fixed an issue where "core" validation events were not suppressible ([#1646](https://github.com/awslabs/smithy/pull/1646))
* Fixed an issue with `NodeMapper`'s handling of lists of generic types ([#1635](https://github.com/awslabs/smithy/pull/1635))
* Fixed various typos of the word "ignore", including for the `NodeMapper.WhenMissing` enum ([#1652](https://github.com/awslabs/smithy/pull/1652))
* Fixed an issue where `enum` members were flagged by the `MissingSensitiveTrait` validator ([#1661](https://github.com/awslabs/smithy/pull/1661))
* Updated the validation messages for `uniqueItems` malformed request tests ([#1639](https://github.com/awslabs/smithy/pull/1639))
* Updated the validation messages for `enum` malformed request tests to not return internal values ([#1658](https://github.com/awslabs/smithy/pull/1658))
* Fixed various issues with protocol tests ([#1642](https://github.com/awslabs/smithy/pull/1642), [#1648](https://github.com/awslabs/smithy/pull/1648),
  [#1645](https://github.com/awslabs/smithy/pull/1645))

## 1.28.0 (2023-02-24)

### Features
* Add client-only protocol tests for fractional second parsing ([#1627](https://github.com/awslabs/smithy/pull/1627))
* Add protocol test for omitting empty http-query lists ([#1629](https://github.com/awslabs/smithy/pull/1629))
* Add support for JSON Schema draft2020-12 ([#1617](https://github.com/awslabs/smithy/pull/1617))
* Add hierarchical eventIds ([#1527](https://github.com/awslabs/smithy/pull/1527), [#1631](https://github.com/awslabs/smithy/pull/1631))
* Preserve tag order in generated OpenAPI specification ([#1604](https://github.com/awslabs/smithy/pull/1604))
* Add shapes generation order in CodegenDirector ([#1615](https://github.com/awslabs/smithy/pull/1615))

### Bug Fixes
* Remove reflected input values from validation protocol tests ([#1622](https://github.com/awslabs/smithy/pull/1622))
* Fail ExamplesTraitValidator when both output and error are defined ([#1599](https://github.com/awslabs/smithy/pull/1599))
* Fix mixin cycles being incorrectly detected ([#1628](https://github.com/awslabs/smithy/pull/1628))
* Fix warnings in AST Loader for Resource and Operation Shapes with mixins ([#1626](https://github.com/awslabs/smithy/pull/1626))
* Fix referenced components removed in openapi schema ([#1595](https://github.com/awslabs/smithy/pull/1595))
* Fix OR condition in scoped attribute selector ([#1618](https://github.com/awslabs/smithy/pull/1618))
* Fix passthroughBehavior casing on x-amzn-apigateway-integration ([#1619](https://github.com/awslabs/smithy/pull/1619))

### Documentation
* Clarify rules for escaping shapes bound to URIs ([#1630](https://github.com/awslabs/smithy/pull/1630))
* Document Tree Sitter implementation ([#1621](https://github.com/awslabs/smithy/pull/1621))
* Clarify handling of date-time offsets ([#1597](https://github.com/awslabs/smithy/pull/1597))
* Add Smithy code generation guide ([#1586](https://github.com/awslabs/smithy/pull/1586), [#1592](https://github.com/awslabs/smithy/pull/1592))

## 1.27.2 (2023-01-30)

### Features
* Implement Comparable interface for TagObject and ExternalDocumentation ([#1589](https://github.com/awslabs/smithy/pull/1589))
* Relax rule engine validation to support test auth schemes ([#1590](https://github.com/awslabs/smithy/pull/1590))
* Ensure that AuthSchemes added to Endpoint builder retain parameter ordering ([#1591](https://github.com/awslabs/smithy/pull/1591))
* Add intEnum coverage on map of string list ([#1596](https://github.com/awslabs/smithy/pull/1596))

### Bug Fixes
* Add source location to synthetic Enum trait ([#1580](https://github.com/awslabs/smithy/pull/1580))

### Documentation
* Minor fix to restJson1 docs ([#1587](https://github.com/awslabs/smithy/pull/1587))

## 1.27.1 (2023-01-11)

### Features
* Update protocol tests with datetime offset coverage ([#1502](https://github.com/awslabs/smithy/pull/1502))
* Add protocol tests to cover @range for short, long and integer shapes ([#1515](https://github.com/awslabs/smithy/pull/1515))
* Add exclude/include tranforms using selectors ([#1534](https://github.com/awslabs/smithy/pull/1534))
* Add a parseArn test case for resources with `:` and `/` ([#1537](https://github.com/awslabs/smithy/pull/1537))
* Move CDS warmup to the CLI directly ([#1553](https://github.com/awslabs/smithy/pull/1553))
* Allow error rename and disallow error rename for all AWS protocols ([#1554](https://github.com/awslabs/smithy/pull/1554))
* Add details to ModifiedTrait event id ([#1560](https://github.com/awslabs/smithy/pull/1560))

### Bug Fixes
* Fix deterministic order of properties ([#1555](https://github.com/awslabs/smithy/pull/1555))
* Fix datetime offset restXml payload ([#1559](https://github.com/awslabs/smithy/pull/1559))
* Fix `RestJsonQueryStringEscaping` protocol test ([#1562](https://github.com/awslabs/smithy/pull/1562))
* Fix `RestJsonAllQueryStringTypes` protocol test ([#1564](https://github.com/awslabs/smithy/pull/1564))
* Fix Upgrade1to2Command for Set shape ([#1569](https://github.com/awslabs/smithy/pull/1569))
* Fix parameters to builder ([#1571](https://github.com/awslabs/smithy/pull/1571))

### Documentation
* Fix typo for NOTE under breaking change rules ([#1552](https://github.com/awslabs/smithy/pull/1552))

## 1.27.0 (2022-12-15)

### Features

* Add tests for ACCEPT * ([#1365](https://github.com/awslabs/smithy/pull/1365))
* Test content-type modeled inputs without body ([#1399](https://github.com/awslabs/smithy/pull/1399))
* Improve member not targeting a property error message to better hint at fix ([#1501](https://github.com/awslabs/smithy/pull/1501))
* Add typechecking to EndpointRuleset build ([#1507](https://github.com/awslabs/smithy/pull/1507))
* Add warnings for private access on traits ([#1508](https://github.com/awslabs/smithy/pull/1508))
* Tweak class caching in node (de)serializers. ([#1518](https://github.com/awslabs/smithy/pull/1518), [#1530](https://github.com/awslabs/smithy/pull/1530))
* Add Maven dependency resolution to the CLI ([#1526](https://github.com/awslabs/smithy/pull/1526))
* Add details to TraitBreakingChange EventId ([#1538](https://github.com/awslabs/smithy/pull/1538))

### Bug Fixes

* Fix dedicated io transform leaving unused shapes ([#1419](https://github.com/awslabs/smithy/pull/1419))
* Fix return type of substring method ([#1504](https://github.com/awslabs/smithy/pull/1504))
* Fix quoted text grammar and parsing ([#1535](https://github.com/awslabs/smithy/pull/1535))
* Fix backwards compatibility rules for the paginated trait ([#1549](https://github.com/awslabs/smithy/pull/1549))

### Documentation

* Make it clearer that assembly-name stripping is not a MUST in `restJson1` ([#1493](https://github.com/awslabs/smithy/pull/1493))
* Clarify service-level pagination configuration ([#1514](https://github.com/awslabs/smithy/pull/1514))
* Document it is generally breaking to add/remove input trait ([#1519](https://github.com/awslabs/smithy/pull/1519))
* Fix grammar for MapMembers ([#1520](https://github.com/awslabs/smithy/pull/1520))
* Clarify that metadata has no namespace ([#1521](https://github.com/awslabs/smithy/pull/1521))
* Update trailing line break, list member grammar ([#1533](https://github.com/awslabs/smithy/pull/1533))
* Fix MapMembers grammar and update test ([#1536](https://github.com/awslabs/smithy/pull/1536))

## 1.26.4 (2022-11-22)

### Bug Fixes

* Fixed updating mixins when replacing shapes in transforms ([1509](https://github.com/awslabs/smithy/pull/1509))

## 1.26.3 (2022-11-17)

### Features

* Moved useIntegerType to jsonschema ([1495](https://github.com/awslabs/smithy/pull/1495))
* Added intEnum protocol tests ([1492](https://github.com/awslabs/smithy/pull/1492))
* Added timestampFormat protocol tests on target shapes ([1440](https://github.com/awslabs/smithy/pull/1440))
* Added MissingSensitiveTraitValidator ([1364](https://github.com/awslabs/smithy/pull/1364))

### Bug Fixes

* Fixed applying protocol tests to correct operations ([1477](https://github.com/awslabs/smithy/pull/1477))
* Fixed cfn-mutability for inherited identifiers ([1465](https://github.com/awslabs/smithy/pull/1465))
* Fixed Resource shape properties Type entry ([1415](https://github.com/awslabs/smithy/pull/1415))

### Documentation

* Updated links to point to smithy.io ([1497](https://github.com/awslabs/smithy/pull/1497))
* Fixed docs and fail on additional doc warnings ([1496](https://github.com/awslabs/smithy/pull/1496))
* Fixed AbstractCodeWriter documentation ([1490](https://github.com/awslabs/smithy/pull/1490))

## 1.26.2 (2022-11-07)

### Bug Fixes

* Add missing regions and fix typo in partitions.json ([#1487](https://github.com/awslabs/smithy/pull/1487))

## 1.26.1 (2022-10-31)

### Features

* Added support for hierarchical event IDs in validation events, allowing for more granular suppression ([#1466](https://github.com/awslabs/smithy/pull/1466))
* Removed the pattern from the `@suppress` trait's entry list, allowing them to match all validator IDs ([#1455](https://github.com/awslabs/smithy/pull/1455))
* Added the ability to lint based on word boundaries for the `ReservedWords` validator ([#1461](https://github.com/awslabs/smithy/pull/1461))
* Added a `toNode` method to the `Partition` class in `smithy-rules-engine` ([#1449](https://github.com/awslabs/smithy/pull/1449))
* Added a warning when `smithy-diff` detects changes to traits that do not have definitions loaded ([#1468](https://github.com/awslabs/smithy/pull/1468))
* Improved validation for members that target nullable shapes ([#1454](https://github.com/awslabs/smithy/pull/1454), [1460](https://github.com/awslabs/smithy/pull/1460))
* Added a hook to the `CodegenDirector` to allow for customization before shape generation ([#1469](https://github.com/awslabs/smithy/pull/1469))
* Updated model assembling to always attempt model interop transforms ([#1435](https://github.com/awslabs/smithy/pull/1435))

### Bug Fixes

* Fixed a bug where transforms would not remove enum members ([#1442](https://github.com/awslabs/smithy/pull/1442), [#1447](https://github.com/awslabs/smithy/pull/1447))
* Fixed a bug where documentation comments were dropped if they occurred after a member using the default value syntactic
  sugar ([1459](https://github.com/awslabs/smithy/pull/1459))
* Fixed an issue where resource identifier collisions would cause a model to fail loading ([#1453](https://github.com/awslabs/smithy/pull/1453),
  [#1474](https://github.com/awslabs/smithy/pull/1474))
* Added `@private` to the local traits for the AWS `HttpConfiguration` shape ([#1445](https://github.com/awslabs/smithy/pull/1445))
* Fixed an issue with behavior defined in an `awsQuery` protocol test ([#1444](https://github.com/awslabs/smithy/pull/1444))
* Fixed several protocol tests in the `awsJson1_1` protocol test suite ([#1392](https://github.com/awslabs/smithy/pull/1392))
* Fixed an incorrect application of the `@httpMalformedRequestTests` trait ([#1467](https://github.com/awslabs/smithy/pull/1467))

### Documentation

* Clarified streaming trait values and semantics ([#1458](https://github.com/awslabs/smithy/pull/1458))
* Updated the identifier ABNF and parser ([#1464](https://github.com/awslabs/smithy/pull/1464))

## 1.26.0 (2022-10-10)

### Features

* Add support for missing authorizer members ([#1426](https://github.com/awslabs/smithy/pull/1426))
* Add intEnum DirectedCodegen ([#1434](https://github.com/awslabs/smithy/pull/1434))
* Add Smithy Rules Engine (unstable) ([#1356](https://github.com/awslabs/smithy/pull/1356))

### Documentation

* Fix intEnum example ([#1432](https://github.com/awslabs/smithy/pull/1432))
* Fix javadoc for CodegenDirector.simplifyModelForServiceCodegen ([#1433](https://github.com/awslabs/smithy/pull/1433))

## 1.25.2 (2022-09-28)

### Bug Fixes
* Revert "Enforce private on traits (#1406)" ([#1428](https://github.com/awslabs/smithy/pull/1428))
* Remove aws query compatible protocol test ([#1424](https://github.com/awslabs/smithy/pull/1424))

## 1.25.1 (2022-09-23)

### Features
* Warn when box trait found on union member ([#1420](https://github.com/awslabs/smithy/pull/1420))
* Warn when default used with union member target ([#1418](https://github.com/awslabs/smithy/pull/1418))
* Simplify ShapeId caching ([#1411](https://github.com/awslabs/smithy/pull/1411))
* Update smithy-diff for strings with the enum trait to enum shapes ([#1409](https://github.com/awslabs/smithy/pull/1409))
* Add support for 1.0 downgrades and serialization ([#1403](https://github.com/awslabs/smithy/pull/1403) and [#1410](https://github.com/awslabs/smithy/pull/1410))
* Add AwsQueryCompatible trait ([#1314](https://github.com/awslabs/smithy/pull/1314))

### Bug Fixes
* Only emit deprecation of enum trait in 2.0 models ([#1421](https://github.com/awslabs/smithy/pull/1421))
* Enforce private on traits ([#1406](https://github.com/awslabs/smithy/pull/1406))
* Fix apply statement parsing and ABNF ([#1414](https://github.com/awslabs/smithy/pull/1414))
* Add test for synthetic box traits on mixins ([#1404](https://github.com/awslabs/smithy/pull/1404))

### Documentation
* Add some clarifications to revised default value design doc ([#1413](https://github.com/awslabs/smithy/pull/1413))
* Revise default value design doc to match recent updates ([#1412](https://github.com/awslabs/smithy/pull/1412))
* Fix typo in migration guide ([#1405](https://github.com/awslabs/smithy/pull/1405))

## 1.25.0 (2022-09-13)

### Features

* Made many improvements for Smithy 1.0 and 2.0 interoperability. ([1394](https://github.com/awslabs/smithy/pull/1394))
* Default traits can now coexist with required trais. This indicates that a member should be serialized, but it is a
  protocol-specific decision if and how this is enforced. This was a pattern that occurred in Smithy 1.0 models when
  a member was required and targeted a shape with a zero value.
* Default traits can be added to root-level shapes. Any structure member that targets a shape marked with the default
  trait must repeat the default on the member. This removes the action at a distance problem observed in Smithy IDL 1.0
  where a root level shape implicitly introduced a default zero value, and to know if that's the case for any member,
  you had to look through from the member to the target shape. This change allows us to know if a root level shape was
  boxed in IDL 1.0 too (root shapes with no default or a default set to anything other than the zero value was boxed).
* Added the `@addedDefault` trait which is used to indicate that a `@default` trait was added to a member after it
  was initially released. This can be used by tooling to make an appropriate determination if generating a
  non-nullable type for the member is a backward compatible change. For example, if a generator only uses default
  zero values to generate non-nullable types, then the removal of the required trait and addition of a default trait
  would be a breaking change for them, so they can use addedDefault to ignore the default trait.
* Add new NullableIndex modes for testing if a member is nullable based on the supported features of the
  generator. For example, some generators only make members non-optional when the member is set to the zero value
  of a type, so there is a NullableIndex check mode for that and other use cases.
* When loading IDL 2.0 models, we will now patch synthetic box traits onto shapes that would have been considered
  boxed in Smithy IDL 1.0. This improves further interop with tooling that has not yet adopted Smithy IDL 2 or that
  hasn't yet migrated to use the NullableIndex abstraction.
* When loading 1.0 models, rather than dropping the default trait from a member when the range trait of a shape is
  invalid for its zero value, we now instead emit only a warning for this specific case. This prevents changing the
  type and also doesn't lose the range constraint.
* The Primitive* shapes in the prelude are no longer deprecated, and they now have a `@default` trait on them set to
  the zero value of the type. This makes these traits function exactly as they did in Smithy 1.0 models. Any member
  that targets one of these primitive prelude shapes must now also repeat the zero value of the target shape.
* Added an optional nullability report to smithy-build that shows the computed nullability semantics of each member in
  a model. This can be used to better understand nullability semantics.
* Added method to NumberNode to detect if it is set to zero. ([#1385](https://github.com/awslabs/smithy/pull/1385))
* In ChangeShapeType transform, ignored types changes to same type. ([#1397](https://github.com/awslabs/smithy/pull/1397))

### Bug fixes

* Updated smithy-diff to not emit events when diffing a 1.0 model against the 2.0 serialized version of the model.
  This means that changes to the box trait are ignored unless the change impacts the nullability of the shape.
  Special handling was added to detect breaking changes with the default trait too (you can't change a default
  value of a root-level shape for example, you cannot change a default value of a shape to or from the zero value
  of a type as this might break code generators, etc). ([1394](https://github.com/awslabs/smithy/pull/1394))
* smithy-diff is no longer reporting expected `set` shape to `list` shape transitions. Sets are deprecated and
  models are encouraged to migrate from sets to lists with the `@uniqueItems` trait. ([1383](https://github.com/awslabs/smithy/pull/1383))

### Documentation

* Fix operationOutputSuffix in example code snippet ([#1393](https://github.com/awslabs/smithy/pull/1393))
* Fix ABNF grammar of inlined structure ([1377](https://github.com/awslabs/smithy/pull/1377))

## 1.24.0 (2022-08-30)

### Features

* Made string enum to enum shape transform opt-in in CodegenDirector. ([#1370](https://github.com/awslabs/smithy/pull/1370))
* Updated `@httpResponseCode` to not be applicable to `@input` structures. ([#1359](https://github.com/awslabs/smithy/pull/1359))
* Made some improvements in smithy-build. ([#1366](https://github.com/awslabs/smithy/pull/1366))

### Bug fixes

* Filtered out synthetic traits from build info. ([#1374](https://github.com/awslabs/smithy/pull/1374))
* Fixed a log message when unable to convert string enum to enum shape. ([#1372](https://github.com/awslabs/smithy/pull/1372))

### Documentation

* Updated Smithy IDL ABNF Docs. ([#1357](https://github.com/awslabs/smithy/pull/1357))

## 1.23.1 (2022-08-18)

### Features

* Added new methods to help deserializing object nodes
  ([#1350](https://github.com/awslabs/smithy/pull/1350))
* Added several (unstable) traits for endpoint resolution in the new
  `smithy-rules-engine` package
  ([#1248](https://github.com/awslabs/smithy/pull/1248))

### Bug fixes

* Fixed an issue where validation events were emitted twice ([#1362](https://github.com/awslabs/smithy/pull/1362))
* Fixed a bug that was causing errors loading 1.0 models with `@enum` traits ([#1358](https://github.com/awslabs/smithy/pull/1358))
* Fixed `PostUnionWithJsonName` and `MalformedAcceptWithGenericString` protocol test. ([#1361](https://github.com/awslabs/smithy/pull/1361),  [#1360](https://github.com/awslabs/smithy/pull/1360))
* Added missing readonly traits on HTTP GET tests ([#1354](https://github.com/awslabs/smithy/pull/1354))

### Documentation

* Fixed several documentation issues
  ([#1355](https://github.com/awslabs/smithy/pull/1355),
  [#1353](https://github.com/awslabs/smithy/pull/1353),
  [#1349](https://github.com/awslabs/smithy/pull/1349),
  [#1347](https://github.com/awslabs/smithy/pull/1347),
  [#1346](https://github.com/awslabs/smithy/pull/1346),
  [#1345](https://github.com/awslabs/smithy/pull/1345))


## 1.23.0 (2022-08-10)

### Features

* Added version 2.0 of the Smithy IDL. ([#1317](https://github.com/awslabs/smithy/pull/1317),
  [#1312](https://github.com/awslabs/smithy/pull/1312), [#1318](https://github.com/awslabs/smithy/pull/1318))
* Added mixins for all shape types. ([#889](https://github.com/awslabs/smithy/pull/889), [#1025](https://github.com/awslabs/smithy/pull/1025),
  [#1139](https://github.com/awslabs/smithy/pull/1139), [#1323](https://github.com/awslabs/smithy/pull/1323))
* Added resource properties to version 2.0 of Smithy IDL. ([#1213](https://github.com/awslabs/smithy/pull/1213))
* Added target elision for mixins/resources. ([#1231](https://github.com/awslabs/smithy/pull/1231))
* Added inline operation IO shapes. ([#963](https://github.com/awslabs/smithy/pull/963), [#962](https://github.com/awslabs/smithy/pull/962),
  [#1007](https://github.com/awslabs/smithy/pull/1007))
* Added validation for multiple IDL versions. ([#917](https://github.com/awslabs/smithy/pull/917))
* Added IDL 1.0 to 2.0 model migration tool. ([#1175](https://github.com/awslabs/smithy/pull/1175))
* Added enum shapes. ([#1088](https://github.com/awslabs/smithy/pull/1088), [#1114](https://github.com/awslabs/smithy/pull/1114),
  [#1133](https://github.com/awslabs/smithy/pull/1133), [#1313](https://github.com/awslabs/smithy/pull/1313))
* Added `@clientOptional` trait. ([#1052](https://github.com/awslabs/smithy/pull/1052), [#1264](https://github.com/awslabs/smithy/pull/1264))
* Multiple traits can now be applied in a single `apply` statement. ([#885](https://github.com/awslabs/smithy/pull/885))
* Commas are now optional in the IDL. ([#772](https://github.com/awslabs/smithy/pull/772), [#776](https://github.com/awslabs/smithy/pull/776),
  [#1166](https://github.com/awslabs/smithy/pull/1166))
* Added `@default` trait. ([#1019](https://github.com/awslabs/smithy/pull/1019), [#1286](https://github.com/awslabs/smithy/pull/1286),
  [#1021](https://github.com/awslabs/smithy/pull/1021), [#1048](https://github.com/awslabs/smithy/pull/1048), [#920](https://github.com/awslabs/smithy/pull/920))
* Sets can no longer be used in 2.0 models. Use a list with the `@uniqueItems` trait instead. Sets can still be used in 1.0 models, though a
  warning will be emitted. The `SetShape` in smithy-model is a subclass of `ListShape`, so new code generators can simply treat any `SetShape`
  like a `ListShape`. ([#1292](https://github.com/awslabs/smithy/pull/1292))
* Migrated traits packages to use Smithy IDL definitions instead of JSON AST. ([#1207](https://github.com/awslabs/smithy/pull/1207))
* Added `@cfnDefaultValue` trait ([#1285](https://github.com/awslabs/smithy/pull/1285))

### Bug fixes

* Made streaming blobs required/default. ([#1209](https://github.com/awslabs/smithy/pull/1209))
* Fixed TextIndex to handle synthetic traits. ([#1206](https://github.com/awslabs/smithy/pull/1206))
* Removed duplicate aws.protocols model file. ([#1310](https://github.com/awslabs/smithy/pull/1310))
* Removed empty trait block serialization. ([#1240](https://github.com/awslabs/smithy/pull/1240))
* Fixed trait source locations. ([#1146](https://github.com/awslabs/smithy/pull/1146), [#1157](https://github.com/awslabs/smithy/pull/1157))

### Documentation

* Added documentation for IDL 2.0 and changed location of 1.0 docs. ([#1302](https://github.com/awslabs/smithy/pull/1302),
  [#1057](https://github.com/awslabs/smithy/pull/1057))
* Added IDL 1.0 to 2.0 migration guide. ([#1065](https://github.com/awslabs/smithy/pull/1065), [#1074](https://github.com/awslabs/smithy/pull/1074))
* Updated the doc highlighter for IDL 2.0. ([#1251](https://github.com/awslabs/smithy/pull/1251))
* Added documentation to IAM trait enums. ([#1322](https://github.com/awslabs/smithy/pull/1322))

## 1.22.0 (2022-06-30)

### Breaking changes

* Disallowed `@sensitive` trait on members. It must be applied the shape targeted by members. ([#1226](https://github.com/awslabs/smithy/pull/1226))
* Deprecated `set` in favor of `@uniqueItems`. `@uniqueItems` can no longer target `float`, `double` and `document`. ([#1278](https://github.com/awslabs/smithy/pull/1278))

### Features

* Added `breakingChanges` property to `@trait` to specify more complex backward compatibility rules. ([#1193](https://github.com/awslabs/smithy/pull/1193))
* Added automatic casing detection to CamelCaseValidator. ([#1217](https://github.com/awslabs/smithy/pull/1217))
* Added `--quiet` flag to all CLI commands. ([#1257](https://github.com/awslabs/smithy/pull/1257))
* Added CodeWriter support to pull named parameters from CodeSections. ([#1256](https://github.com/awslabs/smithy/pull/1256))
* Added stack trace comment support to code writer. ([#1198](https://github.com/awslabs/smithy/pull/1198))
* Added an automatic topological sorting of shape in DirectedCodegen. ([#1214](https://github.com/awslabs/smithy/pull/1214))
* Updated CodegenDirector to generate shapes before generating service. ([#1289](https://github.com/awslabs/smithy/pull/1289))
* Updated CodegenDirector to automatically use `SymbolProvider.cache`. ([#1233](https://github.com/awslabs/smithy/pull/1233))
* Made SmithyIntegrations available from CodegenContext. ([#1237](https://github.com/awslabs/smithy/pull/1237))
* Added helper to convert `Symbol` to `SymbolReference`. ([#1220](https://github.com/awslabs/smithy/pull/1220))
* Updated NodeDiff to sort results to make them easier to understand. ([#1238](https://github.com/awslabs/smithy/pull/1238))
* Implemented `Comparable` in `SourceLocation`. ([#1192](https://github.com/awslabs/smithy/pull/1192))
* Added missing validation to ensure that unions have at least one member. ([#1229](https://github.com/awslabs/smithy/pull/1229))
* Added validation to forbid impossibly recursive shapes. ([#1200](https://github.com/awslabs/smithy/pull/1200),
  [#1212](https://github.com/awslabs/smithy/pull/1212), [#1253](https://github.com/awslabs/smithy/pull/1253),
  [#1269](https://github.com/awslabs/smithy/pull/1269))
* Added validation to warn when HTTP 204/205 responses have bodies. ([#1254](https://github.com/awslabs/smithy/pull/1254),
  [#1276](https://github.com/awslabs/smithy/pull/1276))
* Added validation to forbid sparse maps with httpPrefixHeaders. ([#1268](https://github.com/awslabs/smithy/pull/1268))
* Added ability to serialize the prelude. ([#1275](https://github.com/awslabs/smithy/pull/1275))
* Added protocol tests for httpResponseCode. ([#1241](https://github.com/awslabs/smithy/pull/1241))

### Bug fixes

* Enabled the PostUnionWithJsonName protocol test. ([#1239](https://github.com/awslabs/smithy/pull/1239))
* Fixed the MalformedAcceptWithGenericString compliance test. ([#1243](https://github.com/awslabs/smithy/pull/1243))

### Documentation

* Added definition for value equality for `@uniqueItems`. ([#1278](https://github.com/awslabs/smithy/pull/1278))
* Added documentation for Smithy Server Generator for TypeScript. ([#1119](https://github.com/awslabs/smithy/pull/1119))
* Added link to Smithy Diff from Evolving Models guide. ([#1208](https://github.com/awslabs/smithy/pull/1208))
* Fixed constraint traits doc regarding non-structure members. ([#1205](https://github.com/awslabs/smithy/pull/1205))
* Fixed typo in `uniqueItems` warning. ([#1201](https://github.com/awslabs/smithy/pull/1201))
* Clarified `@deprecated` javadocs in `smithy-codegen-core`. ([#1197](https://github.com/awslabs/smithy/pull/1197))
* Clarified Selectors documentation. ([#1196](https://github.com/awslabs/smithy/pull/1196))
* Clarified meaning of Language in implementations. ([#1191](https://github.com/awslabs/smithy/pull/1191))
* Clarified that constraint traits cascade for all members. ([#1205](https://github.com/awslabs/smithy/pull/1205))
* Removed jsonName note from awsJson protocols. ([#1279](https://github.com/awslabs/smithy/pull/1279))

## 1.21.0 (2022-04-13)

### Features
* Added `DirectedCodegen` to make codegen simpler. ([#1167](https://github.com/awslabs/smithy/pull/1167), [#1180](https://github.com/awslabs/smithy/pull/1180))
* Add ability to register interceptors with delegator. ([#1165](https://github.com/awslabs/smithy/pull/1165))
* Optimized deprecated trait validation. ([#1162](https://github.com/awslabs/smithy/pull/1162))
* Used ConcurrentSkipListMap in Model for knowledge instead of synchornized IdentityMap. ([#1161](https://github.com/awslabs/smithy/pull/1161))

### Documentation

* Added http and eventStreamHttp properties to AWS protocols. ([#1172](https://github.com/awslabs/smithy/pull/1172))

## 1.19.0 (2022-03-21)

### Features

* Disallowed `requiresLength` trait in output. ([#1155](https://github.com/awslabs/smithy/pull/1155), [#1152](https://github.com/awslabs/smithy/pull/1152))
* Added validation that `code` in `http` trait is between 100 and 999. ([#1156](https://github.com/awslabs/smithy/pull/1156))
* Added validation that `uri` in `http` trait uses ASCII characters. ([#1154](https://github.com/awslabs/smithy/pull/1154))
* Allowed `jsonName` trait on union members. ([#1153](https://github.com/awslabs/smithy/pull/1153))
* Improved Dockerfile support. ([#1140](https://github.com/awslabs/smithy/pull/1140))
* Added support for conditions and loops to AbstractCodeWriter. ([#1144](https://github.com/awslabs/smithy/pull/1144))
* Added a warning for missing `^` or `$` anchors in `@pattern` trait. ([#1141](https://github.com/awslabs/smithy/pull/1141))
* Added a validator to catch usage of non-inclusive words. ([#931](https://github.com/awslabs/smithy/pull/931))
* Added new classes for code writing and delegation, which deprecates the `software.amazon.smithy.codegen.core.writer`
  package. ([#1131](https://github.com/awslabs/smithy/pull/1131))
* Added a warning for using `@sensitive` trait on members, which will be removed in IDL 2.0. ([#1132](https://github.com/awslabs/smithy/pull/1132))

### Documentation

* Documented the supported OpenAPI version. ([#1151](https://github.com/awslabs/smithy/pull/1151))
* Added links to Scala generator and plugin. ([#1145](https://github.com/awslabs/smithy/pull/1145))

## 1.18.1 (2022-03-10)

### Features
* Downgraded set type violations from ERROR to WARNING to give consumers more time to convert these sets to lists.
  These will be upgraded to ERROR again in a future release. ([#1125](https://github.com/awslabs/smithy/pull/1125))

### Bug Fixes

* Fixed backwards compatibility of CodeWriter and created a new basic implementation of `AbstractCodeWriter` named
  `SimpleCodeWriter`. ([#1123](https://github.com/awslabs/smithy/pull/1123))
* Fixed a bug in `AbstractCodeWriter` where indenting the next line would not be preserved after popping a state.
  ([#1129](https://github.com/awslabs/smithy/pull/1129))
* Fixed a bug in `AbstractCodeWriter` where text could sometimes be lost due to lazy StringBuilder construction.
  ([#1128](https://github.com/awslabs/smithy/pull/1128))
* Fixed a null pointer exception in `ModelAssembler` after calling `reset()`. 
  ([#1124](https://github.com/awslabs/smithy/pull/1124))

### Documentation

* Removed examples showing `@sensitive` on structure members, which is deprecated in IDL 2.0.
  ([#1127](https://github.com/awslabs/smithy/pull/1127))

## 1.18.0 (2022-03-07)

### Breaking changes

* Sets can now only contain byte, short, integer, long, bigInteger, bigDecimal,
  and string shapes. Sets with other types of values are either difficult to
  implement in various programming languages (for example, sets of floats in
  Rust), or highly problematic for client/server use cases. Clients that are
  out of sync with a service model could receive structures or unions from a
  service, not recognize new members and drop them, causing the hash codes of
  members of the set to collide, and this would result in the client discarding
  set entries. For example, a service might return a set of 3 structures, but
  when clients deserialize them, they drop unknown members, and the set
  contains fewer than 3 entries.

  Existing models that already use a set of other types will need to migrate to
  use a list rather than a set, and they will need to implement any necessary
  uniqueness checks server-side as needed.

  **NOTE**: This restriction was downgraded to a WARNING in 1.18.1
  ([#1106](https://github.com/awslabs/smithy/pull/1106))

* Removed unused `UseShapeWriterObserver` and related features. ([#1117](https://github.com/awslabs/smithy/pull/1117))

### Features

* Added interfaces for Codegen integrations, interceptors, and contexts. ([#1109](https://github.com/awslabs/smithy/pull/1109),
  [#1118](https://github.com/awslabs/smithy/pull/1118))
* Added support for typed sections, prependers and appenders, and more explicit newline control to `CodeWriter`. ([#1110](https://github.com/awslabs/smithy/pull/1110))
* Added built-in `Symbol` and `Call` formatters, a typed context, and debug info to `CodeWriter`. ([#1095](https://github.com/awslabs/smithy/pull/1095),
  [#1104](https://github.com/awslabs/smithy/pull/1104))
* Added a `DependencyTracker` for `Symbol`s. ([#1107](https://github.com/awslabs/smithy/pull/1107))
* Rewrote `CodeFormatter` to be easier to understand and evolve. ([#1104](https://github.com/awslabs/smithy/pull/1104))
* Exposed `CodegenWriter`'s `DocumentationWriter`. ([#1083](https://github.com/awslabs/smithy/pull/1083))
* Improved error messages from `SmithyBuilder`. ([#1100](https://github.com/awslabs/smithy/pull/1100))
* Reduced copies made in `smithy-codegen-core` and `smithy-build`. ([#1103](https://github.com/awslabs/smithy/pull/1103))
* Added non-optional method for `@httpMalformedRequestTest` uris. ([#1108](https://github.com/awslabs/smithy/pull/1108))
* Added multi-code-unit characters to `@length` validation tests. ([#1092](https://github.com/awslabs/smithy/pull/1092))
* Added malformed request tests for `set` types. ([#1094](https://github.com/awslabs/smithy/pull/1094))
* Clarified a message for `@httpPayload` binding errors. ([#1113](https://github.com/awslabs/smithy/pull/1113))  
* Deprecated `onSectionAppend` and `onSectionPrepend`. ([#1110](https://github.com/awslabs/smithy/pull/1110))

### Bug Fixes

* Fixed an incorrect warning when the `errors` property was set on a `service`. ([#1120](https://github.com/awslabs/smithy/pull/1120))
* Fixed various issues in protocol tests. ([#1084](https://github.com/awslabs/smithy/pull/1084), [#1040](https://github.com/awslabs/smithy/pull/1040))
* Fixed a failing code path in `SmithyBuild`. ([#1100](https://github.com/awslabs/smithy/pull/1100))

### Documentation

* Added note about escaping `\` in `@pattern`. ([#1091](https://github.com/awslabs/smithy/pull/1091))
* Clarified error serialization behavior for `@restJson1`. ([#1099](https://github.com/awslabs/smithy/pull/1099))
* Clarified defaulting behavior of `@httpResponseCode`. ([#1111](https://github.com/awslabs/smithy/pull/1111))
* Clarified behavior of the `sources` plugin. ([#977](https://github.com/awslabs/smithy/pull/977))
* Clarified how `@length` interacts with UTF-8 encoding. ([#1089](https://github.com/awslabs/smithy/pull/1089))
* Fixed an `@idRef` example. ([#1087](https://github.com/awslabs/smithy/pull/1087))

### Other

* Migrated to using Gradle 7.3.3 to build Smithy. This should have no impactful downstream effects. ([#1085](https://github.com/awslabs/smithy/pull/1085))

## 1.17.0 (2022-02-04)

### Bug Fixes
* Updated `@streaming` validation for protocols that support `@httpPayload`.
  ([#1076](https://github.com/awslabs/smithy/pull/1076))

### Features
* Added ability to serialize the JMESPath AST. ([#1059](https://github.com/awslabs/smithy/pull/1059))
* Update `CodeWriter` to add getters and ability to copy settings. ([#1067](https://github.com/awslabs/smithy/pull/1067))

### Documentation
* Clarified `outputToken`. ([#1056](https://github.com/awslabs/smithy/pull/1056))
* Removed repeated words. ([#1063](https://github.com/awslabs/smithy/pull/1063))
* Clarified server behavior for query parameter deserialization. ([#1080](https://github.com/awslabs/smithy/pull/1080))

### Other
* Updated traits to preserve the original `Node` value from the model.
  ([#1047](https://github.com/awslabs/smithy/pull/1047))

## 1.16.3 (2022-01-13)

### Bug Fixes
* Removed @internal from the @unitType trait. ([#1054](https://github.com/awslabs/smithy/pull/1054))
* Fixed JMESPath and-expression evaluation to correctly provide the result of the
  left expression when it is falsey. ([#1053](https://github.com/awslabs/smithy/pull/1053))
* Fixed quoted string headers restJson1 response protocol test. ([#1049](https://github.com/awslabs/smithy/pull/1049))

## 1.16.2 (2022-01-10)

### Features
* Renamed `StutteredShapeName` validator to `RepeatingShapeName` and added an `exactMatch` configuration to let it more
  precisely prevent problematic models. ([#1041](https://github.com/awslabs/smithy/pull/1041))
* Reduced the severity of `HttpBindingsMissing` events for services that do not use protocols that support the `@http`
  trait. ([#1044](https://github.com/awslabs/smithy/pull/1044))
* Added `unwrite()` to `CodeWriter`. ([#1038](https://github.com/awslabs/smithy/pull/1038))

### Bug Fixes
* Fixed the `RestJsonInputUnionWithUnitMember` protocol test. ([#1042](https://github.com/awslabs/smithy/pull/1042))

### Documentation
* Updated the documentation for pagination. ([#1043](https://github.com/awslabs/smithy/pull/1043))

## 1.16.1 (2022-01-06)

### Features
* Make `smithy.api#Unit` easier to adopt by excluding direct relationships between it and operation inputs and outputs.
  ([#1034](https://github.com/awslabs/smithy/pull/1034))

### Bug Fixes
* Fixed character escaping in a restJson1 protocol test. ([#1035](https://github.com/awslabs/smithy/pull/1035))

## 1.16.0 (2022-01-03)

### Features
* Added `smithy.api#Unit` and `@input` and `@output` traits. ([#980](https://github.com/awslabs/smithy/pull/980),
  [#1005](https://github.com/awslabs/smithy/pull/1005))
* Removed support for collection values for `@httpPrefixHeaders`. ([#1022](https://github.com/awslabs/smithy/pull/1022))
* Added a protocol test for handling path segments that contain regex expressions. 
  ([#1018](https://github.com/awslabs/smithy/pull/1018))

### Bug Fixes
* Removed `jsonName` from the `awsJson` protocol tests and documentation.
  ([#1026](https://github.com/awslabs/smithy/pull/1026))
* Reverted changes to timestamp list header serialization protocol tests.
  ([#1023](https://github.com/awslabs/smithy/pull/1023))
* Fixed links in the search results of Smithy's javadocs.
  ([#1009](https://github.com/awslabs/smithy/pull/1009))
* Fixed duplication of validation events for conflicting names.
  ([#999](https://github.com/awslabs/smithy/pull/999))

### Documentation
* Added links to Kotlin and Swift generators.
  ([#1020](https://github.com/awslabs/smithy/pull/1020))
* Clarified matching of URIs where greedy labels have no matching segment.
  ([#1013](https://github.com/awslabs/smithy/pull/1013))

### Other
* Added minor optimizations. ([#1028](https://github.com/awslabs/smithy/pull/1028), 
  [#1027](https://github.com/awslabs/smithy/pull/1027),
  [#1004](https://github.com/awslabs/smithy/pull/1004))
* Added Apple silicon target for smithy-cli. ([#1012](https://github.com/awslabs/smithy/pull/1012))
* Updated smithy-cli to use JDK 17. ([#1003](https://github.com/awslabs/smithy/pull/1003))

## 1.15.0 (2021-12-02)

### Features
* Added protocol tests for quoted strings in headers. ([#986](https://github.com/awslabs/smithy/pull/986))

### Bug Fixes

* Fixed `filterSuppressions` transform's handling of members. ([#989](https://github.com/awslabs/smithy/pull/989))
* Fixed http-content-type protocol tests. ([#993](https://github.com/awslabs/smithy/pull/993))

### Documentation

* Fixed documentation regarding `@length` and `@auth` traits. ([#988](https://github.com/awslabs/smithy/pull/988), [#997](https://github.com/awslabs/smithy/pull/997))
* Added documentation for `httpMalformedRequestTests`. ([#973](https://github.com/awslabs/smithy/pull/973))

### Other

* Upgraded to use version `0.6.0` of the [Smithy Gradle Plugin](https://github.com/awslabs/smithy-gradle-plugin). ([#996](https://github.com/awslabs/smithy/pull/996))
* Reduced number of copies builders need to make when building up immutable objects. ([#995](https://github.com/awslabs/smithy/pull/995))
* Ensured InputStreams in loader are closed. ([#991](https://github.com/awslabs/smithy/pull/991))

## 1.14.1 (2021-11-15)

### Features

* Updated the `@aws.protocols#httpChecksum` trait to use uppercase algorithm names. ([#982](https://github.com/awslabs/smithy/pull/982))

### Bug Fixes

* Fixed an issue where JSON Schema conversion wouldn't remove out-of-service references before deconflicting. ([#978](https://github.com/awslabs/smithy/pull/978))
* Fixed IAM condition key inference not using the `@aws.iam#iamResource` trait. ([#981](https://github.com/awslabs/smithy/pull/981))

## 1.14.0 (2021-11-10)

### Features

* Added the `@aws.protocols#httpChecksum` trait to describe checksumming behavior for operations. ([#972](https://github.com/awslabs/smithy/pull/972))

### Bug Fixes

* Fixed a bug that used a JSON pointer instead of names when generating CloudFormation Resource Schema
  required properties. ([#971](https://github.com/awslabs/smithy/pull/971))

### Documentation

* Clarified parsing of members marked with the `@httpQueryParams` trait. ([#957](https://github.com/awslabs/smithy/pull/957))

## 1.13.1 (2021-11-02)

### Bug Fixes

* Fixed a bug that caused the `apply` transform to not run its projections. ([#969](https://github.com/awslabs/smithy/pull/969))

### Documentation

* Clarified uri label and greedy label documentation. ([#965](https://github.com/awslabs/smithy/pull/965), [#968](https://github.com/awslabs/smithy/pull/968))

## 1.13.0 (2021-10-29)

### Features

* Added a `filterSuppressions` model transform. ([#940](https://github.com/awslabs/smithy/pull/940))
* Updated selector attributes to be stricter. ([#946](https://github.com/awslabs/smithy/pull/946))
* Added support for generating the `required` property when generating CloudFormation Resource Schemas. ([#937](https://github.com/awslabs/smithy/pull/937))
* Added support for generating the `handlers` property when generating CloudFormation Resource Schemas. ([#939](https://github.com/awslabs/smithy/pull/939))
* Added the `@aws.iam#iamResource` trait to indicate properties of a Smithy resource in AWS IAM. ([#948](https://github.com/awslabs/smithy/pull/948))
* Added the `@aws.iam#supportedPrincipleTypes` trait to indicate which IAM principal types can use a service or
  operation. ([#941](https://github.com/awslabs/smithy/pull/941))
* Updated model serializers to allow for serializing the prelude. ([#955](https://github.com/awslabs/smithy/pull/955))
* Updated JSON Schema conversion to maintain property order. ([#932](https://github.com/awslabs/smithy/pull/932))
* Improved `@httpApiKeyAuth` description when converting to OpenAPI. ([#934](https://github.com/awslabs/smithy/pull/934))
* Updated the error message received when http request body content issues are encountered. ([#959](https://github.com/awslabs/smithy/pull/959))
* Updated request tests for `restJson1` query strings. ([#933](https://github.com/awslabs/smithy/pull/933), [#958](https://github.com/awslabs/smithy/pull/958))
* Added protocol tests for `restJson1` content types. ([#924](https://github.com/awslabs/smithy/pull/924), [#945](https://github.com/awslabs/smithy/pull/945))

### Bug Fixes

* Fixed issues in model loading that required a service `version` property. ([#936](https://github.com/awslabs/smithy/pull/936))
* Fixed an issue that where CORS headers in OpenAPI conversions were not case-insensitive. ([#950](https://github.com/awslabs/smithy/pull/950))
* Fixed various issues in protocol tests. ([#930](https://github.com/awslabs/smithy/pull/930), [#933](https://github.com/awslabs/smithy/pull/933),
  [#935](https://github.com/awslabs/smithy/pull/935), [#944](https://github.com/awslabs/smithy/pull/944), [#949](https://github.com/awslabs/smithy/pull/949),
  [#954](https://github.com/awslabs/smithy/pull/954))

### Documentation

* Clarified host-related settings in the `@httpRequestTests` trait documentation. ([#951](https://github.com/awslabs/smithy/pull/951))
* Clarified uri samples and descriptions. ([#960](https://github.com/awslabs/smithy/pull/960))
* Fixed some issues in documentation. ([#952](https://github.com/awslabs/smithy/pull/952))

## 1.12.0 (2021-10-05)

### Features

* Added support for binding common errors to a `service` shape. ([#919](https://github.com/awslabs/smithy/pull/919))
* Loosened the requirement of setting a `version` property when defining a `service`. ([#918](https://github.com/awslabs/smithy/pull/918))
* Updated `smithy-build` to fail when a build plugin cannot be found. ([#909](https://github.com/awslabs/smithy/pull/909))
* Added a `changeTypes` build transform. ([#912](https://github.com/awslabs/smithy/pull/912))
* Added support for replacing simple shapes in `ModelTransformer`. ([#900](https://github.com/awslabs/smithy/pull/900))
* Added a `scheme` property to the `@httpApiKeyAuth` trait. ([#893](https://github.com/awslabs/smithy/pull/893))
* Added support for specifying errors in the `@examples` trait. ([#888](https://github.com/awslabs/smithy/pull/888))
* Added multi-character newline support in `CodeWriter`. ([#892](https://github.com/awslabs/smithy/pull/892))
* Updated semantic validation of modeled `OPTIONS` operations. ([#890](https://github.com/awslabs/smithy/pull/890))
* Added several malformed request protocol tests. ([#879](https://github.com/awslabs/smithy/pull/879), [#882](https://github.com/awslabs/smithy/pull/882),
  [#881](https://github.com/awslabs/smithy/pull/881), [#898](https://github.com/awslabs/smithy/pull/898), [#901](https://github.com/awslabs/smithy/pull/901),
  [#905](https://github.com/awslabs/smithy/pull/905), [#908](https://github.com/awslabs/smithy/pull/908))
* Added protocol tests for path prefixes. ([#899](https://github.com/awslabs/smithy/pull/899))

### Bug Fixes

* Fixed how `NodeMapper` handles generic params. ([#912](https://github.com/awslabs/smithy/pull/912))
* Fixed how the CLI logs messages and interacts with logging levels. ([#910](https://github.com/awslabs/smithy/pull/910))

### Documentation

* Updated guidance on ordering of `set` shapes. ([#875](https://github.com/awslabs/smithy/pull/875))
* Clarify that event streams contain modeled errors. ([#891](https://github.com/awslabs/smithy/pull/891))
* Added an index that lists all traits. ([#876](https://github.com/awslabs/smithy/pull/876))
* Fixed various documentation issues. ([#884](https://github.com/awslabs/smithy/pull/884), [#911](https://github.com/awslabs/smithy/pull/911),
  [#927](https://github.com/awslabs/smithy/pull/927))

## 1.11.0 (2021-08-03)

### Features

* Updated CORS header configuration when converting to OpenAPI while using `sigv4` or `restJson1`. ([#868](https://github.com/awslabs/smithy/pull/868))
* Added the (unstable) `httpMalformedRequestTests` trait to validate service behavior around malformed requests. ([#871](https://github.com/awslabs/smithy/pull/871))
* Added `smithy-diff` error when an enum entry is inserted. ([#873](https://github.com/awslabs/smithy/pull/873))
* Added a `restXml` protocol test. ([#866](https://github.com/awslabs/smithy/pull/866))
* Added a `httpChecksumRequired` protocol test. ([#869](https://github.com/awslabs/smithy/pull/869))

### Bug Fixes

* Updated `NodeMapper` to properly handle `sourceLocation` for traits. ([#865](https://github.com/awslabs/smithy/pull/865))
* Removed warning when an operation using the HTTP `PATCH` method is marked with the `@idempotent` trait. ([#867](https://github.com/awslabs/smithy/pull/867))
* Fixed several issues where a `sourceLocation` wasn't propagated for traits. ([#864](https://github.com/awslabs/smithy/pull/864))

### Documentation

* Fixed various documentation issues. ([#870](https://github.com/awslabs/smithy/pull/870), [#874](https://github.com/awslabs/smithy/pull/874))

## 1.10.0 (2021-07-14)

### Features

* Loosened the requirement of setting an `error` property when configuring `aws.api#clientEndpointDiscovery` trait. ([#850](https://github.com/awslabs/smithy/pull/850))
* Added a `restJson1` protocol test. ([#845](https://github.com/awslabs/smithy/pull/845))
* Added a warning when using the OpenAPI conversion `jsonAdd` setting to alter schemas. ([#851](https://github.com/awslabs/smithy/pull/851))
* Added the `httpChecksum` trait. ([#843](https://github.com/awslabs/smithy/pull/843))

### Bug Fixes

* Revert "Tightened substitution pattern for Fn::Sub to match CloudFormation." ([#858](https://github.com/awslabs/smithy/pull/858))
* Fixed an issue where `cors` trait `additionalExposedHeaders` were not added to gateway responses. ([#852](https://github.com/awslabs/smithy/pull/852))
* Fixed various issues in protocol tests. ([#849](https://github.com/awslabs/smithy/pull/849), [#855](https://github.com/awslabs/smithy/pull/855), [#857](https://github.com/awslabs/smithy/pull/857))

### Documentation

* Clarified behavior for the `aws.api#service` trait's `sdkId` member. ([#848](https://github.com/awslabs/smithy/pull/848))
* Fixed various typos. ([#853](https://github.com/awslabs/smithy/pull/853), [#859](https://github.com/awslabs/smithy/pull/859))

## 1.9.1 (2021-06-28)

### Bug Fixes

* Fixed a number of protocol tests related to non-numeric floats. ([#844](https://github.com/awslabs/smithy/pull/844))
* Tightened substitution pattern for Fn::Sub to match CloudFormation. ([#842](https://github.com/awslabs/smithy/pull/842))

## 1.9.0 (2021-06-23)

### Features

* Added a common validation model for use in server SDKs. ([#813](https://github.com/awslabs/smithy/pull/813))
* Added support for cross platform builds of the CLI. ([#832](https://github.com/awslabs/smithy/pull/832))
* Validate the contents of protocol test bodies for known media types. ([#822](https://github.com/awslabs/smithy/pull/822),
  [#835](https://github.com/awslabs/smithy/pull/835))
* Updated support for non-numeric floating-point values in several places. ([#828](https://github.com/awslabs/smithy/pull/828))
* Added several `restJson` protocol tests. ([#684](https://github.com/awslabs/smithy/pull/684))
* Added several `restXml` protocol tests. ([#804](https://github.com/awslabs/smithy/pull/804))
* Added and updated several `awsQuery` and `ec2Query` protocol tests. ([#815](https://github.com/awslabs/smithy/pull/815),
  [#833](https://github.com/awslabs/smithy/pull/833))
* Added several `document` type protocol tests. ([#810](https://github.com/awslabs/smithy/pull/810))
* Added `s3UnwrappedXmlOutput` trait, which defines when an S3 operation does not use the protocol standard XML wrapper.
  ([#839](https://github.com/awslabs/smithy/pull/839))

### Bug Fixes

* Fixed a `NullPointerException` when loading a config and no parent path is present. ([#814](https://github.com/awslabs/smithy/pull/814))

### Documentation

* Added an overview of known Smithy implementations and projects. ([#830](https://github.com/awslabs/smithy/pull/830), [#831](https://github.com/awslabs/smithy/pull/831))
* Improved the documentation for the `restXml`. ([#827](https://github.com/awslabs/smithy/pull/827))
* Improved the documentation for the `awsQuery`. ([#827](https://github.com/awslabs/smithy/pull/827))
* Improved the documentation for the `ec2Query` protocol. ([#823](https://github.com/awslabs/smithy/pull/823), [#827](https://github.com/awslabs/smithy/pull/827),
  [#836](https://github.com/awslabs/smithy/pull/836))
* Added more context for documentation types. ([#818](https://github.com/awslabs/smithy/pull/818))
* Fixed several minor documentation issues. ([#816](https://github.com/awslabs/smithy/pull/816), [#818](https://github.com/awslabs/smithy/pull/818),
  [#837](https://github.com/awslabs/smithy/pull/837), [#840](https://github.com/awslabs/smithy/pull/840))

## 1.8.0 (2021-05-20)

### Features
* Added `awsQueryError` trait, which defines the value in the `Code` distinguishing field. ([#807](https://github.com/awslabs/smithy/pull/807))
* Added methods to get shapes by type and trait. ([#806](https://github.com/awslabs/smithy/pull/806))
* Improved performance. ([#805](https://github.com/awslabs/smithy/pull/805))
* Improved percent-encoding tests and doumentation. ([#803](https://github.com/awslabs/smithy/pull/803))
* Added `double` format to epoch-seconds timestamps when converting to OpenAPI. ([#802](https://github.com/awslabs/smithy/pull/802))
* Improved CLI output. ([#800](https://github.com/awslabs/smithy/pull/800), [#801](https://github.com/awslabs/smithy/pull/801))

### Bug Fixes
* Fixed awsQuery protocol test to show distinction from ignored `@xmlNamespace` trait. ([#799](https://github.com/awslabs/smithy/pull/799))

## 1.7.2 (2021-05-11)

### Bug Fixes
* Fixed a bug where unions would cause CloudFormation schema conversion to fail. ([#794](https://github.com/awslabs/smithy/pull/794))
* Fixed an incorrect restXml protocol test. ([#795](https://github.com/awslabs/smithy/pull/795))

## 1.7.1 (2021-05-07)

### Features
* Added the `recommended` structure member trait, which indicates that a structure member SHOULD be set. ([#745](https://github.com/awslabs/smithy/pull/745))
* Added support for service renames when using the `flattenNamespaces` transformer. ([#760](https://github.com/awslabs/smithy/pull/760))
* Set `additionalProperties` to `false` for CloudFormation objects. ([#764](https://github.com/awslabs/smithy/pull/764))
* Improved model validation debugging by stopping validation when an `ERROR` occurs while loading models. ([#775](https://github.com/awslabs/smithy/pull/775))
* Added validation warning when a `hostPrefix` contains a label that does not end in a period. ([#779](https://github.com/awslabs/smithy/pull/779))
* Added and updated several `@restXml` protocol test. ([#744](https://github.com/awslabs/smithy/pull/744), [#755](https://github.com/awslabs/smithy/pull/755),
  [#757](https://github.com/awslabs/smithy/pull/757), [#777](https://github.com/awslabs/smithy/pull/777), [#766](https://github.com/awslabs/smithy/pull/781),
  [#789](https://github.com/awslabs/smithy/pull/789))
* Added and updated several `@restJson1` protocol test. ([#747](https://github.com/awslabs/smithy/pull/747), [#755](https://github.com/awslabs/smithy/pull/755),
  [#765](https://github.com/awslabs/smithy/pull/765), [#790](https://github.com/awslabs/smithy/pull/790))
* Added missing `name` properties to `aws.iam#ConditionKeyType` enum. ([#759](https://github.com/awslabs/smithy/pull/759))

### Bug Fixes
* Fixed number parsing in the IDL, using BigDecimal or BigInteger where needed. ([#766](https://github.com/awslabs/smithy/pull/766))
* Fixed Gradle 7 builds. ([#758](https://github.com/awslabs/smithy/pull/758))
* Added `Document` type to list of inherently boxed shapes. ([#749](https://github.com/awslabs/smithy/pull/749))
* Reordered `TraitService` SPI entries for readability. ([#742](https://github.com/awslabs/smithy/pull/742))

### Documentation
* Fixed `selector_expression` and `comment` in ABNF for Smithy IDL. ([#771](https://github.com/awslabs/smithy/pull/771),
  [#771](https://github.com/awslabs/smithy/pull/773))
* Documented conflict resolution of HTTP query params. ([#783](https://github.com/awslabs/smithy/pull/783))
* Documented precedence of constraint traits. ([#784](https://github.com/awslabs/smithy/pull/784))

### Other
* Upgraded to use version `0.5.3` of the [Smithy Gradle Plugin](https://github.com/awslabs/smithy-gradle-plugin). ([#791](https://github.com/awslabs/smithy/pull/791))

## 1.7.0 (2021-03-12)

### Features

* Added the `rename` property to the `service` shape to disambiguate shape name conflicts in the service closure. ([#734](https://github.com/awslabs/smithy/pull/734))
* Added the `httpQueryParams` trait that binds a map of key-value pairs to query string parameters. ([#735](https://github.com/awslabs/smithy/pull/735))
* Improved the usability of code for building and running Selectors. ([#726](https://github.com/awslabs/smithy/pull/726))
* Added several protocol tests for behavior around `null` serialization. ([#728](https://github.com/awslabs/smithy/pull/728))

### Documentation

* Added missing documentation for some trait models. ([#737](https://github.com/awslabs/smithy/pull/737))
* Fixed `awsQuery` and `ec2Query` list serialization examples. ([#732](https://github.com/awslabs/smithy/pull/732))

## 1.6.1 (2021-02-23)

### Features

* Added the `renameShapes` build transform to rename shapes within a model. ([#721](https://github.com/awslabs/smithy/pull/721))

### Bug Fixes

* Fixed several issues in protocol tests around `@endpoint`. ([#720](https://github.com/awslabs/smithy/pull/720))

## 1.6.0 (2021-02-22)

### Features

* Added support for checking backwards compatibility for diffs of trait contents. ([#716](https://github.com/awslabs/smithy/pull/716))
* Added support for adding CORS configurations to API Gateway HTTP APIs. ([#670](https://github.com/awslabs/smithy/pull/670))
* Relaxed constraints on the `@httpPayload` trait, allowing it to target `list`, `set`, and `map` shapes except in AWS protocols. ([#679](https://github.com/awslabs/smithy/pull/679), [#683](https://github.com/awslabs/smithy/pull/683))
* Added validation to ensure a `payloadFormatVersion` is set when generating an API Gateway HTTP API. ([#688](https://github.com/awslabs/smithy/pull/688))
* Added `vendorParamsShape` to protocol test cases to support validating a test case's `vendorParams` values are
  configured properly. ([#702](https://github.com/awslabs/smithy/pull/702))
* Added the ability to validate resolved hosts to protocol tests. ([#707](https://github.com/awslabs/smithy/pull/707))
* Added backwards compatibility checking to `smithy-diff` for the `@paginated` trait. ([#716](https://github.com/awslabs/smithy/pull/716))
* Added `tags` and `appliesTo` to protocol test definitions for better categorization and grouping. ([#696](https://github.com/awslabs/smithy/pull/696))
* Added several protocol tests for the `endpoint` and `hostLabel` traits. ([#708](https://github.com/awslabs/smithy/pull/708))
* Added several `@restXml` protocol tests. ([#689](https://github.com/awslabs/smithy/pull/689), [#690](https://github.com/awslabs/smithy/pull/690),
  [#678](https://github.com/awslabs/smithy/pull/678), [#694](https://github.com/awslabs/smithy/pull/694))
* Added a configuration definition for use validating `vendorParams` in AWS protocol tests. ([#705](https://github.com/awslabs/smithy/pull/705))
* Added tests and documentation for some required Amazon S3 customizations. ([#709](https://github.com/awslabs/smithy/pull/709))
* Added tests and documentation for required Amazon Glacier customizations. ([#704](https://github.com/awslabs/smithy/pull/704))
* Added a test and documentation for the required Amazon API Gateway customization. ([#706](https://github.com/awslabs/smithy/pull/706))
* Added a test and documentation for the required Amazon Machine Learning customization. ([#707](https://github.com/awslabs/smithy/pull/707)) 

### Bug Fixes

* Fixed an issue that produced duplicate entries in the `security` list of a converted OpenAPI document. ([#687](https://github.com/awslabs/smithy/pull/687))
* Fixed an issue where `alphanumericOnlyRefs` was not fully satisfied when generating synthesized shapes. ([#695](https://github.com/awslabs/smithy/pull/695))
* Fixed several issues in IDL parsing where duplicate bindings were allowed incorrectly. ([#714](https://github.com/awslabs/smithy/pull/714))
* Fixed several issues in protocol tests around serialization of empty contents. ([#692](https://github.com/awslabs/smithy/pull/692))
* Fixed an issue where parameters in a diff error message were swapped. ([#713](https://github.com/awslabs/smithy/pull/713))
* Fixed an issue in a `restXml` protocol test. ([#715](https://github.com/awslabs/smithy/pull/715))

### Documentation

* Improved the documentation for the `awsJson1_0` and `awsJson1_1` protocols. ([#698](https://github.com/awslabs/smithy/pull/698))
* Improved the documentation for the `awsQuery` and `ec2Query` protocols. ([#700](https://github.com/awslabs/smithy/pull/700))
* Clarified that Smithy requires support for fractional seconds for the `http-date` value of `@timestampFormat`. ([#672](https://github.com/awslabs/smithy/pull/672))
* Added missing shape documentation for some waiters related shapes. ([#711](https://github.com/awslabs/smithy/pull/711))
* Fixed several minor documentation issues. ([#681](https://github.com/awslabs/smithy/pull/681), [#693](https://github.com/awslabs/smithy/pull/693),
  [#697](https://github.com/awslabs/smithy/pull/697), [#701](https://github.com/awslabs/smithy/pull/701), [#708](https://github.com/awslabs/smithy/pull/708),
  [#717](https://github.com/awslabs/smithy/pull/717))

### Other

* Migrated to using Gradle 6 to build Smithy. This should have no impactful downstream effects. ([#194](https://github.com/awslabs/smithy/pull/194))
* Migrated to using `main` from `master` for the default branch. This should have no impactful downstream effects. ([#685](https://github.com/awslabs/smithy/pull/685))

## 1.5.1 (2020-12-21)

### Bug Fixes

* Fixed several issues related to building and running on Windows. ([#671](https://github.com/awslabs/smithy/pull/671))
* Fixed an issue loading the `jsonAdd` map from configuration for the `cloudformation` plugin. ([#673](https://github.com/awslabs/smithy/pull/673))
* Fixed an issue where API Gateway REST APIs would have greedy label parameter names rendered into OpenAPI with
  a `+` suffix. ([#674](https://github.com/awslabs/smithy/pull/674))

## 1.5.0 (2020-12-10)

### Features

* Added the `endpointPrefix` property to the `@aws.api#service` trait. ([#663](https://github.com/awslabs/smithy/pull/663))
* Added support for `tags` and `deprecated` members to `@waitable` definitions. ([#652](https://github.com/awslabs/smithy/pull/652))
* Added validation for `@httpHeader` trait values. ([#650](https://github.com/awslabs/smithy/pull/650))
* Add `required` property for `requestBody` when converting to OpenAPI. ([#655](https://github.com/awslabs/smithy/pull/655))
* Added more helper methods to `OperationIndex`. ([#657](https://github.com/awslabs/smithy/pull/657))

### Bug Fixes

* Ensure that names in the `@waitable` trait are unique in the closure of the service. ([#645](https://github.com/awslabs/smithy/pull/645))
* Fixed a regression with `@aws.apigateway#authorizors` behavior when setting the `customAuthType` property without
  having set its `type` property. ([#613](https://github.com/awslabs/smithy/pull/613))
* Fixed an issue where modeled headers were not populated in to `Access-Control-Expose-Headers` in CORS responses.
  ([#659](https://github.com/awslabs/smithy/pull/659))
* Added missing `deprecated` member to `@enum` definitions in the prelude model. ([#651](https://github.com/awslabs/smithy/pull/651))
* Fixed an issue with the conversion of greedy label parameter names in to OpenAPI. ([#641](https://github.com/awslabs/smithy/pull/641))
* Fixed an issue in `CodeWriter.popState` where it would not honor custom expression start characters. ([#648](https://github.com/awslabs/smithy/pull/648))
* Fixed a potential `NullPointerException` when validating the `@examples` trait. ([#642](https://github.com/awslabs/smithy/pull/642))
* Fixed issues with some `@awsQuery` and `@ec2Query` protocol test responses. ([#653](https://github.com/awslabs/smithy/pull/653))
* Fixed an issue where the `removeTraitDefinitions` build transform was not registered with the SPI. ([#660](https://github.com/awslabs/smithy/pull/660))
* Fixed an issue where using an environment variable in `smithy-build.json` would consume an extra preceding
  character when performing a replacement. ([#662](https://github.com/awslabs/smithy/pull/662)) 

### Documentation

* Update `@waitable` documentation to specify using jitter and account for overflows. ([#656](https://github.com/awslabs/smithy/pull/656))
* Added examples and clarified documentation for several HTTP traits, most importantly `@httpLabel` and `@httpQuery`.
  ([#654](https://github.com/awslabs/smithy/pull/654))
* Clarified various aspects of the `@xmlNamespace` trait documentation. ([#643](https://github.com/awslabs/smithy/pull/643))
* Clarified `@waitable` documentation. ([#646](https://github.com/awslabs/smithy/pull/646), [#664](https://github.com/awslabs/smithy/pull/664))
* Clarified that the `@pattern` trait does not implicitly match an entire string. ([#649](https://github.com/awslabs/smithy/pull/649))
* Fixed various examples in the specification. ([#639](https://github.com/awslabs/smithy/pull/639))

### Other

* Sort `TopDownIndex` contents to provide deterministic results. ([#667](https://github.com/awslabs/smithy/pull/667))
* Improved error messages when an unknown annotation trait is encountered. ([#644](https://github.com/awslabs/smithy/pull/644))
* Added `smithy-diff` error when the `@idempotencyTrait` token is removed from a shape. ([#640](https://github.com/awslabs/smithy/pull/640))

## 1.4.0 (2020-11-20)

### Features

* Added `smithy-jmespath`, a dependency-less, JMESPath parser with a rich AST that can be used in code generation, and
  performs static analysis of expressions. ([#621](https://github.com/awslabs/smithy/pull/621))
* Added `smithy-waiters`, containing the `@waitable` trait. This provides information that clients can use to poll
  until a desired state is reached, or it is determined that state cannot be reached. ([#623](https://github.com/awslabs/smithy/pull/623))
* Added `smithy-aws-cloudformation-traits`, containing several (unstable) traits that indicate CloudFormation resources
  and the additional metadata about their properties. ([#579](https://github.com/awslabs/smithy/pull/579))
* Added `smithy-aws-cloudformation`, containing the (unstable) "cloudformation" build tool that, given a model
  decorated with traits from `aws.cloudformation`, will generate CloudFormation Resource Schemas. ([#622](https://github.com/awslabs/smithy/pull/622))
* Added support for `patternProperties` when generating JSON Schema. ([#611](https://github.com/awslabs/smithy/pull/611))
* Added more utility methods to the `CodeWriter`. ([#624](https://github.com/awslabs/smithy/pull/624))
* Added validation for `@sensitive` trait when applied to members. ([#609](https://github.com/awslabs/smithy/pull/609))
* Added support for retrieving full paths to the `outputToken` and `items` pagination members. ([#628](https://github.com/awslabs/smithy/pull/628))
* Added a warning for `@enum` entries without names. ([#610](https://github.com/awslabs/smithy/pull/610))
* Added support for generating an `integer` OpenAPI type. ([#632](https://github.com/awslabs/smithy/pull/632))
* Improved `smithy-diff` evaluation of changing member targets. ([#630](https://github.com/awslabs/smithy/pull/630))
* Updated pagination tokens to support being `map` shapes. ([#629](https://github.com/awslabs/smithy/pull/629))

### Bug Fixes

* Fixed a bug where URIs would be declared conflicting if the differed through the `@endpoint` trait. ([#626](https://github.com/awslabs/smithy/pull/626))
* Fixed a bug that would allow the `@aws.auth#sigv4` trait's `name` property to be empty. ([#635](https://github.com/awslabs/smithy/pull/635))
* Updated protocol tests for `@sparse` trait. ([#620](https://github.com/awslabs/smithy/pull/620), [#631](https://github.com/awslabs/smithy/pull/631))
* Fixed a bug with the interaction of `CodeWriter.writeInline` with sections. ([#617](https://github.com/awslabs/smithy/pull/617))

### Documentation

* Fixed links for protocol test suites. ([#615](https://github.com/awslabs/smithy/pull/615))
* Added example and test for composing with `CodeWriter`. ([#619](https://github.com/awslabs/smithy/pull/619))
* Clarified that `@enum` values cannot be empty. ([#633](https://github.com/awslabs/smithy/pull/633))
* Clarified binary data in protocol tests. ([#634](https://github.com/awslabs/smithy/pull/634))

### Other

* Lowered severity of validation that a `pageSize` member is marked `@required`. ([#612](https://github.com/awslabs/smithy/pull/612))

## 1.3.0 (2020-10-20)

### Features

* Added several `CodegenWriter` and related abstractions to simplify creating code generators. ([#587](https://github.com/awslabs/smithy/pull/587))
* Added the `@sparse` trait to the Prelude. ([#599](https://github.com/awslabs/smithy/pull/599))
* Added the `NullableIndex` to help check if a shape can be set to null. ([#599](https://github.com/awslabs/smithy/pull/599))
* Added support for API Gateway API key usage plans. ([#603](https://github.com/awslabs/smithy/pull/603), [#605](https://github.com/awslabs/smithy/pull/605))
* Added the `sortMembers` model transform to reorder the members of structures and unions. ([#588](https://github.com/awslabs/smithy/pull/588))
* Add `description` property to operations when converting to OpenAPI. ([#589](https://github.com/awslabs/smithy/pull/589))

### Bug Fixes

* Fixed an issue where the `flattenNamespaces` build transform was not registered with the SPI. ([#593](https://github.com/awslabs/smithy/pull/593))

### Documentation

* Clarified that `map` keys, `set` values, and `union` members cannot be null. ([#596](https://github.com/awslabs/smithy/pull/596/))
* Clarified `enum` names and their usage. ([#601](https://github.com/awslabs/smithy/pull/601))
* Added an example dependency to OpenAPI conversion. ([#594](https://github.com/awslabs/smithy/pull/594))
* Improve and clean up formatting. ([#585](https://github.com/awslabs/smithy/pull/585), [#597](https://github.com/awslabs/smithy/pull/597),
  [#598](https://github.com/awslabs/smithy/pull/598))

### Other

* Optimized the reverse `NeighborProvider` for memory usage. ([#590](https://github.com/awslabs/smithy/pull/590))
* Optimized model validation event aggregation for memory usage. ([#595](https://github.com/awslabs/smithy/pull/595))
* Updated `service`, `resource`, and `operation` shapes to maintain the order of bound `resource` and `operation`
  shapes. ([#602](https://github.com/awslabs/smithy/pull/602))
* Updated the `sources` build plugin to create an empty manifest if there are no source models. ([#607](https://github.com/awslabs/smithy/pull/607))
* Deprecated the `BoxIndex`. ([#599](https://github.com/awslabs/smithy/pull/599))
* Added `enum` names for `httpApiKeyLocation` in the Prelude. ([#606](https://github.com/awslabs/smithy/pull/606))

## 1.2.0 (2020-09-30)

### Features

* Added information to the `ModelDiff.Result` indicating how events have changed between the diff'd models. ([#574](https://github.com/awslabs/smithy/pull/574))
* Added a media type parser and validation for the `@mediaType` trait. ([#582](https://github.com/awslabs/smithy/pull/582))
* Added additional default CORS headers and configuration for OpenAPI conversions. ([#583](https://github.com/awslabs/smithy/pull/583))
* Added the `flattenNamespaces` build transform to flatten the namespaces of shapes connected to a specified service
  in a model in to a target namespace. ([#572](https://github.com/awslabs/smithy/pull/572))
* Added `runCommand` functionality to `smithy-utils`. ([#580](https://github.com/awslabs/smithy/pull/580))
* Added a `TriConsumer` to `smithy-utils`. ([#581](https://github.com/awslabs/smithy/pull/581))
* Added support for the `@httpResponseCode` trait in the `HttpBindingIndex`. ([#571](https://github.com/awslabs/smithy/pull/571))
* Added protocol tests for the `@httpResponseCode` trait. ([#573](https://github.com/awslabs/smithy/pull/573))

### Bug Fixes

* Fixed several issues that would cause Smithy to fail when running on Windows. ([#575](https://github.com/awslabs/smithy/pull/575),
  [#576](https://github.com/awslabs/smithy/pull/576), [#577](https://github.com/awslabs/smithy/pull/577))
* Fixed a bug where a `union` shape marked as an `@httpPayload` would throw an exception when trying to resolve
  its content-type. ([#584](https://github.com/awslabs/smithy/pull/584))
* Fixed a bug in OpenAPI conversions where tags were not passed through unless set in the `supportedTags` list, even
  when the `tags` setting was enabled. ([#570](https://github.com/awslabs/smithy/pull/570))

## 1.1.0 (2020-09-16)

### Features

* Added the `removeTraitDefinitions` build transform to remove trait definitions from models but leave instances intact.
  ([#558](https://github.com/awslabs/smithy/pull/558))
* Added payload binding validation to HTTP `DELETE` operations. ([#566](https://github.com/awslabs/smithy/pull/566))
* Updated `SmithyDiff` to emit events when traits are changed. ([#561](https://github.com/awslabs/smithy/pull/561))

### Bug Fixes

* Fixed an issue where some `StringListTrait` instances could lose `SourceLocation` information. ([#564](https://github.com/awslabs/smithy/pull/564))
* Fixed some issues in protocol tests. ([#560](https://github.com/awslabs/smithy/pull/560), [#563](https://github.com/awslabs/smithy/pull/563))

### Other

* Model components are now deduplicated based on location and value. ([#565](https://github.com/awslabs/smithy/pull/565))
* Normalize URL import filenames for better deduplication and reporting. ([#562](https://github.com/awslabs/smithy/pull/562))

## 1.0.11 (2020-09-10)

### Features

* Added a reverse-topological knowledge index to aid in code generation for languages that require types to be
  defined before they are referenced. ([#545](https://github.com/awslabs/smithy/pull/545), [#53](https://github.com/awslabs/smithy/pull/553))
* Added the `@httpResponseCode` trait to indicate that a structure member represents an HTTP response status code. ([#546](https://github.com/awslabs/smithy/pull/546))
* Added (unstable) support for generating a "Trace File" to link code generated artifacts back to their modeled source.
  ([#552](https://github.com/awslabs/smithy/pull/552))
* Added the `:topdown` selector that matches shapes hierarchically. ([#539](https://github.com/awslabs/smithy/pull/539))
* Added validation for the `cloudTrailEventSource` property of the `@aws.api#service` trait. ([#550](https://github.com/awslabs/smithy/pull/550))
* Updated shape builders to properly update their member ShapeIds if the ShapeId of the builder changes. ([#556](https://github.com/awslabs/smithy/pull/556))
* Added several more XML related protocol tests. ([#547](https://github.com/awslabs/smithy/pull/547))

### Bug Fixes

* Fixed a bug where the `PaginatedIndex` did not properly support resolving paths. ([#554](https://github.com/awslabs/smithy/pull/554))

### Documentation

* Clarified the documentation for the `cloudTrailEventSource` property of the `@aws.api#service` trait. ([#550](https://github.com/awslabs/smithy/pull/550))
* Clarified that the `@aws.api#arn` trait has no impact on OpenAPI conversions. ([#555](https://github.com/awslabs/smithy/pull/555))

## 1.0.10 (2020-08-26)

### Features

* Added a validation event when a syntactic shape ID is found that does not target an actual shape in the model.
  ([#542](https://github.com/awslabs/smithy/pull/542))

### Bug Fixes

* Fixed a bug where forward reference resolution would use the incorrect namespace when resolving operation and
  resource bindings. ([#543](https://github.com/awslabs/smithy/pull/543))

### Other

* Deprecated the reflection-based creation pattern for `KnowledgeIndex` implementations. ([#541](https://github.com/awslabs/smithy/pull/541))

## 1.0.9 (2020-08-21)

### Features

* Allow conflicting shape definitions if the fully built shapes are equivalent. ([#520](https://github.com/awslabs/smithy/pull/520))
* Added the `@internal` trait to the prelude. ([#531](https://github.com/awslabs/smithy/pull/531))
* Added the `excludeShapesByTrait` build transform that will remove any shapes marked with one or more of the
  specified traits. ([#531](https://github.com/awslabs/smithy/pull/531))
* Improved support for newlines and indentation in `CodeWriter`. ([#529](https://github.com/awslabs/smithy/pull/529))
* Added support for configuring the expression starting character in `CodeWriter`. ([#529](https://github.com/awslabs/smithy/pull/529))
* Added `payloadFormatVersion` property for API Gateway integrations. ([#527](https://github.com/awslabs/smithy/pull/527))
* Add `deprecated` property to operations when converting to OpenAPI. ([#535](https://github.com/awslabs/smithy/pull/535))
* Added several more protocol tests. ([#528](https://github.com/awslabs/smithy/pull/528), [#536](https://github.com/awslabs/smithy/pull/536)) 

### Bug Fixes

* Fixed the selector for the `@httpQuery` trait. ([#534](https://github.com/awslabs/smithy/pull/534))
* Fixed the selector for the `@httpPrefixHeaders` trait. ([#533](https://github.com/awslabs/smithy/pull/533))
* Fixed some issues in protocol tests. ([#526](https://github.com/awslabs/smithy/pull/526))

### Other

* Removed the `abbreviation` property from the `@aws.api#service` trait. ([#532](https://github.com/awslabs/smithy/pull/532))
* Simplified prelude model loading. ([#524](https://github.com/awslabs/smithy/pull/524))
* Further simplified overall model loading. ([#525](https://github.com/awslabs/smithy/pull/525))

## 1.0.8 (2020-07-31)

### Features

* Updated `Walker` to provide a stable sort for shapes. ([#511](https://github.com/awslabs/smithy/pull/511))
* Improved support for loading `ValidationEvent`s via `NodeMapper`. ([#518](https://github.com/awslabs/smithy/pull/518),
  [#516](https://github.com/awslabs/smithy/pull/516))
* Added the ability to `disableFromNode` via `NodeMapper`. ([#505](https://github.com/awslabs/smithy/pull/505))

### Bug Fixes

* Fixed several issues in protocol tests. ([#502](https://github.com/awslabs/smithy/pull/502), [#507](https://github.com/awslabs/smithy/pull/507))

### Other

* Stopped raising validation errors and running validation with `RenameShapes` transformer. ([#512](https://github.com/awslabs/smithy/pull/512))
* Simplified conflict handling for shapes. ([#514](https://github.com/awslabs/smithy/pull/514))
* Simplified duplicate member detection and handling. ([#513](https://github.com/awslabs/smithy/pull/513))

## 1.0.7 (2020-07-16)

### Features

* Use the `@title` trait to improve generated documentation for JSON Schema unions that use `"oneOf"`. ([#485](https://github.com/awslabs/smithy/pull/485))
* Added and updated several protocol tests for `restJson1`. ([#490](https://github.com/awslabs/smithy/pull/490))
* Added and updated several protocol tests for `awsJson1_1`. ([#484](https://github.com/awslabs/smithy/pull/484), [#493](https://github.com/awslabs/smithy/pull/493))
* Added protocol tests for `awsJson1_0`. ([#496](https://github.com/awslabs/smithy/pull/496))

### Bug Fixes

* Fixed a bug where `passthroughBehavior` was misspelled as `passThroughBehavior`
  in APIGateway OpenAPI output for integrations and mock integrations. ([#495](https://github.com/awslabs/smithy/pull/495))
* Fixed a bug where only the last line in a multiline doc comment on a
  member would be successfully parsed. ([#498](https://github.com/awslabs/smithy/pull/498))
* Fixed several issues in protocol tests. ([#473](https://github.com/awslabs/smithy/pull/473), [#476](https://github.com/awslabs/smithy/pull/476),
  [#481](https://github.com/awslabs/smithy/pull/481), [#491](https://github.com/awslabs/smithy/pull/491))

### Documentation

* Refactored the specification to better explain the semantic model and its representations. ([#497](https://github.com/awslabs/smithy/pull/497),
  [#482](https://github.com/awslabs/smithy/pull/482))
* Clarified guidance on using `@mediaType`. ([#500](https://github.com/awslabs/smithy/pull/500))
* Removed outdated namespace guidance. ([#487](https://github.com/awslabs/smithy/pull/487))
* Fixed several minor issues. ([#494](https://github.com/awslabs/smithy/pull/494))

### Other

* Disallowed problematic identifiers misusing `_`. ([#499](https://github.com/awslabs/smithy/pull/499))
* Moved validation of members with the `@httpLabel` trait being marked required to the selector. ([#480](https://github.com/awslabs/smithy/pull/480))

## 1.0.6 (2020-06-24)

### Features

* Update `structure` and `union` shapes so member order is maintained as part of the contract. ([#465](https://github.com/awslabs/smithy/pull/465))
* Add validation for `document` types in protocols. ([#474](https://github.com/awslabs/smithy/pull/474))
* Provide suggestions for invalid Shape ID targets if a close match is found. ([#466](https://github.com/awslabs/smithy/pull/466))
* Added message templates and trait binding to the `EmitEachSelector`. ([#467](https://github.com/awslabs/smithy/pull/467))
* Added ability to add traits directly to the `ModelAssembler`. ([#470](https://github.com/awslabs/smithy/pull/470))
* Convert `awsJson1_1` protocol tests to Smithy IDL. ([#472](https://github.com/awslabs/smithy/pull/472))
* Update decimal values in protocol tests. ([#471](https://github.com/awslabs/smithy/pull/471)) 

### Documentation

* Update quick start guide with more examples. ([#462](https://github.com/awslabs/smithy/pull/462))

### Bug Fixes

* Fixed issues allowing `document` types in `@httpHeader` and `@httpPrefixHeaders` traits. ([#474](https://github.com/awslabs/smithy/pull/474))

## 1.0.5 (2020-06-05)

### Bug Fixes

* Fixed a bug in loading IDL files where resolving a forward reference that needed another
  forward reference would throw an exception. ([#458](https://github.com/awslabs/smithy/pull/458))
* Fixed a bug where smithy-build imports were not resolved relative to their `smithy-build.json`. ([#457](https://github.com/awslabs/smithy/pull/457))  
* Fixed a bug where the `PREFIX_HEADERS` HTTP binding location would not default its timestamp
  format to `HTTP_DATE`. ([#456](https://github.com/awslabs/smithy/pull/456))

## 1.0.4 (2020-05-29)

### Features

* Ensure that when a property is removed from a JSON schema object, that a corresponding "required"
  entry is also removed. ([#452](https://github.com/awslabs/smithy/pull/452))
* Added the (unstable) `@httpChecksumRequired` trait to indicate an operation requires a checksum
  in its HTTP request. ([#433](https://github.com/awslabs/smithy/pull/433), [#453](https://github.com/awslabs/smithy/pull/453))

### Bug Fixes

* Fixed a bug in OpenApi conversion where removing authentication for an operation would result
  in the operation inheriting the global "security" configuration instead of having it set to none. ([#451](https://github.com/awslabs/smithy/pull/451/))

### Documentation

* Added examples of building models to various guides. ([#449](https://github.com/awslabs/smithy/pull/449))
* Fixed various documentation issues. ([#449](https://github.com/awslabs/smithy/pull/449))

## 1.0.3 (2020-05-26)

### Bug Fixes

* Prevent parsing overly deep Node values ([#442](https://github.com/awslabs/smithy/pull/442))
* Fix an issue with the OpenAPI conversion where synthesized structure inputs reference required properties that
  were removed. ([#443](https://github.com/awslabs/smithy/pull/443))

## 1.0.2 (2020-05-18)

### Bug Fixes

* Fix an issue that would squash exceptions thrown for invalid suppressions. ([#440](https://github.com/awslabs/smithy/pull/440))

## 1.0.1 (2020-05-13)

### Features

* The `smithy.api#httpPayload` trait can now target document shapes. ([#431](https://github.com/awslabs/smithy/pull/431))
* Updated the IDL grammar to include many previously enforced parsing rules. ([#434](https://github.com/awslabs/smithy/pull/434))
* Added the `select` command to the CLI to print out shapes from a model that match a selector. ([#430](https://github.com/awslabs/smithy/pull/430))
* Added the `ast` command to the CLI to convert 0 or more Smithy models into a JSON AST. ([#435](https://github.com/awslabs/smithy/pull/435))
* Added a Dockerfile for building Smithy as a Docker image. ([#427](https://github.com/awslabs/smithy/pull/427))

### Bug Fixes

* Fix several ambiguities and issues in the IDL grammar. ([#434](https://github.com/awslabs/smithy/pull/434))
* JSON pretty printing of the AST now uses 4 spaces for indentation. ([#435](https://github.com/awslabs/smithy/pull/435))
* Fix CLI `--help` output alignment. ([#429](https://github.com/awslabs/smithy/pull/429))

### Other

* The Smithy IDL parser has been rewritten and optimized. ([#434](https://github.com/awslabs/smithy/pull/434))
* Generate a class data share to speed up the CLI. ([#429](https://github.com/awslabs/smithy/pull/429))

## 1.0.0 (2020-05-04)

***Note***: Changes marked with "[BC]" are breaking changes more accurately described in the
specific section. A list of further intended breaking changes have a specific section near
the end of this entry.

### Features

#### General

* The model format version has been updated to `1.0` and contains several updates: [BC] ([#357](https://github.com/awslabs/smithy/pull/357),
  [#381](https://github.com/awslabs/smithy/pull/381))
  * The JSON AST representation requires describing annotation traits as `{}` instead of `true`.
  * Annotation traits in the IDL are now provided as `@foo` or `@foo()`. Explicit `@foo(true)` and
  `@foo(null)` support was removed.
* Smithy models can now be serialized to the IDL. ([#284](https://github.com/awslabs/smithy/pull/284))
* Added a Node-based object mapper to simplify the process of building and using Java components
from Smithy `Node`s. ([#301](https://github.com/awslabs/smithy/pull/301))
  * Many packages have seen significant updates to use this functionality. ([#305](https://github.com/awslabs/smithy/pull/305),
    [#364](https://github.com/awslabs/smithy/pull/364))
* Made error messages clearer when encountering duplicate shapes. ([#324](https://github.com/awslabs/smithy/pull/324))
* Model loaders now warn on additional shape properties instead of fail. ([#374](https://github.com/awslabs/smithy/pull/374))
* Added expect* methods to the base `Shape`. ([#314](https://github.com/awslabs/smithy/pull/314))
* Added `@SmithyUnstableApi`, `@SmithyInternalApi` and `@SmithyGenerated` Java annotations. ([#297](https://github.com/awslabs/smithy/pull/297))
* `NodeValidationVisitor`s are marked as internal and/or unstable. ([#375](https://github.com/awslabs/smithy/pull/375))
* The `$version` control statement can now be set to only a major version (e.g., "1") to indicate that an
  implementation must support a version >= 1 and < 2. `$version` can now be set to `major.minor` (e.g., "1.1")
  to indicate that an implementation must support a version >= 1.1 and < 2.

#### Trait updates

* Individual protocols are now defined as individual traits that are annotated with
[the `protocolDefinition` trait.](https://smithy.io/2.0/spec/protocol-traits.html#protocoldefinition-trait) [BC]
  ([#273](https://github.com/awslabs/smithy/pull/273), [#280](https://github.com/awslabs/smithy/pull/280), [#379](https://github.com/awslabs/smithy/pull/379),
    [#390](https://github.com/awslabs/smithy/pull/390))
  * Previously listed [AWS protocols now have trait implementations.](https://smithy.io/2.0/aws/index.html#aws-protocols)
* Individual authentication schemes are now defined as individual traits that are annotated with
[the `authDefinition` trait.](https://smithy.io/2.0/spec/authentication-traits.html#authdefinition-trait) [BC]
  ([#273](https://github.com/awslabs/smithy/pull/273), [#280](https://github.com/awslabs/smithy/pull/280))
  * Previously listed [authentication schemes now have trait implementations.](https://smithy.io/2.0/spec/authentication-traits.html)
* The `smithy.api#enum` trait is now a list of enum definitions instead of a map of string keys to
enum definitions to improve clarity and encourage adding more properties to definitions. [BC] ([#326](https://github.com/awslabs/smithy/pull/326))
* The `aws.api#streaming` trait is now applied to shapes directly instead of members. [BC] ([#340](https://github.com/awslabs/smithy/pull/340))
* The `smithy.api#eventStream` trait has been removed. Event streams are now indicated by applying
the `smithy.api#streaming` trait to unions. [BC] ([#365](https://github.com/awslabs/smithy/pull/365))
* The `smithy.api#requiresLength` trait has been split out of the `smithy.api#streaming` trait to
improve clarity around event stream modeling. [BC] ([#368](https://github.com/awslabs/smithy/pull/368))
* The `smithy.api#externalDocumentation` trait is now a map instead of a single string to allow for
multiple links per trait. [BC] ([#363](https://github.com/awslabs/smithy/pull/363))
* Added the `smithy.api#noReplace` trait to indicate a PUT lifecycle operation cannot replace the
existing resource. ([#351](https://github.com/awslabs/smithy/pull/351))
* Added the `smithy.api#unstable` trait to indicate a shape MAY change. ([#290](https://github.com/awslabs/smithy/pull/290))
* Simplified `aws.api#unsignedPayload` to be an annotation. [BC] ([#270](https://github.com/awslabs/smithy/pull/270))
* Annotation traits are now lossless when loaded with additional properties, meaning they will
contain those properties when serialized. ([#385](https://github.com/awslabs/smithy/pull/385))

#### Selector updates

Selectors have received significant updates: ([#388](https://github.com/awslabs/smithy/pull/388))

* Attribute selectors can now evaluate scoped comparisons using `@foo:` to define a scope
and `@{bar}` to define a context value. ([#391](https://github.com/awslabs/smithy/pull/391))
* And logic, via `&&`, has been added to allow multiple attribute comparisons. ([#391](https://github.com/awslabs/smithy/pull/391))
* Support for selecting nested trait properties with `|`, including list/object values and object
keys, was added.
* An opt-in `trait` relationship has been added. ([#384](https://github.com/awslabs/smithy/pull/384))
* The recursive neighbor selector, `~>`, has been added. ([#386](https://github.com/awslabs/smithy/pull/386))
* A not equal comparison, `!=`, was added.
* An exists comparison, `?=`, was added. ([#391](https://github.com/awslabs/smithy/pull/391))
* Support for numbers in attribute selectors was added.
* Numeric comparisons (`>`, `>=`, `<`, `<=`) were added.
* The `(length)` function property was added. ([#391](https://github.com/awslabs/smithy/pull/391))
* Attribute selectors now support CSV values, allowing matching on one or more target values.
* The `:each` selector is now `:is` for clarity. [BC]
* The `:of` selector is now removed. Use reverse neighbors instead (e.g., `member :test(< structure)`). [BC]
* The semantics of the `:not` selector have changed significantly. `:not(list > member > string)` now means
  "do not match list shapes that target strings", whereas this previously meant,
  "do not match string shapes targeted by lists". [BC]
* Shape IDs with members must now be quoted. [BC]
* Selector parsing and evaluating now tolerates unknown relationship types. ([#377](https://github.com/awslabs/smithy/pull/377))

#### Validation updates

* Services must now contain a closure of shapes that have case-insensitively unique names. [BC] ([#337](https://github.com/awslabs/smithy/pull/337))
* `suppressions` has been updated to now only suppress validation events that occur for an entire namespace or across
  the entire model. The `@suppress` trait was added to suppress validation events for a specific shape. [BC] ([#397](https://github.com/awslabs/smithy/pull/397)).
* The `UnreferencedShape` validator has moved to `smithy-model` and is now always run. [BC] ([#319](https://github.com/awslabs/smithy/pull/319))
* `EmitEachSelector` and `EmitNoneSelector` were moved from `smithy-linters` into `smithy-model`.

#### JSON Schema conversion

The conversion to JSON schema was significantly overhauled. [BC] ([#274](https://github.com/awslabs/smithy/pull/274))

* Configuration for the build plugin was significantly overhauled. [BC] ([#364](https://github.com/awslabs/smithy/pull/364))
* The strategy for shape inlining has been changed. [BC]
* The strategy for naming shapes and handling shape id conflicts has been changed. [BC]
* Output schema error detection was improved.
* The Java API surface has been reduced. [BC]
* Added the ability to select schemas from a document using a JSON pointer.

#### OpenAPI conversion

The conversion to OpenAPI was significantly overhauled. [BC] ([#275](https://github.com/awslabs/smithy/pull/275))

* Configuration for the build plugin was significantly overhauled.  [BC] ([#364](https://github.com/awslabs/smithy/pull/364))
* Protocol conversion was updated to utilize the new traits. ([#275](https://github.com/awslabs/smithy/pull/275), [#392](https://github.com/awslabs/smithy/pull/392))
* Schemas are now generated for requests and responses instead of being inlined. [BC]
* Fixed several issues with CORS integrations.

#### API Gateway OpenAPI conversion

The API Gateway specific OpenAPI mappers have been updated. [BC] ([#367](https://github.com/awslabs/smithy/pull/367))

*  The `ApiGatewayMapper` interface was added, allowing mappers to control which API Gateway API
type(s) they support.
* Fixed several issues with CORS integrations. ([#370](https://github.com/awslabs/smithy/pull/370))
* Added support for JSON Patch-like OpenAPI schema changes based on JSON Pointers. ([#293](https://github.com/awslabs/smithy/pull/293))
* Added support for event streams in OpenAPI conversion. ([#334](https://github.com/awslabs/smithy/pull/334))

### Bug Fixes

* Fixed an issue in JSON schema conversion where member traits were dropped in some scenarios. ([#274](https://github.com/awslabs/smithy/pull/274))
* Fixed an issue where authorization headers were not properly added to CORS configurations. ([#328](https://github.com/awslabs/smithy/pull/328))
* Fixed an issue where operation response headers were being applied to error responses in OpenAPI
conversions. ([#275](https://github.com/awslabs/smithy/pull/275))
* Fixed an issue where `apply` statements wouldn't resolve target shapes properly in some cases. ([#287](https://github.com/awslabs/smithy/pull/287))
* Fixed an issue with the selector for the `smithy.api#title` trait. ([#387](https://github.com/awslabs/smithy/pull/387))
* Fixed several issues with the `smithy.api#httpApiKeyAuth` trait and its related conversions. ([#291](https://github.com/awslabs/smithy/pull/291))
* Fixed a bug with timestamp validation in specific versions of Java. ([#316](https://github.com/awslabs/smithy/pull/316))

### Optimizations

* The `TraitTargetValidator` now performs as few checks on and selections of the entire model. ([#389](https://github.com/awslabs/smithy/pull/389))
* The dependency on `jackson-core` was replaced with a vendored version of `minimal-json` to reduce
the chances of dependency conflicts. [BC] ([#323](https://github.com/awslabs/smithy/pull/323))
* Sped up model loading time by loading JSON models before IDL models to reduce forward
reference lookups. ([#287](https://github.com/awslabs/smithy/pull/287))

### Breaking changes

* The `BooleanTrait` abstract class in `smithy-model` was renamed `AnnotationTrait`. ([#381](https://github.com/awslabs/smithy/pull/381))
* The traits in the `aws.apigateway` namespace have moved from `smithy-aws-traits` to the
`smithy-aws-apigateway-traits` package for more granular use. ([#322](https://github.com/awslabs/smithy/pull/322))
  * Tooling that referenced these traits has also been updated.
* The traits in the `aws.iam` namespace have moved from `smithy-aws-traits` to the
`smithy-aws-iam-traits` package for more granular use. ([#322](https://github.com/awslabs/smithy/pull/322))
  * Tooling that referenced these traits has also been updated.
* The `aws.api#ec2QueryName` trait has moved to `aws.protocols#ec2QueryName`. ([#286](https://github.com/awslabs/smithy/pull/286))
* The `aws.api#unsignedPayload ` trait has moved to `aws.auth#unsignedPayload `. ([#286](https://github.com/awslabs/smithy/pull/286))
* The `smithy-codegen-freemarker` package has been removed. ([#382](https://github.com/awslabs/smithy/pull/382))
* Traits can no longer be applied to public Smithy Prelude shapes. ([#317](https://github.com/awslabs/smithy/pull/317))
* Smithy's `Pattern` class is renamed to `SmithyPattern` to remove the conflict with Java's regex
`Pattern` class. ([#315](https://github.com/awslabs/smithy/pull/315))
* Removed the `Triple` class from `smithy-utils`. ([#313](https://github.com/awslabs/smithy/pull/313))
* Normalized class names for OpenAPI `SecurityScemeConverter` implementations. ([#291](https://github.com/awslabs/smithy/pull/291))
* Removed alias functionality from `software.amazon.smithy.build.SmithyBuildPlugin` and
  `software.amazon.smithy.build.ProjectionTransformer`. ([#409](https://github.com/awslabs/smithy/pull/409))
* Removed `software.amazon.smithy.model.shapes.Shape#visitor` and
  `software.amazon.smithy.model.shapes.ShapeVisitor$Builder`. Use
  `software.amazon.smithy.model.shapes.ShapeVisitor$Default` instead. ([#413](https://github.com/awslabs/smithy/pull/413))
* `software.amazon.smithy.model.Model#getTraitDefinitions` and `getTraitShapes` were removed in favor of
  `software.amazon.smithy.model.Model#getShapesWithTrait`. ([#412](https://github.com/awslabs/smithy/pull/412))

#### Deprecation cleanup

* The deprecated IDL operation syntax has been removed ([#373](https://github.com/awslabs/smithy/pull/373))
* The deprecated `NodeFactory` interface has been removed. ([#265](https://github.com/awslabs/smithy/pull/265))
* The deprecated `ShapeIndex` class and all related APIs have been removed. ([#266](https://github.com/awslabs/smithy/pull/266))
* Support for the deprecated `0.4.0` model version has been removed. ([#267](https://github.com/awslabs/smithy/pull/267))
* The `aws.api#service` trait no longer supports the deprecated
`sdkServiceId`, `arnService`, or `productName` properties. ([#268](https://github.com/awslabs/smithy/pull/268))
* The deprecated `TemplateEngine` and `DefaultDataTemplateEngine` have been removed. ([#268](https://github.com/awslabs/smithy/pull/268))
* The deprecated `smithy.validators` and `smithy.suppressions` are no longer used as aliases for
validators and suppressions. ([#268](https://github.com/awslabs/smithy/pull/268))
* The `smithy.api#references` and `smithy.api#idRef` traits no longer support relative shape IDs. ([#268](https://github.com/awslabs/smithy/pull/268))

### Documentation

A significant overhaul of the specification and guides has been completed. This includes a better
flow to the spec, more complete guides, deeper documentation of AWS specific components, and a
complete redesign. Many direct links to components of the documentation will have changed.

## 0.9.9 (2020-04-01)

### Bug Fixes

* Add security to individual operations in OpenAPI conversion ([#329](https://github.com/awslabs/smithy/pull/329))
* Fix an issue with header casing for `x-api-key` integration with API Gateway ([#330](https://github.com/awslabs/smithy/pull/330))
* Fix discrepancies in `smithy-aws-protocol-tests` ([#333](https://github.com/awslabs/smithy/pull/333), [#335](https://github.com/awslabs/smithy/pull/335),
  [#349](https://github.com/awslabs/smithy/pull/349))

## 0.9.8 (2020-03-26)

### Features

* Add `RenameShapes` model transformer ([#318](https://github.com/awslabs/smithy/pull/318))
* Build `ValidationEvents` are now sorted ([#263](https://github.com/awslabs/smithy/pull/263))
* Smithy CLI logging improvements ([#263](https://github.com/awslabs/smithy/pull/263))
* Model builds fail early when syntax errors occur ([#264](https://github.com/awslabs/smithy/pull/264))
* Warn when a deprecated trait is applied to a shape ([#279](https://github.com/awslabs/smithy/pull/279))

### Bug Fixes

* Fix behavior of `schemaDocumentExtensions` when converting to OpenAPI ([#320](https://github.com/awslabs/smithy/pull/320))
* Fix discrepancies in `smithy-aws-protocol-tests` ([#309](https://github.com/awslabs/smithy/pull/309), [#321](https://github.com/awslabs/smithy/pull/321))
* Properly format test case results ([#271](https://github.com/awslabs/smithy/pull/271))
* Fix dropping one character text block lines ([#285](https://github.com/awslabs/smithy/pull/285))

### Other

* Builds run parallel projections in parallel only if there are more than one ([#263](https://github.com/awslabs/smithy/pull/263))
* Run Smithy test suites as parameterized tests ([#263](https://github.com/awslabs/smithy/pull/263))
* Migrate protocol tests to new operation syntax ([#260](https://github.com/awslabs/smithy/pull/260))
* Build protocol tests with the Smithy Gradle plugin ([#263](https://github.com/awslabs/smithy/pull/263))
* Deprecate using explicitly `smithy.api` for trait removal ([#306](https://github.com/awslabs/smithy/pull/306))

## 0.9.7 (2020-01-15)

### Features

* Updated Operation syntax in the Smithy IDL ([#253](https://github.com/awslabs/smithy/pull/253))
* Updated specification for XML traits ([#242](https://github.com/awslabs/smithy/pull/242))
* Add the `@aws.api#ec2QueryName-trait` trait ([#251](https://github.com/awslabs/smithy/pull/251))
* Add AWS protocol test models ([#246](https://github.com/awslabs/smithy/pull/246), [#247](https://github.com/awslabs/smithy/pull/247),
  [#250](https://github.com/awslabs/smithy/pull/250), [#255](https://github.com/awslabs/smithy/pull/255),
  and [#258](https://github.com/awslabs/smithy/pull/258))

### Other

* Use URLConnection cache setting in ModelAssembler ([#244](https://github.com/awslabs/smithy/pull/244))

### Bug Fixes

* Use list of string for queryParams in the `httpRequestTests` trait ([#240](https://github.com/awslabs/smithy/pull/240))

## 0.9.6 (2020-01-02)

### Features

* Allow XML maps to be flattened ([#205](https://github.com/awslabs/smithy/pull/205))
* Add and remove shape members to model automatically ([#206](https://github.com/awslabs/smithy/pull/206))
* Deprecate ShapeIndex in favor of Model ([#209](https://github.com/awslabs/smithy/pull/209))
* Allow the sensitive trait to be applied to all but operations, services, and resources ([#212](https://github.com/awslabs/smithy/pull/212))
* Added 0.5.0 IDL and AST format ([#213](https://github.com/awslabs/smithy/pull/213))
* Allow min to equal max in range trait ([#216](https://github.com/awslabs/smithy/pull/216))
* Added validation for length trait values ([#217](https://github.com/awslabs/smithy/pull/217))
* Limit streaming trait to top-level members ([#221](https://github.com/awslabs/smithy/pull/221))
* Added protocol compliance test traits ([#226](https://github.com/awslabs/smithy/pull/226))
* Added ability to configure timestamp validation ([#229](https://github.com/awslabs/smithy/pull/229))
* Moved `TemplateEngine` implementation into FreeMarker implementation ([#230](https://github.com/awslabs/smithy/pull/230))
* Added `BoxIndex` ([#234](https://github.com/awslabs/smithy/pull/234))
* Added more expect methods to `Shape` and `Model` ([#237](https://github.com/awslabs/smithy/pull/237))

### Other

* Update smithy-build to be streaming ([#211](https://github.com/awslabs/smithy/pull/211))

### Bug Fixes

* Prevent bad list, set, and map recursion ([#204](https://github.com/awslabs/smithy/pull/204))
* Properly allow omitting endpoint discovery operation inputs ([#220](https://github.com/awslabs/smithy/pull/220))

## 0.9.5 (2019-11-11)

### Features

* Allow overriding state management in CodeWriter ([#186](https://github.com/awslabs/smithy/pull/186))
* Allow the `xmlFlattened` trait to be applied to members ([#191](https://github.com/awslabs/smithy/pull/191))
* Add helper to determine HTTP-based timestamp formats ([#193](https://github.com/awslabs/smithy/pull/193))
* Allow specifying XML namespace prefixes ([#195](https://github.com/awslabs/smithy/pull/195))
* Add `SymbolContainer`, an abstraction over `Symbol`s that enables easily
  creating and aggregating `Symbols` ([#202](https://github.com/awslabs/smithy/pull/202))

### Bug Fixes

* Escape popped state content ([#187](https://github.com/awslabs/smithy/pull/187))
* Make shape ID serialization consistent ([#196](https://github.com/awslabs/smithy/pull/196))
* Exclude private members targeted in JSON schema converters ([#199](https://github.com/awslabs/smithy/pull/199))
* Fix naming collisions in JSON schema output ([#200](https://github.com/awslabs/smithy/pull/200))
* Update `equals` to included typed bag parents ([#201](https://github.com/awslabs/smithy/pull/201))

## 0.9.4 (2019-10-09)

### Features

* Add support for AWS Client Endpoint Discovery ([#165](https://github.com/awslabs/smithy/pull/165))
* Refactor event streams to target members ([#171](https://github.com/awslabs/smithy/pull/171))
* Add support for aliasing referenced `Symbol`s ([#168](https://github.com/awslabs/smithy/pull/168))
* Add support for `Symbol`s to introduce dependencies ([#169](https://github.com/awslabs/smithy/pull/169))
* Add ability to manually escape reserved words in `ReservedWordSymbolProvider` ([#174](https://github.com/awslabs/smithy/pull/174))
* Add method to gather dependencies for `Symbol`s ([#170](https://github.com/awslabs/smithy/pull/170))
* Add a caching `SymbolProvider` ([#167](https://github.com/awslabs/smithy/pull/167))
* Improve the usability of `CodeWroter#openBlock` ([#175](https://github.com/awslabs/smithy/pull/175))
* Improve the usability of `PluginContext` ([#181](https://github.com/awslabs/smithy/pull/181))

### Other

* Disable URLConnection cache in CLI ([#180](https://github.com/awslabs/smithy/pull/180))

### Bug Fixes

* Fix issue with generated authentication for CORS checks ([#179](https://github.com/awslabs/smithy/pull/179))
* Set the `defaultTimestampFormat` to `epoch-seconds` for `aws.rest-json` protocols in OpenAPI ([#184](https://github.com/awslabs/smithy/pull/184))

## 0.9.3 (2019-09-16)

### Features

* Clean up `CodeWriter` modifiers ([#143](https://github.com/awslabs/smithy/pull/143))
* Add typed `ObjectNode` member expectation functions ([#144](https://github.com/awslabs/smithy/pull/144))
* Add `expectShapeId` for fully-qualified shape ID ([#147](https://github.com/awslabs/smithy/pull/147))
* Add helper to `EnumTrait` to check if it has names ([#148](https://github.com/awslabs/smithy/pull/148))
* Add `Symbol` references ([#149](https://github.com/awslabs/smithy/pull/149))
* Add `ReservedWords` builder for simpler construction ([#150](https://github.com/awslabs/smithy/pull/150))
* Allow using path expressions in paginator outputs ([#152](https://github.com/awslabs/smithy/pull/152))
* Add method to get non-trait shapes ([#153](https://github.com/awslabs/smithy/pull/153))
* Add method to write class resource to manifest ([#157](https://github.com/awslabs/smithy/pull/157))
* Allow `authType` to be specified ([#160](https://github.com/awslabs/smithy/pull/160))


### Bug Fixes

* Fix collection and gradle doc issues ([#145](https://github.com/awslabs/smithy/pull/145))
* Make `AuthorizerDefinition` definition private ([#146](https://github.com/awslabs/smithy/pull/146))
* Fix put handling on `ResourceShape` ([#158](https://github.com/awslabs/smithy/pull/158))
* Fix parse error when `apply` is at eof ([#159](https://github.com/awslabs/smithy/pull/159))
* Prevent `list`/`set` member from targeting container ([#162](https://github.com/awslabs/smithy/pull/162))
* Allow model assembling from symlink model files / directory ([#163](https://github.com/awslabs/smithy/pull/163))
