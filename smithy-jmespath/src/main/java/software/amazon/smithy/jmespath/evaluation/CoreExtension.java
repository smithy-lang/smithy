package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExtension;

public class CoreExtension implements JmespathExtension {
    @Override
    public <T> FunctionRegistry<T> getFunctionRegistry() {
        FunctionRegistry<T> registry = new FunctionRegistry<>();

        // Builtins from the specification
        registry.registerFunction(new AbsFunction<>());
        registry.registerFunction(new AvgFunction<>());
        registry.registerFunction(new CeilFunction<>());
        registry.registerFunction(new ContainsFunction<>());
        registry.registerFunction(new EndsWithFunction<>());
        registry.registerFunction(new FloorFunction<>());
        registry.registerFunction(new JoinFunction<>());
        registry.registerFunction(new KeysFunction<>());
        registry.registerFunction(new LengthFunction<>());
        registry.registerFunction(new MapFunction<>());
        registry.registerFunction(new MaxFunction<>());
        registry.registerFunction(new MergeFunction<>());
        registry.registerFunction(new MaxByFunction<>());
        registry.registerFunction(new MinFunction<>());
        registry.registerFunction(new MinByFunction<>());
        registry.registerFunction(new NotNullFunction<>());
        registry.registerFunction(new ReverseFunction<>());
        registry.registerFunction(new SortFunction<>());
        registry.registerFunction(new SortByFunction<>());
        registry.registerFunction(new StartsWithFunction<>());
        registry.registerFunction(new SumFunction<>());
        registry.registerFunction(new ToArrayFunction<>());
        registry.registerFunction(new ToNumberFunction<>());
        registry.registerFunction(new ToStringFunction<>());
        registry.registerFunction(new TypeFunction<>());
        registry.registerFunction(new ValuesFunction<>());

        // TODO: Separate extension?
        registry.registerFunction(new AddFunction<>());
        registry.registerFunction(new AppendFunction<>());
        registry.registerFunction(new ConcatFunction<>());
        registry.registerFunction(new IfFunction<>());
        registry.registerFunction(new FoldLeftFunction<>());
        registry.registerFunction(new OneNotNullFunction<>());

        return registry;
    }
}
