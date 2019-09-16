# Smithy Changelog

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
