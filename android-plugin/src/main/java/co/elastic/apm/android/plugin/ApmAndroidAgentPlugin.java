/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.android.plugin;

import com.android.build.api.artifact.MultipleArtifact;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.variant.ApplicationVariant;
import com.android.build.gradle.BaseExtension;

import net.bytebuddy.build.gradle.android.ByteBuddyAndroidPlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskProvider;

import co.elastic.apm.android.agp.api.AgpCompatibilityEntrypoint;
import co.elastic.apm.android.agp.api.AgpCompatibilityManager;
import co.elastic.apm.android.agp.api.tools.ClasspathProvider;
import co.elastic.apm.android.agp.api.usecase.ApmInfoUseCase;
import co.elastic.apm.android.common.internal.logging.Elog;
import co.elastic.apm.android.plugin.extensions.ElasticApmExtension;
import co.elastic.apm.android.plugin.instrumentation.ElasticLocalInstrumentationFactory;
import co.elastic.apm.android.plugin.logging.GradleLoggerFactory;
import co.elastic.apm.android.plugin.tasks.OkHttpEventlistenerGenerator;
import co.elastic.apm.generated.BuildConfig;
import kotlin.Unit;

class ApmAndroidAgentPlugin implements Plugin<Project> {

    private Project project;
    private BaseExtension androidExtension;
    private ElasticApmExtension defaultExtension;
    private AgpCompatibilityManager compatibleManager;

    @Override
    public void apply(Project project) {
        this.project = project;
        Elog.init(new GradleLoggerFactory());
        androidExtension = project.getExtensions().getByType(BaseExtension.class);
        compatibleManager = AgpCompatibilityEntrypoint.findCompatibleManager(project);
        initializeElasticExtension(project);
        addBytebuddyPlugin();
        addSdkDependency();
        addInstrumentationDependency();
        addTasks();
        applyCompatibleUseCases();
    }

    private void initializeElasticExtension(Project project) {
        defaultExtension = project.getExtensions().create("elasticApm", ElasticApmExtension.class);
        defaultExtension.getServiceName().convention(project.provider(() -> androidExtension.getDefaultConfig().getApplicationId()));
        defaultExtension.getServiceVersion().convention(project.provider(() -> androidExtension.getDefaultConfig().getVersionName()));
    }

    private void addBytebuddyPlugin() {
        project.getPluginManager().apply(ByteBuddyAndroidPlugin.class);
    }

    private void addSdkDependency() {
        project.getDependencies().add("implementation", BuildConfig.SDK_DEPENDENCY_URI);
        if (kotlinPluginFound()) {
            project.getDependencies().add("implementation", BuildConfig.SDK_KTX_DEPENDENCY_URI);
        }
    }

    private boolean kotlinPluginFound() {
        return project.getExtensions().findByName("kotlin") != null;
    }

    private void addInstrumentationDependency() {
        project.getDependencies().add("byteBuddy", BuildConfig.INSTRUMENTATION_DEPENDENCY_URI);
    }

    private void addTasks() {
        ExtensionContainer extensions = project.getExtensions();
        ApplicationAndroidComponentsExtension extension = extensions.getByType(ApplicationAndroidComponentsExtension.class);

        extension.onVariants(extension.selector().all(), this::enhanceVariant);
    }

    private void enhanceVariant(ApplicationVariant applicationVariant) {
        addOkhttpEventListenerGenerator(applicationVariant, compatibleManager.getClasspathProvider());
        addLocalRemapping(applicationVariant);
    }

    private void addLocalRemapping(ApplicationVariant applicationVariant) {
        applicationVariant.getInstrumentation().transformClassesWith(ElasticLocalInstrumentationFactory.class, InstrumentationScope.PROJECT, none -> Unit.INSTANCE);
    }

    private void addOkhttpEventListenerGenerator(ApplicationVariant applicationVariant, ClasspathProvider classpathProvider) {
        TaskProvider<OkHttpEventlistenerGenerator> taskProvider =
                project.getTasks().register(applicationVariant.getName() + "GenerateOkhttpEventListener", OkHttpEventlistenerGenerator.class);
        taskProvider.configure(task -> {
            task.getOutputDir().set(project.getLayout().getBuildDirectory().dir(task.getName()));
            task.getAppRuntimeClasspath().from(classpathProvider.getRuntimeClasspath(applicationVariant));
            task.getJvmTargetVersion().set(androidExtension.getCompileOptions().getTargetCompatibility().toString());
        });
        applicationVariant.getArtifacts().use(taskProvider)
                .wiredWith(OkHttpEventlistenerGenerator::getOutputDir)
                .toAppendTo(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE);
    }

    private void applyCompatibleUseCases() {
        ApmInfoUseCase apmInfoUseCase = compatibleManager.getApmInfoUseCase(parameters -> {
            parameters.getServiceName().set(defaultExtension.getServiceName());
            parameters.getServiceVersion().set(defaultExtension.getServiceVersion());
            parameters.getServerUrl().set(defaultExtension.getServerUrl());
            parameters.getSecretToken().set(defaultExtension.getSecretToken());
            parameters.getApiKey().set(defaultExtension.getApiKey());
            parameters.getClasspathProvider().set(compatibleManager.getClasspathProvider());
        });
        apmInfoUseCase.execute();
    }
}