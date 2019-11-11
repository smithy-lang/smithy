# Smithy Changelog

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
