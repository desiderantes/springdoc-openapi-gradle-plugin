import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	`java-gradle-plugin`
	alias(libs.plugins.plugin.publish)
	alias(libs.plugins.sonarqube)
	kotlin("jvm") version libs.versions.kotlin
	`kotlin-dsl`
	`maven-publish`
	alias(libs.plugins.versions)
	alias(libs.plugins.detekt)
}

group = "org.springdoc"
version = "2.0.0"

sonarqube {
	properties {
		property("sonar.projectKey", "springdoc_springdoc-openapi-gradle-plugin")
	}
}
repositories {
	gradlePluginPortal()
	mavenCentral()
	maven {
		name = "Spring Repositories"
		url = uri("https://repo.spring.io/libs-release/")
	}
	maven {
		name = "Gradle Plugins Maven Repository"
		url = uri("https://plugins.gradle.org/m2/")
	}
	mavenLocal()
}

publishing {
	repositories {
		maven {
			// change URLs to point to your repos, e.g. http://my.org/repo
			val releasesRepoUrl =
				uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
			val snapshotsRepoUrl =
				uri("https://oss.sonatype.org/content/repositories/snapshots")
			url = if (version.toString()
					.endsWith("SNAPSHOT")
			) {
				snapshotsRepoUrl
			} else {
				releasesRepoUrl
			}
			credentials {
				username = System.getenv("OSSRH_USER")
				password = System.getenv("OSSRH_PASS")
			}
		}
	}
}

dependencies {
	implementation(kotlin("reflect"))
	implementation(gradleKotlinDsl())
	implementation(libs.klaxon)
	implementation(libs.awaitility.kotlin)
	implementation(libs.pluginlib.gradle.execfork)
	implementation(libs.pluginlib.spring.boot)

	testImplementation(gradleTestKit())
	testImplementation(platform(libs.junit.bom))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testImplementation(libs.jackson.kotlin)
	testImplementation(libs.jackson.yaml)

	detektPlugins(libs.detekt.formatting)
}

gradlePlugin {
	website = "https://github.com/springdoc/springdoc-openapi-gradle-plugin"
	vcsUrl = "https://github.com/springdoc/springdoc-openapi-gradle-plugin.git"
	plugins {
		create("springdoc-gradle-plugin") {
			id = "org.springdoc.openapi-gradle-plugin"
			displayName = "A Gradle plugin for the springdoc-openapi library"
			description = " This plugin uses springdoc-openapi to generate an OpenAPI description at build time"
			implementationClass = "org.springdoc.openapi.gradle.plugin.OpenApiGradlePlugin"
			tags = listOf("springdoc", "openapi", "swagger")
		}
	}
}


java {
	toolchain.languageVersion.set(libs.versions.java.map(JavaLanguageVersion::of))
	// Recommended by https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_packaging
	withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	compilerOptions{
		jvmTarget.set(libs.versions.java.map (JvmTarget::fromTarget))
	}
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	maxParallelForks =
		(Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

detekt {
	config.setFrom("config/detekt/detekt.yml")
	parallel = true
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
	jvmTarget = libs.versions.java.get()
}
