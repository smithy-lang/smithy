# Smithy Changelog

## 1.0.0 (2020-??-??)

***Note***: Changes marked with "[BC]" are breaking changes more accurately described in the
specific section. A list of further intended breaking changes have a specific section near
the end of this entry.

### Features

#### General

* The model format version has beeen updated to `1.0.0` and contains several updates: [BC] ([#357](https://github.com/awslabs/smithy/pull/357), [#381](https://github.com/awslabs/smithy/pull/381))
  * The JSON AST representation requires describing annotation traits as `{}` instead of `true`.
  * Annotation traits in the IDL are now provided as `@foo` or `@foo()`. Explicit `@foo(true)` and
  `@foo(null)` support was removed.
* Smithy models can now be serialized to the IDL. ([#284](https://github.com/awslabs/smithy/pull/284))
* Added a Node-based object mapper to simplify the process of building and using Java components
from Smithy `Node`s. ([#301](https://github.com/awslabs/smithy/pull/301))
  * Many packages have seen significant updates to use this functionality. ([#305](https://github.com/awslabs/smithy/pull/305), [#364](https://github.com/awslabs/smithy/pull/364))
* Made error messages clearer when encountering duplicate shapes. ([#324](https://github.com/awslabs/smithy/pull/324))
* Model loaders now warn on additional shape properties instead of fail. ([#374](https://github.com/awslabs/smithy/pull/374))
* Added expect* methods to the base `Shape`. ([#314](https://github.com/awslabs/smithy/pull/314))
* Added `@SmithyUnstableApi`, `@SmithyInternalApi` and `@SmithyGenerated` Java annotations. ([#297](https://github.com/awslabs/smithy/pull/297))
* `NodeValidationVisitor`s are marked as internal and/or unstable. ([#375](https://github.com/awslabs/smithy/pull/375))

#### Trait updates

* Individual protocols are now defined as individual traits that are annotated with
[the `protocolDefinition` trait.](https://awslabs.github.io/smithy/spec/core/protocol-traits.html#protocoldefinition-trait) [BC] ([#273](https://github.com/awslabs/smithy/pull/273), [#280](https://github.com/awslabs/smithy/pull/280), [#379](https://github.com/awslabs/smithy/pull/379), [#390](https://github.com/awslabs/smithy/pull/390))
  * Previously listed [AWS protocols now have trait implementations.](https://awslabs.github.io/smithy/spec/aws/index.html#aws-protocols)
* Individual authentication schemes are now defined as individual traits that are annotated with
[the `authDefinition` trait.](https://awslabs.github.io/smithy/spec/core/auth-traits.html#authdefinition-trait) [BC] ([#273](https://github.com/awslabs/smithy/pull/273), [#280](https://github.com/awslabs/smithy/pull/280))
  * Previously listed [authentication schemes now have trait implementations.](https://awslabs.github.io/smithy/spec/core/auth-traits.html)
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
* Added the `aws.protocols#httpContentMd5` trait to indicate an operation requrest requires a
sending a Content-MD5 header. ([#372](https://github.com/awslabs/smithy/pull/372))
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

#### Deprecation cleanup

* The deprecated IDL operation syntax has been removed ([#373](https://github.com/awslabs/smithy/pull/373))
* The deprecated `NodeFactory` interface has been removed. ([#265](https://github.com/awslabs/smithy/pull/265))
* The deprecated `ShapeIndex` class and all related APIs have been removed. ([#266](https://github.com/awslabs/smithy/pull/266))
* Support for the deprecated `0.4.0` model version has been removed. ([#267](https://github.com/awslabs/smithy/pull/267))
* The `aws.api#service` trait no longer supports the deprecated
`sdkServiceId`, `arnService`, or `productName` properties. ([#268](https://github.com/awslabs/smithy/pull/268))
* The deprecated `TemplateEngine` and `DefaultDataTemplateEngine` have been removed. ([#268](https://github.com/awslabs/smithy/pull/268))
* The deprecated `smithy.validators` and `smithy.suppressions` are no loner used as aliases for
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

### Optimizations

* Builds run parallel projections in parallel only if there are more than one ([#263](https://github.com/awslabs/smithy/pull/263))
* Run Smithy test suites as parameterized tests ([#263](https://github.com/awslabs/smithy/pull/263))

### Cleanup

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

### Optimizations

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

### Optimizations

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

### Optimizations

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
