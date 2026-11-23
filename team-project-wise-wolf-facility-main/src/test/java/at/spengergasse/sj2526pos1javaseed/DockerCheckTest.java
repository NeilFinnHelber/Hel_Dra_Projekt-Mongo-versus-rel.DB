package at.spengergasse.sj2526pos1javaseed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Docker-related configuration files exist and contain
 * expected keywords, without requiring Docker to be installed or running.
 */
public class DockerCheckTest {

    @Test
    void dockerfile_exists_in_project_root() {
        java.io.File dockerfile = new java.io.File("Dockerfile");
        assertThat(dockerfile).exists();
    }

    @Test
    void dockerfile_contains_multistage_build() throws Exception {
        java.io.File dockerfile = new java.io.File("Dockerfile");
        String content = java.nio.file.Files.readString(dockerfile.toPath());
        assertThat(content).contains("AS builder");           // stage 1
        assertThat(content).contains("eclipse-temurin");     // JDK base image
        assertThat(content).contains("EXPOSE 8080");         // port declaration
        assertThat(content).contains("ENTRYPOINT");          // entrypoint defined
    }

    @Test
    void docker_compose_exists_in_project_root() {
        java.io.File compose = new java.io.File("docker-compose.yml");
        assertThat(compose).exists();
    }

    @Test
    void docker_compose_contains_postgres_and_app_services() throws Exception {
        java.io.File compose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(compose.toPath());
        assertThat(content).contains("postgres:");           // DB service
        assertThat(content).contains("app:");               // app service
        assertThat(content).contains("8080:8080");          // app port mapping
        assertThat(content).contains("pg_isready");         // healthcheck
        assertThat(content).contains("service_healthy");    // depends_on condition
    }

    @Test
    void docker_compose_app_uses_postgres_datasource_url() throws Exception {
        java.io.File compose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(compose.toPath());
        assertThat(content).contains("SPRING_DATASOURCE_URL");
        assertThat(content).contains("jdbc:postgresql://postgres:5432");
    }
}
