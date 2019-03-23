// Parse error at line 7, column 3 near `fuzz`: Invalid member `fuzz` found in map shape `com.foo#MyMap`. Expected one of the following members: [`key`, `value`]
namespace com.foo

map MyMap {
  key: smithy.api#String,
  value: smithy.api#String,
  fuzz: smithy.api#String,
}
