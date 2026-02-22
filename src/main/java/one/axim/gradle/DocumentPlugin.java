package one.axim.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.Arrays;

/**
 * Gradle plugin entry point for {@code gradle-restdoc-generator}.
 *
 * <p>Registers the {@code restMetaGenerator} task that scans Spring Boot
 * {@code @RestController} classes and generates REST API documentation
 * (JSON metadata, OpenAPI 3.0 spec, spec-bundle, and Postman collections).
 *
 * <h3>Usage in {@code build.gradle}:</h3>
 * <pre>{@code
 * plugins {
 *     id 'gradle-restdoc-generator' version '2.0.6'
 * }
 *
 * restMetaGenerator {
 *     documentPath = 'build/docs'
 *     basePackage  = 'com.example'
 *     serviceId    = 'my-service'
 * }
 * }</pre>
 *
 * @see RestMetaGeneratorTask
 */
public class DocumentPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {

            public void execute(JavaPlugin javaPlugin) {

                JavaPluginExtension javaExtension =
                        project.getExtensions().getByType(JavaPluginExtension.class);

                SourceSet main = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                main.getJava().setSrcDirs(Arrays.asList("src" + File.separator + "main" + File.separator + "java"));
            }
        });

        Task javaCompile = project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);

        Task restMetaGenerateTask = project.getTasks().create(RestMetaGeneratorTask.TASK_NAME, RestMetaGeneratorTask.class);
        restMetaGenerateTask.dependsOn(javaCompile);

//		Task restMetaDeployTask = project.getTasks().create(RestMetaDeployTask.TASK_NAME, RestMetaDeployTask.class);
//		restMetaDeployTask.dependsOn(javaCompile);
    }
}
