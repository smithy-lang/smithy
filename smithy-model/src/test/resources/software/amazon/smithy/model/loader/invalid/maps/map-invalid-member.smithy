// Syntax error at line 7, column 3: Expected RBRACE('}') but found IDENTIFIER('fuzz') | Model
namespace com.foo

map MyMap {
  key: smithy.api#String,
  value: smithy.api#String,
  fuzz: smithy.api#String,
}
