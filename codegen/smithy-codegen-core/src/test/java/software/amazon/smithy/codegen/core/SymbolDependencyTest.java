package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class SymbolDependencyTest {
    @Test
    public void setsDefaultTypeToEmptyString() {
        SymbolDependency dependency = SymbolDependency.builder()
                .packageName("foo")
                .version("10")
                .build();

        assertThat(dependency.getPackageName(), equalTo("foo"));
        assertThat(dependency.getVersion(), equalTo("10"));
        assertThat(dependency.getDependencyType(), equalTo(""));
    }

    @Test
    public void convertsToBuilder() {
        SymbolDependency dependency = SymbolDependency.builder()
                .dependencyType("dev")
                .packageName("foo")
                .version("10.0.1")
                .build();

        assertThat(dependency.toBuilder().build(), equalTo(dependency));
    }

    @Test
    public void hasProperties() {
        SymbolDependency dependency = SymbolDependency.builder()
                .dependencyType("dev")
                .packageName("foo")
                .version("10.0.1")
                .putProperty("foo", "baz!")
                .build();

        assertThat(dependency.expectProperty("foo", String.class), equalTo("baz!"));
    }
}
