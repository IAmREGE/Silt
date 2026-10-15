plugins {
    java
}

val sourcesLicenseFilenames: Set<String> = setOf(
    "GPL-3.0.txt", "LGPL-3.0.txt"
)

allprojects {
    repositories {
        maven("https://mirrors.aliyun.com/repository/public") {
            name = "Aliyun"
        }
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        java {
            sourceCompatibility = JavaVersion.VERSION_1_5
            targetCompatibility = JavaVersion.VERSION_1_5
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile> {
            val jdk5Rt = file("/usr/local/lib/jvm/jdk1.5.0_22/jre/lib/rt.jar")
            if (jdk5Rt.exists()) {
                options.bootstrapClasspath = files(jdk5Rt)
            }
        }

        tasks.withType<Jar> {
            archiveBaseName.set(rootProject.name)
            archiveAppendix.set(project.name)
            archiveVersion.set(project.version.toString())
            if (name != "javadocJar") {
                val licenseFile = rootProject.file("LICENSE")
                if (licenseFile.exists()) {
                    from(licenseFile) {
                        into("META-INF")
                    }
                }
                for (filename in sourcesLicenseFilenames) {
                    val licenseFile = rootProject.file("LICENSES/$filename")
                    if (licenseFile.exists()) {
                        from(licenseFile) {
                            into("META-INF/LICENSES")
                        }
                    }
                }
            } else {
                val licenseFile = rootProject.file("LICENSES/FDL-1.3.txt")
                if (licenseFile.exists()) {
                    from(licenseFile) {
                        rename {
                            "LICENSE"
                        }
                    }
                }
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_5
    targetCompatibility = JavaVersion.VERSION_1_5
}

tasks.withType<Jar> {
    enabled = false
}
