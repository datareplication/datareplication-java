val buildTimestamp: String by project
val versionSuffix: String? by project

plugins {
    `java-library`
    `maven-publish`

    pmd
    checkstyle
    jacoco
    id("com.github.spotbugs") version "6.0.12"
}

group = "io.datareplication"
val baseVersion = "0.1"
version = "${baseVersion}.${buildTimestamp}${versionSuffix ?: ""}"

repositories {
    mavenCentral()
}

val lombok = "org.projectlombok:lombok:1.18.30"

dependencies {
    implementation(platform("org.slf4j:slf4j-bom:2.0.13"))
    implementation(platform("io.projectreactor:reactor-bom:2023.0.5"))

    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")
    implementation("commons-io:commons-io:2.16.1")
    implementation("com.github.mizosoft.methanol:methanol:1.7.0")
    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly(lombok)
    annotationProcessor(lombok)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation(platform("org.mockito:mockito-bom:5.11.0"))

    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("com.networknt:json-schema-validator:1.4.0")
    testImplementation("org.wiremock:wiremock:3.5.4")

    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
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
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("datareplication.username") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("datareplication.password") as String?
            }
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
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
