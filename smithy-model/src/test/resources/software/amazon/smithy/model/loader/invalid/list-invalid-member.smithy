// Parse error at line 5, column 3 near `foo`: Invalid member `foo` found in list shape `com.foo#MyList`. Expected one of the following members: [`member`]
namespace com.foo

list MyList {
  foo: smithy.api#String,
}
