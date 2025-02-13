import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val versionSuffix: String? by project

plugins {
    `java-library`
    `maven-publish`

    pmd
    checkstyle
    jacoco
    id("com.github.spotbugs") version "6.1.4"
}

group = "io.datareplication"
val baseVersion = "0.0.1"
version = "${baseVersion}${versionSuffix ?: ""}"

repositories {
    mavenCentral()
}

val lombok = "org.projectlombok:lombok:1.18.36"

dependencies {
    implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
    implementation(platform("io.projectreactor:reactor-bom:2024.0.3"))

    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")
    implementation("commons-io:commons-io:2.18.0")
    implementation("com.github.mizosoft.methanol:methanol:1.8.1")
    implementation("com.google.code.gson:gson:2.12.1")

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
    testImplementation("com.networknt:json-schema-validator:1.5.5")
    testImplementation("org.wiremock:wiremock:3.12.0")

    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}

sourceSets {
    getByName("main") {
        java.srcDir("src/main/moduleInfo")
    }
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/datareplication/datareplication-java")
            credentials(PasswordCredentials::class) {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
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
