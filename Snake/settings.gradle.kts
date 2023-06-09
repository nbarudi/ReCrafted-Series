pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}


rootProject.name = "Snake-Parent"

startParameter.isParallelProjectExecutionEnabled = true

include("core")
include(":nms:shared")
include(":nms:v1_19_R3")