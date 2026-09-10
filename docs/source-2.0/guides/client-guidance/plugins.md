(client-guidance-plugins)=
# Client Plugins

A **client plugin** packages reusable changes to client configuration. Plugins
can configure protocols, transports, endpoint resolvers, authentication, retry
behavior, [interceptors](interceptors.md), and other client components. They can
also compose other plugins.

Plugins are applied while resolving client configuration rather than at a fixed
stage of request execution. An interceptor is the appropriate extension point
for observing or modifying a particular execution stage. A plugin is the
appropriate extension point for installing and configuring one or more client
features. Plugins commonly register interceptors as part of their
configuration.

## Application model

Clients should support plugins from the following sources:

1. Framework plugins applied by default.
2. Plugins explicitly configured by the user.
3. Service-specific or generated default plugins.
4. Plugins configured for a single operation call.

The first three sources are resolved while constructing a client configuration.
Call-scoped plugins are resolved later from the already configured client. This
allows a call-scoped plugin to replace a protocol, transport, endpoint resolver,
or other dependency for one call without changing later calls.

Plugin registration and plugin application are separate operations.
Registration establishes the set of plugins and their insertion order.
Resolution filters, deduplicates, and orders that set. Application then invokes
each resolved plugin against the current configuration.

Client configurations should retain the plugin registration and resolution
state used to create them. This allows a derived configuration or call-scoped
configuration to apply only newly introduced plugins.

## Interface

A plugin needs three capabilities:

- Configure the client.
- Contribute child plugins.
- Select an application phase.

The following Java-like example returns a configuration to make the sequencing
of immutable configurations explicit:

```java
public interface ClientPlugin {
    ClientConfig configureClient(ClientConfig config);

    default List<ClientPlugin> getChildPlugins() {
        return List.of();
    }

    default PluginPhase getPluginPhase() {
        return PluginPhase.APPLY;
    }
}
```

A language may instead provide a mutable configuration view during client
construction and allow `configureClient` to return nothing. The important
requirement is that each plugin receives the configuration produced by the
previous plugin.

Plugin interfaces should provide defaults for child plugins and the application
phase. Languages without default interface methods can provide an abstract base
class with equivalent behavior.

### Configuring a client

Plugins should be able to configure the same components that users can configure
directly. A plugin might:

- Register an interceptor.
- Set or decorate a protocol or transport.
- Add an authentication scheme or identity resolver.
- Replace an endpoint resolver.
- Set retry behavior.
- Add typed [context](context.md) values.
- Register another reusable client component.

A plugin should configure the client and return. It should not execute an
operation or retain call-specific mutable state.

```java
public final class TracingPlugin implements ClientPlugin {
    @Override
    public ClientConfig configureClient(ClientConfig config) {
        return config.withInterceptor(new TracingInterceptor());
    }
}
```

## Child plugins

A plugin can contribute other plugins through `getChildPlugins`. Child plugins
allow one feature to aggregate reusable components while preserving the normal
resolution behavior of each component.

```java
public final class ObservabilityPlugin implements ClientPlugin {
    @Override
    public ClientConfig configureClient(ClientConfig config) {
        return config;
    }

    @Override
    public List<ClientPlugin> getChildPlugins() {
        return List.of(
            new TracingPlugin(),
            new MetricsPlugin()
        );
    }
}
```

Child plugins:

- Are visited only if their parent is accepted.
- Are independently evaluated by the plugin predicate.
- Use their own application phases.
- Are deduplicated with top-level and other child plugins.
- Can contribute children of their own.

Plugins should contribute child plugins instead of invoking another plugin's
configuration method directly. Direct invocation bypasses phase ordering,
predicates, deduplication, and persisted resolution state.

Implementations should mark an accepted plugin as applied before visiting its
children. In addition to enforcing first-registration-wins behavior, this causes
cycles in a child-plugin graph to terminate through normal deduplication.

## Ordering

Plugins should be ordered by application phase, then by insertion order within
the same phase. The phase sort must be stable.

The recommended phases are:

```java
public enum PluginPhase {
    FIRST,
    BEFORE_DEFAULTS,
    DEFAULTS,
    AFTER_DEFAULTS,
    BEFORE_APPLY,
    APPLY,
    AFTER_APPLY,
    LAST
}
```

| Phase | Intended use |
|---|---|
| `FIRST` | Framework setup that must occur before other plugins. |
| `BEFORE_DEFAULTS` | Validation or configuration that precedes defaults. |
| `DEFAULTS` | Applying default values or components. |
| `AFTER_DEFAULTS` | Validation or adjustment after defaults. |
| `BEFORE_APPLY` | Preparation for general configuration changes. |
| `APPLY` | General configuration changes and the default plugin phase. |
| `AFTER_APPLY` | Validation or adjustment after general changes. |
| `LAST` | Framework finalization after all other phases. |

`FIRST` and `LAST` should generally be reserved for framework-level behavior.
Most plugins should use `DEFAULTS`, `APPLY`, or one of their adjacent phases.

The recommended top-level insertion order is:

1. The framework default plugin.
2. User-configured plugins.
3. Service-specific or generated default plugins.

Automatic plugins should be children of the framework default plugin. They are
therefore encountered before user plugins and generated defaults. Generated
defaults are registered after explicit user plugins so that a user-provided
instance wins a concrete-type duplicate. Application phases can move plugins
relative to one another, but insertion order remains the tie breaker within a
phase.

Call-scoped plugins are resolved after construction plugins have already been
applied. Their phases order them relative to other call-scoped plugins, but
cannot move them before a construction plugin.

## Deduplication

Each concrete plugin implementation type should be applied at most once to a
configuration. The first registered instance of a concrete type wins.
Equivalent implementation-type identity should be used in languages that do
not expose runtime classes.

Deduplication occurs while traversing registrations and children, before phase
sorting. A later instance does not replace an earlier instance even if the later
instance selects an earlier phase.

The set of applied plugin types should be stored with the client configuration.
When deriving a new configuration, previously applied types are not applied
again. Newly registered plugins remain eligible.

For example, applying `MetricsPlugin` to a base configuration and then deriving
a call configuration with another `MetricsPlugin` should not configure metrics
twice. A different `TracingPlugin` introduced by the call remains eligible.

## Predicates

Clients should support a predicate that determines whether a plugin is
accepted. Predicates allow users and framework code to disable optional
features without changing plugin registration.

The predicate is evaluated before a plugin is marked as applied:

- A rejected parent prevents traversal of all its children.
- An accepted parent's children are each evaluated independently.
- A rejected plugin is not marked as applied.

Implementations may expose operations to replace the current predicate and to
combine another predicate with it. Registered plugins that were rejected remain
eligible if a later configuration uses a predicate that accepts them and the
registration is still reachable.

## Resolution

The following pseudocode demonstrates the recommended resolution algorithm:

```java
List<ClientPlugin> resolve(
        List<ClientPlugin> registered,
        Predicate<ClientPlugin> predicate,
        Set<PluginType> applied
) {
    List<ResolvedPlugin> resolved = new ArrayList<>();
    int sequence = 0;

    for (ClientPlugin plugin : registered) {
        sequence = collect(plugin, predicate, applied, resolved, sequence);
    }

    resolved.sort(comparing(ResolvedPlugin::phase)
        .thenComparing(ResolvedPlugin::sequence));

    return resolved.stream()
        .map(ResolvedPlugin::plugin)
        .toList();
}

int collect(
        ClientPlugin plugin,
        Predicate<ClientPlugin> predicate,
        Set<PluginType> applied,
        List<ResolvedPlugin> resolved,
        int sequence
) {
    PluginType type = implementationType(plugin);
    if (applied.contains(type) || !predicate.test(plugin)) {
        return sequence;
    }

    applied.add(type);
    resolved.add(new ResolvedPlugin(
        plugin,
        plugin.getPluginPhase(),
        sequence++
    ));

    for (ClientPlugin child : plugin.getChildPlugins()) {
        sequence = collect(
            child,
            predicate,
            applied,
            resolved,
            sequence
        );
    }

    return sequence;
}
```

The explicit sequence value makes insertion order deterministic even when the
language's sort implementation is not stable.

After resolution, apply plugins in order. Each plugin receives the
configuration returned or modified by the previous plugin. The updated applied
type set and registered plugin set are retained by the resulting configuration.

## Call-scoped plugins

Clients should allow plugins to be added to a single operation invocation.
Call-scoped plugins begin with the client's configured dependencies,
registrations, predicate, and applied type set.

Only newly eligible plugins are applied. The resulting configuration is used to
construct or select the execution pipeline for that call and does not replace
the configuration stored by the client.

Call-scoped plugins should run before the interceptor
[`modifyBeforeCall`](#client-guidance-interceptors-modify-before-call) hook.
This gives interceptors installed by a call-scoped plugin an opportunity to
participate in call configuration, wrapping, and execution.

## Automatic plugins

Some languages provide a package-level service discovery mechanism. Client
implementations may use that mechanism to discover plugins automatically:

- Java can use `ServiceLoader`.
- PHP can use a registry populated by Composer autoload files.
- Other languages can use an equivalent package manifest or registry.

Automatic discovery should be modeled as a normal parent plugin:

```text
DefaultPlugin
└── AutoPlugin
    ├── DiscoveredPluginA
    └── DiscoveredPluginB
```

`AutoPlugin` performs no configuration of its own. Its children are the
discovered plugins. Making it a child of the default plugin gives discovered
plugins the same phases, predicates, deduplication, and persisted state as all
other plugins.

Automatic plugin discovery should:

- Produce plugins in deterministic registration order.
- Cache discovery and plugin instances when the language runtime permits it.
- Report invalid registrations or construction failures clearly.
- Allow users to disable all automatic plugins.

Disabling automatic plugins can be implemented by rejecting `AutoPlugin` with
the plugin predicate. Because a rejected parent's children are not visited,
none of the discovered plugins are resolved or applied.

Automatic plugins should be used sparingly. They are appropriate for
cross-cutting functionality that applies broadly but can determine its
applicability from an explicit signal such as a modeled trait, configured
context value, service identity, or selected protocol. Installing a package
should not cause unrelated clients to acquire behavior that the plugin cannot
determine is applicable.

Automatically discovered plugin instances may be shared by multiple clients.
They should be immutable or otherwise safe for concurrent use.

## Error handling

An error raised while resolving or applying a construction plugin should fail
client construction. An error raised by a call-scoped plugin should fail that
call before the execution pipeline begins.

Plugin application is not an execution stage, so plugin errors should not be
processed by interceptor completion hooks.
