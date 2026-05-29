plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.66-stable")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "miuby-lib"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/charry-gabriel/MiubyLib")
            credentials {
                username = project.findProperty("gpr.user") as String?
                        ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String?
                        ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}