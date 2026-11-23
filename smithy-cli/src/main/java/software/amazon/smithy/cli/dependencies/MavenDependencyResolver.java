/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.RUNTIME;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongConsumer;
import java.util.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.transport.http.RFC9457.HttpRFC9457Exception;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.cli.EnvironmentVariable;

/**
 * Resolves Maven dependencies for the Smithy CLI using Maven resolvers.
 */
public final class MavenDependencyResolver implements DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 1000;
    private static final long MAX_BACKOFF_MILLIS = 20000;

    private final List<RemoteRepository> remoteRepositories = new ArrayList<>();
    private final List<Dependency> dependencies = new ArrayList<>();
    private final DependencyFilter filter = DependencyFilterUtils.classpathFilter(RUNTIME, COMPILE);
    private final RepositorySystem repositorySystem;
    private final String cacheLocation;
    private final Proxy commonProxy;
    private final LongConsumer sleeper;

    public MavenDependencyResolver() {
        this(null);
    }

    /**
     * @param cacheLocation Maven local cache location.
     */
    public MavenDependencyResolver(String cacheLocation) {
        this(cacheLocation, MavenDependencyResolver::sleep);
    }

    MavenDependencyResolver(String cacheLocation, LongConsumer sleeper) {
        this.sleeper = sleeper;
        commonProxy = getProxy(EnvironmentVariable.SMITHY_PROXY_HOST.get(),
                EnvironmentVariable.SMITHY_PROXY_CREDENTIALS.get());

        // Sets a default maven local to the default local repo of the user.
        if (cacheLocation == null) {
            String userHome = System.getProperty("user.home");
            cacheLocation = Paths.get(userHome, ".m2", "repository").toString();
            LOGGER.fine("Set default Maven local cache location to ~/.m2/repository");
        }
        this.cacheLocation = cacheLocation;

        // RepositorySystemSupplier replaces the DefaultServiceLocator removed in resolver 2.x. Its default
        // factories already register the file + Apache HTTP transports and the basic connector, matching the
        // services this class wired by hand under 1.x. SessionBuilderSupplier likewise reproduces the
        // dependency traverser/manager/selector, conflict resolver, and artifact type registry that
        // MavenRepositorySystemUtils.newSession() set up under 1.x, so resolution behavior is preserved.
        // Both replace the 1.x ServiceLocator error handler by throwing unchecked, so wrap them.
        //
        // The repository system is a shutdownable resource in 2.x, but this resolver is short-lived (built, used
        // for a single resolve(), then discarded per CLI invocation), so we let process exit reclaim it rather
        // than thread Closeable through the customer-facing DependencyResolver SPI. Revisit if this is ever
        // reused in a long-lived or embedded context.
        try {
            repositorySystem = new RepositorySystemSupplier().get();
        } catch (RuntimeException e) {
            throw new DependencyResolverException(e);
        }
    }

    // A new session is created for each resolution attempt because a session remembers which artifacts it has
    // already checked, and would not request an artifact again after a failed transfer.
    private CloseableSession newSession() {
        try {
            return new SessionBuilderSupplier(repositorySystem)
                    .get()
                    .withLocalRepositories(new LocalRepository(cacheLocation))
                    .build();
        } catch (RuntimeException e) {
            throw new DependencyResolverException(e);
        }
    }

    // Read when resolving rather than in the constructor, so an invalid value doesn't fail commands that are
    // served from the dependency cache. An empty value is treated as unset.
    private static int getMaxAttempts() {
        String value = EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.get();
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_MAX_ATTEMPTS;
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result >= 1) {
                return result;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the error below.
        }
        throw new DependencyResolverException("Expected a positive integer for "
                + EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS + ", but found " + value);
    }

    @Override
    public void addRepository(MavenRepository repository) {
        try {
            URI uri = new URI(repository.getUrl());
            int repositoryIndex = remoteRepositories.size() + 1;
            String id = repository.getId().orElseGet(() -> repositoryIndex + "|" + uri.getHost());
            String userInfo = uri.getUserInfo();
            RemoteRepository.Builder builder = new RemoteRepository.Builder(id, "default", repository.getUrl());
            if (userInfo != null) {
                LOGGER.finest(() -> "Setting username and password for " + id + " using URI authority");
                addUserInfoAuth(uri, userInfo, builder);
            }
            repository.getHttpCredentials().ifPresent(credentials -> addUserInfoAuth(uri, credentials, builder));
            addProxyInfo(repository, builder);
            remoteRepositories.add(builder.build());
        } catch (URISyntaxException e) {
            throw new DependencyResolverException("Invalid Maven repository URL: " + repository.getUrl()
                    + ": " + e.getMessage());
        }
    }

    private void addProxyInfo(MavenRepository repository, RemoteRepository.Builder builder) {
        if (repository.getProxyHost().isPresent()) {
            builder.setProxy(
                    getProxy(repository.getProxyHost().get(), repository.getProxyCredentials().orElse(null)));
        } else if (commonProxy != null) {
            builder.setProxy(commonProxy);
        }
    }

    private static Proxy getProxy(final String host, final String userPass) {
        if (host == null) {
            return null;
        }

        URL hostUrl = parseHostString(host);
        if (userPass != null) {
            String[] userSettings = parseProxyCredentials(userPass);
            AuthenticationBuilder authBuilder = new AuthenticationBuilder()
                    .addUsername(userSettings[0])
                    .addPassword(userSettings[1]);
            return new Proxy(hostUrl.getProtocol(), hostUrl.getHost(), hostUrl.getPort(), authBuilder.build());
        } else {
            return new Proxy(hostUrl.getProtocol(), hostUrl.getHost(), hostUrl.getPort());
        }

    }

    private static String[] parseProxyCredentials(String value) {
        String[] settings = value.split(":");
        if (settings.length != 2) {
            throw new DependencyResolverException("Expected two values separated by ':' for "
                    + EnvironmentVariable.SMITHY_PROXY_CREDENTIALS + ", but found " + settings.length);
        }
        return settings;
    }

    private static URL parseHostString(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException exc) {
            throw new DependencyResolverException("Expected a valid URL for "
                    + EnvironmentVariable.SMITHY_PROXY_HOST + ". Found " + value);
        }
    }

    private void addUserInfoAuth(URI uri, String userInfo, RemoteRepository.Builder builder) {
        String[] parts = userInfo.split(":", 2);
        if (parts.length != 2) {
            throw new DependencyResolverException("Invalid credentials provided for " + uri);
        }
        builder.setAuthentication(
                new AuthenticationBuilder()
                        .addUsername(parts[0])
                        .addPassword(parts[1])
                        .build());
    }

    @Override
    public void addDependency(String coordinates) {
        dependencies.add(createDependency(coordinates, "compile"));
    }

    @Override
    public List<ResolvedArtifact> resolve() {
        if (remoteRepositories.isEmpty()) {
            LOGGER.warning("No Maven repositories are configured, so only the local repository cache is being used");
        }

        final List<ArtifactResult> results = resolveMavenArtifacts();
        final List<ResolvedArtifact> artifacts = new ArrayList<>(results.size());
        for (ArtifactResult result : results) {
            Artifact artifact = result.getArtifact();
            artifacts.add(new ResolvedArtifact(artifact.getFile().toPath(),
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getVersion()));
        }
        return artifacts;
    }

    private static Dependency createDependency(String coordinates, String scope) {
        Artifact artifact;
        try {
            artifact = new DefaultArtifact(coordinates);
        } catch (IllegalArgumentException e) {
            throw new DependencyResolverException("Invalid dependency: " + e.getMessage());
        }
        validateDependencyVersion(artifact);
        return new Dependency(artifact, scope);
    }

    private static void validateDependencyVersion(Artifact artifact) {
        String version = artifact.getVersion();
        if (version.equals("LATEST")) {
            throw new DependencyResolverException("LATEST dependencies are not supported: " + artifact);
        } else if (version.equals("latest-status") || version.startsWith("latest.")) {
            throw new DependencyResolverException("Gradle style latest dependencies are not supported: " + artifact);
        } else if (version.equals("RELEASE")) {
            throw new DependencyResolverException("RELEASE dependencies are not supported: " + artifact);
        } else if (version.contains("+")) {
            throw new DependencyResolverException("'+' dependencies are not supported: " + artifact);
        }
    }

    private List<ArtifactResult> resolveMavenArtifacts() {
        LOGGER.fine(() -> "Resolving Maven dependencies for Smithy CLI; repos: "
                + remoteRepositories + "; dependencies: " + dependencies);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setDependencies(dependencies);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
        int maxAttempts = getMaxAttempts();

        for (int attempt = 1;; attempt++) {
            try (CloseableSession session = newSession()) {
                List<ArtifactResult> results = repositorySystem
                        .resolveDependencies(session, dependencyRequest)
                        .getArtifactResults();
                LOGGER.fine(() -> "Resolved Maven dependencies: " + results);
                return results;
            } catch (DependencyResolutionException e) {
                IOException retryableFailure = findRetryableFailure(e);
                if (attempt >= maxAttempts || retryableFailure == null) {
                    throw new DependencyResolverException(e);
                }
                long delay = computeBackoff(attempt - 1);
                LOGGER.warning("Maven dependency resolution attempt " + attempt + " of " + maxAttempts
                        + " failed, retrying in " + delay + " ms: " + e.getMessage() + ": " + retryableFailure);
                sleeper.accept(delay);
            }
        }
    }

    // Exponential backoff with full jitter, following the AWS SDKs' standard retry mode.
    static long computeBackoff(int retry) {
        long ceiling = Math.min(MAX_BACKOFF_MILLIS, BASE_BACKOFF_MILLIS << Math.min(retry, 30));
        return (long) (ThreadLocalRandom.current().nextDouble() * ceiling);
    }

    // Retry when any transfer failed with an I/O error, such as a refused connection, a DNS failure, a TLS
    // error, or a timeout. Failures that are only missing artifacts or HTTP error responses are not retried.
    // DependencyCollectionException exposes only its first failure as the cause and adds every failure as a
    // suppressed exception, so suppressed exceptions are checked too. Returns the I/O error that makes the
    // failure retryable, or null if it isn't retryable.
    static IOException findRetryableFailure(Throwable e) {
        if (e == null
                || e instanceof ArtifactNotFoundException
                || e instanceof MetadataNotFoundException
                // An HTTP error response with an RFC 9457 problem details body.
                || e instanceof HttpRFC9457Exception) {
            return null;
        } else if (e instanceof IOException) {
            return (IOException) e;
        }
        for (Throwable suppressed : e.getSuppressed()) {
            IOException failure = findRetryableFailure(suppressed);
            if (failure != null) {
                return failure;
            }
        }
        return findRetryableFailure(e.getCause());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DependencyResolverException(e);
        }
    }
}
