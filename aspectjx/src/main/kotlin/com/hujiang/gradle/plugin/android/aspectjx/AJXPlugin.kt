package com.hujiang.gradle.plugin.android.aspectjx

import com.android.Version
import com.android.build.gradle.BaseExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.util.GradleVersion

/**
 * aspectj plugin
 */
class AJXPlugin : Plugin<Project> {

    companion object {
        // any one of these plugins should be sufficient to proceed with applying this plugin
        private val PREREQ_PLUGIN_OPTIONS = arrayOf(
            "com.android.application",
            "com.android.feature",
            "com.android.dynamic-feature",
            "com.android.library",
            "android",
            "android-library"
        )
    }

    private var wasApplied = false

    override fun apply(project: Project) {
        project.extensions.create("aspectjx", AJXExtension::class.java)
        // At least one of the prerequisite plugins must by applied before this plugin can be applied, so
        // we will use the PluginManager.withPlugin() callback mechanism to delay applying this plugin until
        // after that has been achieved. If project evaluation completes before one of the prerequisite plugins
        // has been applied then we will assume that none of prerequisite plugins were specified and we will
        // throw an Exception to alert the user of this configuration issue.
        val applyWithPrerequisitePlugin = Action<AppliedPlugin> {
            if (wasApplied) {
                LoggerHolder.logger.info(
                    "The android-aspectjx plugin was already applied to the project: ${project.path}" +
                            " and will not be applied again after plugin: ${it.id}"
                )
            } else {
                wasApplied = true

                doApply(project)
            }
        }

        PREREQ_PLUGIN_OPTIONS.forEach {
            project.pluginManager.withPlugin(it, applyWithPrerequisitePlugin)
        }
        project.afterEvaluate {
            if (!wasApplied) {
                throw GradleException(
                    "The android-aspectjx plugin could not be applied during project evaluation."
                            + " One of the Android plugins must be applied to the project first."
                )
            }
        }
    }

    private fun doApply(project: Project) {
        // 优先设置logger
        LoggerHolder.logger = project.logger
        val gradleVersion = project.gradle.gradleVersion
        val dependencyGav = "org.aspectj:aspectjrt:1.9.3"
        logQuiet("agp version[${Version.ANDROID_GRADLE_PLUGIN_VERSION}]")
        if (GradleVersion.current() > GradleVersion.version("4.0")) {
            logQuiet("gradle version[$gradleVersion] > 4.0")
            project.dependencies.add("implementation", dependencyGav)
            logQuiet("auto add dependency: 'implementation $dependencyGav'")
        } else {
            logQuiet("gradle version[$gradleVersion] < 4.0")
            project.dependencies.add("compile", dependencyGav)
            logQuiet("auto add dependency: 'compile $dependencyGav'")
        }

//        val findAll = project.plugins.findAll(Closure.IDENTITY)
//        logQuiet("plugins:$findAll")
        // register AspectTransform
        val android = project.extensions.getByType(BaseExtension::class.java)
        android.registerTransform(AJXTransform(project))
        logQuiet("register AJXTransform:${android.transforms}")
    }

    private fun logQuiet(msg: String) {
        LoggerHolder.logger.quiet("[${AJXTransform.TAG}] $msg")
    }
}
