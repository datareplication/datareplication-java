import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jreleaser.model.Active

val versionSuffix: String? by project

plugins {
    `java-library`
    `maven-publish`
    id("org.jreleaser") version "1.17.0"
    pmd
    checkstyle
    jacoco
    id("com.github.spotbugs") version "6.1.3"
}

group = "io.datareplication"

// after updating this, make sure to push a new git tag
val baseVersion = "1.0.0"
version = "${baseVersion}${versionSuffix ?: ""}"
// match semver `x.y.z-something`
val isPrereleasePattern = """\d+\.\d+\.\d+-.+"""

val ghUser = "datareplication"
val ghRepo = "datareplication-java"
val ghUrl = "https://github.com/$ghUser/$ghRepo"

val topDesc = "Data Synchronization over HTTP"

repositories {
    mavenCentral()
}

val lombok = "org.projectlombok:lombok:1.18.36"

dependencies {
    implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
    implementation(platform("io.projectreactor:reactor-bom:2024.0.1"))

    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")
    implementation("commons-io:commons-io:2.18.0")
    implementation("com.github.mizosoft.methanol:methanol:1.7.0")
    implementation("com.google.code.gson:gson:2.11.0")

    compileOnly(lombok)
    annotationProcessor(lombok)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation(platform("org.mockito:mockito-bom:5.15.2"))

    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.networknt:json-schema-validator:1.5.4")
    testImplementation("org.wiremock:wiremock:3.10.0")

    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}

sourceSets {
    getByName("main") {
        java.srcDir("src/main/moduleInfo")
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks {
    val delombok by registering(JavaExec::class) {
        classpath = project.configurations.getByName("compileClasspath")
        mainClass = "lombok.launch.Main"
        args("delombok")

        val outputDir by extra { layout.buildDirectory.dir("delombok").get().asFile }
        outputs.dir(outputDir)
        args("-d", outputDir)
        doFirst {
            outputDir.delete()
        }

        val srcDir = sourceSets["main"].java.srcDirs.first()
        inputs.dir(srcDir)
        args(srcDir)
    }
    javadoc {
        dependsOn(delombok)
        val outputDir: File by delombok.get().extra
        source = fileTree(outputDir)
        exclude("io/datareplication/internal")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
    }
}

checkstyle {
    sourceSets = project.sourceSets
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.9".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.jacocoTestCoverageVerification)
}

pmd {
    isConsoleOutput = true
    ruleSetFiles = files("config/pmd/ruleset.xml")
}

spotbugs {
    ignoreFailures = true
    includeFilter = file("config/spotbugs/spotbugs.xml")
}

tasks.withType<Jar> {
    archiveBaseName.set(project.name)
    archiveVersion.set(project.version.toString())
}

tasks.named("jreleaserFullRelease") {
    doFirst {
        val outputDir = layout.buildDirectory.dir("jreleaser").get().asFile
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }
}

val outputDir = layout.buildDirectory.dir("jreleaser").get().asFile
if (!outputDir.exists()) {
    outputDir.mkdirs()
}

jreleaser {
    dryrun.set(System.getenv("CI").isNullOrBlank())

    project {
        name.set(ghRepo)
        description.set(topDesc)
        version.set(rootProject.version.toString())
        authors.set(listOf("The Datareplication developers"))
        license.set("MIT")
        inceptionYear.set("2025")
        links {
            homepage = ghUrl
        }
    }

    release {
        github {
            repoOwner.set(ghUser)
            name.set(ghRepo)
            branch.set("main")

            // skip tag because we're running release on tag creation
            skipTag.set(true)
            prerelease {
                pattern.set(isPrereleasePattern)
            }
        }
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
        verify.set(true)
    }

    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active.set(Active.ALWAYS)
                url.set("https://central.sonatype.com/api/v1/publisher")
                subprojects.filter { it.plugins.hasPlugin("java") }.forEach { subproject ->
                    stagingRepositories.add("${subproject.layout.buildDirectory.get()}/staging-deploy")
                }
                applyMavenCentralRules.set(true)
                retryDelay.set(20)
                maxRetries.set(90)
            }
        }
    }

    distributions {
        create(name) {
            project {
                description.set(topDesc)
            }
            artifact {
                path.set(tasks.named<Jar>("jar").get().archiveFile.get().asFile)
            }
            artifact {
                path.set(tasks.named<Jar>("sourcesJar").get().archiveFile.get().asFile)
                platform.set("java-sources")
            }
            artifact {
                path.set(tasks.named<Jar>("javadocJar").get().archiveFile.get().asFile)
                platform.set("java-docs")
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = project.name
            version = version.toString()

            from(components["java"])

            pom {
                name.set(project.name)
                description.set(topDesc)
                url.set(rootProject.jreleaser.project.links.homepage)

                inceptionYear.set(rootProject.jreleaser.project.inceptionYear.get())
                licenses {
                    license {
                        name.set(rootProject.jreleaser.project.license.get())
                        url.set("https://opensource.org/licenses/${rootProject.jreleaser.project.license.get()}")
                    }
                }
                developers {
                    developer {
                        id.set(rootProject.jreleaser.release.github.repoOwner.get())
                        name.set(rootProject.jreleaser.project.authors.get().joinToString())
                    }
                }
                scm {
                    connection.set("scm:git:$ghUrl.git")
                    developerConnection.set("scm:git:ssh://github.com/$ghUser/$ghRepo.git")
                    url.set(ghUrl)
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}