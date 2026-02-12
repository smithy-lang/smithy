package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExtension;

import java.util.ArrayList;
import java.util.List;

public class CoreExtension implements JmespathExtension {
    @Override
    public <T> List<Function<T>> getFunctions() {
        List<Function<T>> result = new ArrayList<>();

        // Builtins from the specification
        result.add(new AbsFunction<>());
        result.add(new AvgFunction<>());
        result.add(new CeilFunction<>());
        result.add(new ContainsFunction<>());
        result.add(new EndsWithFunction<>());
        result.add(new FloorFunction<>());
        result.add(new JoinFunction<>());
        result.add(new KeysFunction<>());
        result.add(new LengthFunction<>());
        result.add(new MapFunction<>());
        result.add(new MaxFunction<>());
        result.add(new MergeFunction<>());
        result.add(new MaxByFunction<>());
        result.add(new MinFunction<>());
        result.add(new MinByFunction<>());
        result.add(new NotNullFunction<>());
        result.add(new ReverseFunction<>());
        result.add(new SortFunction<>());
        result.add(new SortByFunction<>());
        result.add(new StartsWithFunction<>());
        result.add(new SumFunction<>());
        result.add(new ToArrayFunction<>());
        result.add(new ToNumberFunction<>());
        result.add(new ToStringFunction<>());
        result.add(new TypeFunction<>());
        result.add(new ValuesFunction<>());

        // TODO: Separate extension?
        result.add(new AddFunction<>());
        result.add(new AppendFunction<>());
        result.add(new ConcatFunction<>());
        result.add(new IfFunction<>());
        result.add(new FoldLeftFunction<>());
        result.add(new OneNotNullFunction<>());

        return result;
    }
}
