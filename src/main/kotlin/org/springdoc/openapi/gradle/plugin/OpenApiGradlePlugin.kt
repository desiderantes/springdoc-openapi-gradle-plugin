package org.springdoc.openapi.gradle.plugin

import com.github.psxpaul.task.JavaExecFork
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.springframework.boot.gradle.tasks.run.BootRun

open class OpenApiGradlePlugin : Plugin<Project> {

	override fun apply(project: Project) {
		with(project) {
			// Run time dependency on the following plugins
			plugins.apply(SPRING_BOOT_PLUGIN)
			plugins.apply(EXEC_FORK_PLUGIN)

			extensions.create(EXTENSION_NAME, OpenApiExtension::class.java)
			tasks.register(FORKED_SPRING_BOOT_RUN_TASK_NAME, JavaExecFork::class.java)
			tasks.register(OPEN_API_TASK_NAME, OpenApiGeneratorTask::class.java)

			generate(this)
		}
	}

	private fun generate(project: Project) = project.run {
		val extension = extensions.findByName(EXTENSION_NAME) as OpenApiExtension

		// The task, used to resolve the application's main class (`bootRunMainClassName`)
		val bootRunMainClassNameTask = extension.classNameResolveTaskName.map {
			tasks.named<Task>(it)
		}.get()

		// The task, used to run the Spring Boot application (`bootRun`)
		val bootRunTask = tasks.named(extension.bootRunTaskName.get())
		val customBootRun = extension.customBootRun
		// Create a forked version spring boot run task
		val forkedSpringBoot: TaskProvider<JavaExecFork> = tasks.named<JavaExecFork>(
			FORKED_SPRING_BOOT_RUN_TASK_NAME
		) {
			this.dependsOn(tasks.named(bootRunMainClassNameTask.name))
			this.onlyIf { needToFork(bootRunTask, customBootRun, this) }
		}

		val openApiTask =
			tasks.named<OpenApiGeneratorTask>(OPEN_API_TASK_NAME, OpenApiGeneratorTask::class.java) {
				// This is my task. Before I can run it, I have to run the dependent tasks
				this.dependsOn(forkedSpringBoot)

				// Ensure the task inputs match those of the original application
				this.inputs.files(bootRunTask.get().inputs.files)
			}

		// The forked task need to be terminated as soon as my task is finished
		forkedSpringBoot.get().stopAfter = openApiTask as TaskProvider<Task>
	}

	private fun needToFork(
		bootRunTask: TaskProvider<Task>,
		customBootRun: CustomBootRunAction,
		fork: JavaExecFork
	): Boolean {
		val bootRun = bootRunTask.get() as BootRun

		val baseSystemProperties =
			customBootRun.systemProperties.orNull?.takeIf { it.isNotEmpty() }
				?: bootRun.systemProperties
		with(fork) {
			// copy all system properties, excluding those starting with `java.class.path`
			systemProperties = baseSystemProperties.filter {
				!it.key.startsWith(CLASS_PATH_PROPERTY_NAME)
			}

			// use original bootRun parameter if the list-type customBootRun properties are empty
			workingDir = customBootRun.workingDir.asFile.orNull
				?: fork.temporaryDir
			args = customBootRun.args.orNull?.takeIf { it.isNotEmpty() }?.toMutableList()
				?: bootRun.args.toMutableList()
			classpath = customBootRun.classpath.takeIf { !it.isEmpty }
				?: bootRun.classpath
			main = customBootRun.mainClass.orNull
				?: bootRun.mainClass.get()
			jvmArgs = customBootRun.jvmArgs.orNull?.takeIf { it.isNotEmpty() }
				?: bootRun.jvmArgs
			environment = customBootRun.environment.orNull?.takeIf { it.isNotEmpty() }
				?: bootRun.environment
		}
		return true
	}
}
