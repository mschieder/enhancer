package org.openjpa.ide.idea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;

import org.openjpa.ide.idea.integration.EnhancerSupport;

/**
 * Utility method collection for IDEA projects API.
 */
final class IdeaProjectUtils {

    //
    // Private constructor
    //

    private IdeaProjectUtils() {
        // no instantiation allowed
    }

    //
    // Utility methods
    //

    /**
     * Find modules containing the appropriate enhancer.
     *
     * @param enhancerSupport enum containing persistence implementation information, especially enhancer class names.
     * @param project         the project to search in
     * @return List of modules containing appropriate enhancers
     */
    static List<Module> getDefaultAffectedModules(final EnhancerSupport enhancerSupport, final Project project, boolean fast) {
        final CompilerManager compilerManager = CompilerManager.getInstance(project);
        final CompileScope projectCompileScope = compilerManager.createProjectCompileScope(project);
        final Module[] compileScopeAffectedModules = projectCompileScope.getAffectedModules();
        final List<Module> affectedModules = new ArrayList<>(compileScopeAffectedModules.length);

        if (fast){
            affectedModules.addAll(Arrays.asList(compileScopeAffectedModules));
            return affectedModules;
        }

        //
        // find enhancer class in module dependencies
        for (final Module affectedModule : compileScopeAffectedModules) {

            // query for enhancer class
            final Query<PsiClass> psiClassQuery = createEnhancerClassQuery(enhancerSupport, affectedModule);

            // query returns results? -> enhancer present
            if (psiClassQuery.findFirst() != null) {
                affectedModules.add(affectedModule);
            }
        }
        return affectedModules;
    }

    /**
     * Find virtual files by extension by traversing the directory tree.
     *
     * @param rootDir   the directory to start with
     * @param extension the extension to search for
     * @return List of virtual files matching the file extension
     */
    static List<VirtualFile> findFilesByExtension(final VirtualFile rootDir,
                                                  final String extension) {

        final List<VirtualFile> children = new LinkedList<>();

        // simply iterate directory for finding files
        for (final VirtualFile entry : rootDir.getChildren()) {
            if (entry.isDirectory()) {
                //
                // recursive invocation for directories
                children.addAll(findFilesByExtension(entry, extension));

            } else if (extension.equals(entry.getExtension())) {
                //
                // file with correct extension found
                children.add(entry);
            }
        }

        return children;
    }

    /**
     * Find classes annotated with persistence relevant annotations (depends on {@link org.openjpa.ide.idea.integration.openjpa.EnhancerSupportOpenJpa}).
     *
     * @param enhancerSupport Enhancer integration to search classes for
     * @param module          Module to search in
     * @return List of virtual classes annotated with corresponding annotations
     */
    static List<PsiClass> findPersistenceAnnotatedClasses(final EnhancerSupport enhancerSupport, final Module module) {
        final List<PsiClass> annotatedClasses = new ArrayList<>();

        for (final String annotationName : enhancerSupport.getAnnotationNames()) {
            final Collection<PsiClass> psiClasses = findAnnotatedClasses(module, annotationName);
            if (!psiClasses.isEmpty()) {
                annotatedClasses.addAll(psiClasses);
            }
        }

        return annotatedClasses;
    }

    /**
     * Find classes annotated with provided annotation.
     *
     * @param module              Module to search in
     * @param annotationClassName Fully qualified annotation class name
     * @return List of virtual classes annotated with corresponding annotation
     */
    static Collection<PsiClass> findAnnotatedClasses(final Module module, final String annotationClassName) {
        final PsiClass annotationPsiClass = findClassInModule(module, annotationClassName);
        final Collection<PsiClass> found;
        if (annotationPsiClass == null || !annotationPsiClass.isAnnotationType()) {
            found = Collections.emptyList();
        } else {
            found = findAnnotatedClasses(module, annotationPsiClass);
        }
        return found;
    }

    /**
     * Find classes annotated with provided annotation.
     *
     * @param module             Module to search in
     * @param annotationPsiClass Virtual class for annotation to search for
     * @return List of virtual classes annotated with corresponding annotation
     */
    static Collection<PsiClass> findAnnotatedClasses(final Module module, final PsiClass annotationPsiClass) {
        final Query<PsiClass> pcQuery = AnnotatedElementsSearch.searchPsiClasses(annotationPsiClass, module.getModuleScope());
        return pcQuery.findAll();
    }

    /**
     * Find a class via fully qualified name in module.
     *
     * @param module    Module to search in
     * @param className Fully qualified name of the class to search for
     * @return Corresponding class or null
     */
    static PsiClass findClassInModule(final Module module, final String className) {
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
        return javaPsiFacade.findClass(className, module.getModuleWithDependenciesAndLibrariesScope(true));
    }

    /**
     * Convert a java package name to a path.
     *
     * @param packageName the package
     * @return the path
     */
    @SuppressWarnings("MagicCharacter")
    public static String packageToPath(final String packageName) {
        return packageName.replace('.', '/');
    }

    /**
     * Convert a class to a path.
     *
     * @param psiClass the class
     * @return the path
     */
    public static String classToPath(final PsiClass psiClass) {
        if (psiClass.getContainingClass() == null){
            return packageToPath(psiClass.getQualifiedName());
        }
        else{
            return classToPath(psiClass.getContainingClass()) + "$" + psiClass.getName();
        }
    }

    //
    // Helper methods
    //

    /**
     * Create query for enhancer classes.
     *
     * @param enhancerSupport Determines which classes to search for
     * @param module          Module to search in
     * @return the query
     */
    private static Query<PsiClass> createEnhancerClassQuery(final EnhancerSupport enhancerSupport,
                                                            final Module module) {
        // inspection going wrong as 'equals' variable changes during loop
        return AllClassesSearch.search(module.getModuleWithLibrariesScope(), module.getProject(), s -> {
            final String[] enhancerClassNames = enhancerSupport.getEnhancerClassNames();
            boolean equals = false;

            if (s != null) {

                for (final String enhancerClassName : enhancerClassNames) {
                    equals = s.equals(enhancerClassName);
                    if (equals) {
                        break;
                    }
                }
            }

            return equals;
        });
    }

    /**
     * waits max. 1 sec until the FileIndex is ready.
     *
     * @param enhancerSupport Determines which classes to search for
     * @param module          Module to search in
     * @throws IndexNotReadyException when the index is not ready after 1 sec
     */
    public static void waitUntilIndexIsReady(final EnhancerSupport enhancerSupport,
                                             final Module module) {

        final var application = ApplicationManager.getApplication();
        for (int i = 0; i < 10; i++) {
            try {
                application.runReadAction(() -> {
                    findPersistenceAnnotatedClasses(enhancerSupport, module);
                });
            } catch (IndexNotReadyException e) {
                // ignore
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                if (i == 9) {
                    // after the 9th try throw IndexNotReadyException
                    throw e;
                }
            }
        }

    }
}
