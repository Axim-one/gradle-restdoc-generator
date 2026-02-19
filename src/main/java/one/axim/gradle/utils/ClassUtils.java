package one.axim.gradle.utils;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public class ClassUtils {
    private Project project;
    private SourceSet mainSourceSet;
    private URLClassLoader classLoader;

    private ArrayList<URL> classUrls;

    private ArrayList<URL> externalJarUrls;

    public ClassUtils(Project p) {
        classUrls = new ArrayList<>();
        externalJarUrls = new ArrayList<>();

        project = p;
        if (project != null) {
            mainSourceSet = prepareMainSourceSet();
            classLoader = prepareClassLoader();
        }
    }

    public void addExternalJarPath(String path) {
        try {
            externalJarUrls.add(
                    Path.of(project.getRootProject().getProjectDir().getPath() + File.separator + path).toUri().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<URL> getExternalJarUrls() {
        return this.externalJarUrls;
    }

    public File getSourceFile(Class<?> clazz) {
        String suffix = clazz.getName().replaceAll("\\.", Matcher.quoteReplacement(File.separator)) + ".java";

        File sourceFile = null;

        try {

            Set<Project> projects = project.getRootProject().getAllprojects();

            for (Project p : projects) { // All Project Find
                System.out.println("project " + p.getName() + " source load ... find " + suffix);

                File sourceRoot = new File(p.getProjectDir().getPath() + "/src/main/java");

                if (sourceRoot.exists()) {

                    sourceFile = new File(sourceRoot.getPath() + "/" + suffix);
                }

                if (sourceFile != null && sourceFile.exists()) {
                    return sourceFile;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public SourceSet getMainSourceSet() {
        return mainSourceSet;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ClassLoader getAllClassLoader() {

        ArrayList<URL> mergeUrls = new ArrayList<>();

        try {
            mergeUrls.add(project.getBuildDir().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        mergeUrls.addAll(this.classUrls);
        mergeUrls.addAll(this.externalJarUrls);

        return new URLClassLoader(mergeUrls.toArray(new URL[0]), project.getClass().getClassLoader());
    }

    private SourceSet prepareMainSourceSet() {

        SourceSet sourceSet = ((SourceSetContainer) project.getProperties().get("sourceSets")).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        return sourceSet;
    }

    private URLClassLoader prepareClassLoader() {
        try {
            FileCollection fileCollection = mainSourceSet.getRuntimeClasspath();
            for (File file : fileCollection) {
                if (file.exists()) {
                    System.out.println(" load :: " + file.toURI().toURL());
                    if (!file.toURI().toURL().toString().endsWith(".gradle") && file.toURI().toURL().toString().lastIndexOf("resources") == -1)
                        classUrls.add(file.toURI().toURL());
                }
            }

            classUrls.add(project.getBuildDir().toURI().toURL());

            return new URLClassLoader(classUrls.toArray(new URL[0]), project.getClass().getClassLoader());
        } catch (Exception e) {
        }
        return null;
    }

    public List<Class<?>> getSuperClasses(Class<?> cls) {
        List<Class<?>> classList = new ArrayList<>();
        Class<?> clazz = cls;
        Class<?> superclass = cls.getSuperclass();
        classList.add(superclass);
        while (superclass != null) {
            clazz = superclass;
            superclass = clazz.getSuperclass();
            if (superclass != null)
                classList.add(superclass);
        }
        return classList;
    }

    public List<URL> getClassUrls() {
        return this.classUrls;
    }
}
