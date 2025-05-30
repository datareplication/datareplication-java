import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jreleaser.model.Active

val versionSuffix: String? by project
val expectedVersion: String? by project

plugins {
    `java-library`
    `maven-publish`
    id("org.jreleaser") version "1.18.0"
    pmd
    checkstyle
    jacoco
    id("com.github.spotbugs") version "6.1.12"
}

group = "io.datareplication"

val baseVersion = "1.0.2"
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

val lombok = "org.projectlombok:lombok:1.18.38"

dependencies {
    implementation(platform("org.slf4j:slf4j-bom:2.0.17"))
    implementation(platform("io.projectreactor:reactor-bom:2024.0.6"))

    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")
    implementation("commons-io:commons-io:2.19.0")
    implementation("com.github.mizosoft.methanol:methanol:1.8.2")
    implementation("com.google.code.gson:gson:2.13.1")

    compileOnly(lombok)
    annotationProcessor(lombok)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.0"))
    testImplementation(platform("org.mockito:mockito-bom:5.18.0"))

    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.networknt:json-schema-validator:1.5.6")
    testImplementation("org.wiremock:wiremock:3.13.0")

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

tasks.register("checkVersion") {
    doLast {
        val version = project.version.toString()
        if (version != expectedVersion) {
            throw GradleException("Version $version does not match $expectedVersion.")
        }
    }
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
                counter = "INSTRUCTION"
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
            homepage = "https://datareplication.io"
            documentation = "https://datareplication.io"
            bugTracker = "$ghUrl/issues"
            vcsBrowser = ghUrl
        }
    }

    release {
        github {
            if (version.toString().endsWith("-SNAPSHOT")) {
                skipRelease.set(true)
                skipTag.set(true)
            }
            repoOwner.set(ghUser)
            name.set(ghRepo)

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
                active.set(Active.RELEASE)
                url.set("https://central.sonatype.com/api/v1/publisher")
                stagingRepositories.add("${layout.buildDirectory.get()}/staging-deploy")
                applyMavenCentralRules.set(true)
                retryDelay.set(20)
                maxRetries.set(90)
            }

            nexus2.create("snapshots") {
                active.set(Active.SNAPSHOT)
                url.set("https://ossrh-staging-api.central.sonatype.com/service/local/")
                snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                stagingRepositories.add("${layout.buildDirectory.get()}/staging-deploy")
                applyMavenCentralRules.set(true)
                snapshotSupported.set(true)
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
                    developerConnection.set("scm:git@github.com/$ghUser/$ghRepo.git")
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