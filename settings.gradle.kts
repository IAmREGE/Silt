gradle.beforeProject {
    val projectVersion = "0.0.1a1"
    version = projectVersion
}

rootProject.name = "Silt"
include("core")
include("parser")
