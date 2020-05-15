# Smithy Changelog

## 0.9.10 (2020-05-15)

### Bug Fixes

* Fix issue where `contentHandling` was not set to `CONVERT_TO_TEXT` on CORS preflight requests ([#432](https://github.com/awslabs/smithy/pull/432))

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
