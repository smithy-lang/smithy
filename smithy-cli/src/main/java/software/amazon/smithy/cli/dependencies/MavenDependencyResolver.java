/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.RUNTIME;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.cli.EnvironmentVariable;

/**
 * Resolves Maven dependencies for the Smithy CLI using Maven resolvers.
 */
public final class MavenDependencyResolver implements DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

    private final List<RemoteRepository> remoteRepositories = new ArrayList<>();
    private final List<Dependency> dependencies = new ArrayList<>();
    private final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    private final DependencyFilter filter = DependencyFilterUtils.classpathFilter(RUNTIME, COMPILE);
    private final RepositorySystem repositorySystem;
    private final Proxy commonProxy;

    public MavenDependencyResolver() {
        this(null);
    }

    /**
     * @param cacheLocation Maven local cache location.
     */
    public MavenDependencyResolver(String cacheLocation) {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                throw new DependencyResolverException(exception);
            }
        });
        commonProxy = getProxy(EnvironmentVariable.SMITHY_PROXY_HOST.get(),
                EnvironmentVariable.SMITHY_PROXY_CREDENTIALS.get());
        repositorySystem = locator.getService(RepositorySystem.class);

        // Sets a default maven local to the default local repo of the user.
        if (cacheLocation == null) {
            String userHome = System.getProperty("user.home");
            cacheLocation = Paths.get(userHome, ".m2", "repository").toString();
            LOGGER.fine("Set default Maven local cache location to ~/.m2/repository");
        }

        LocalRepository local = new LocalRepository(cacheLocation);
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, local));
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

        try {
            List<ArtifactResult> results = repositorySystem
                    .resolveDependencies(session, dependencyRequest)
                    .getArtifactResults();
            LOGGER.fine(() -> "Resolved Maven dependencies: " + results);
            return results;
        } catch (DependencyResolutionException e) {
            throw new DependencyResolverException(e);
        }
    }
}
