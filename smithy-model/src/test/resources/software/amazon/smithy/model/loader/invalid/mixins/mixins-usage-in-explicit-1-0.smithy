// Syntax error at line 5, column 15: Mixins can only be used with Smithy version 2 or later. Attempted to use mixins with version `1.0`. | Model
$version: "1.0"
namespace smithy.example

structure Foo with [Bar] {}
