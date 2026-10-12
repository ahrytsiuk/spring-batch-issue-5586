package org.springframework.batch;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

public class MavenArtifactResolver implements AutoCloseable {

    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public MavenArtifactResolver() {
        system = new RepositorySystemSupplier().get();

        session = new DefaultRepositorySystemSession();

        var localRepository = new LocalRepository(
                Path.of(System.getProperty("user.home"), ".m2", "repository")
        );

        session.setLocalRepositoryManager(
                system.newLocalRepositoryManager(session, localRepository)
        );

        repositories = List.of(
                new RemoteRepository.Builder(
                        "central",
                        "default",
                        "https://repo.maven.apache.org/maven2/"
                ).build()
        );
    }

    public byte[] getResourceContent(String gav, String resourcePath) throws Exception {
        var jarPath = resolve(gav);

        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry(resourcePath);
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Resource '%s' not found in artifact '%s'".formatted(resourcePath, gav)
                );
            }

            try (var inputStream = jar.getInputStream(entry)) {
                return inputStream.readAllBytes();
            }
        }
    }

    private Path resolve(String gav) throws Exception {
        var artifact = new DefaultArtifact(gav);

        var request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(repositories);

        return system.resolveArtifact(session, request)
                .getArtifact()
                .getPath();
    }

    @Override
    public void close() {
        system.shutdown();
    }
}
