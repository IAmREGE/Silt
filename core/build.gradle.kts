plugins {
    java
    id("io.github.sgtsilvio.gradle.javadoc-links") version "0.10.0"
}

dependencies {
    compileOnly("org.jetbrains:annotations-java5:24.1.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isFork = true
    options.forkOptions.javaHome = file("/lib/jvm/java-8-openjdk")
}

/*application {
    mainClass.set("rege.rege.silt.test.Test1")
}*/
