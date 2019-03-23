// Parse error at line 5, column 3 near `foo`: Invalid member `foo` found in set shape `com.foo#MySet`. Expected one of the following members: [`member`]
namespace com.foo

set MySet {
  foo: smithy.api#String,
}
