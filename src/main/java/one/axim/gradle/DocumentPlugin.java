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
