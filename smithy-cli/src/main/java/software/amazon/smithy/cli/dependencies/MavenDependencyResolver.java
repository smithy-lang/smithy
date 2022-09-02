/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.cli.dependencies;

import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.RUNTIME;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Resolves Maven dependencies for the Smithy CLI using Maven resolvers.
 */
@SmithyUnstableApi
public final class MavenDependencyResolver implements DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

    private final List<RemoteRepository> remoteRepositories = new ArrayList<>();
    private final List<Dependency> dependencies = new ArrayList<>();
    private final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    private final DependencyFilter filter = DependencyFilterUtils.classpathFilter(RUNTIME, COMPILE);
    private final RepositorySystem repositorySystem;

    public MavenDependencyResolver() {
        this(null);
    }

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

        repositorySystem = locator.getService(RepositorySystem.class);

        // Sets a default maven local to the default local repo of the user.
        if (cacheLocation == null) {
            String userHome = System.getProperty("user.home");
            cacheLocation = Paths.get(userHome, ".m2", "repository").toString();
        }

        LocalRepository local = new LocalRepository(cacheLocation);
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, local));
    }

    @Override
    public void addRepository(MavenRepository repository) {
        try {
            URI uri = new URI(repository.getUrl());
            String name = uri.getHost();
            String userInfo = uri.getUserInfo();
            RemoteRepository.Builder builder = new RemoteRepository.Builder(name, "default", repository.getUrl());
            if (userInfo != null) {
                LOGGER.finest(() -> "Setting username and password for " + name + " using URI authority");
                addUserInfoAuth(uri, userInfo, builder);
            }
            repository.getHttpCredentials().ifPresent(credentials -> addUserInfoAuth(uri, credentials, builder));
            remoteRepositories.add(builder.build());
        } catch (URISyntaxException e) {
            throw new DependencyResolverException("Invalid Maven repository URL: " + repository.getUrl()
                                                  + ": " + e.getMessage());
        }
    }

    private void addUserInfoAuth(URI uri, String userInfo, RemoteRepository.Builder builder) {
        String[] parts = userInfo.split(":", 2);
        if (parts.length != 2) {
            throw new DependencyResolverException("Invalid credentials provided for " + uri);
        }
        builder.setAuthentication(new MavenAuth(parts[0], parts[1]));
    }

    @Override
    public void addDependency(String coordinates) {
        addDependency(coordinates, "compile");
    }

    public void addDependency(String coordinates, String scope) {
        Dependency dependency;
        dependency = createDependency(coordinates, scope);
        dependencies.add(dependency);
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
            artifacts.add(new ResolvedArtifact(artifact.getFile().toPath(), artifact.getGroupId(),
                                               artifact.getArtifactId(), artifact.getVersion()));
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
        if (artifact.isSnapshot()) {
            throw new DependencyResolverException("Snapshot dependencies are not supported: " + artifact);
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

    /**
     * Based on Maven's StringAuthentication.
     */
    private static final class MavenAuth implements Authentication {
        private final String key;
        private final String value;

        private MavenAuth(String key, String value) {
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Authentication key must be provided");
            }
            this.key = key;
            this.value = value;
        }

        @Override
        public void fill(AuthenticationContext context, String key, Map<String, String> data) {
            context.put(this.key, value);
        }

        @Override
        public void digest(AuthenticationDigest digest) {
            digest.update(key, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }
            MavenAuth that = (MavenAuth) obj;
            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return key + "=****";
        }
    }
}
