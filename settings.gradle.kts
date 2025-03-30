pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // necessary for loading projects from github
        maven {
            url = uri("https://jitpack.io")
        }
        jcenter()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // necessary for loading projects from github
        maven {
            url = uri("https://jitpack.io")
        }
        jcenter()


    }
}

rootProject.name = "URL Player Beta"
include(":app")
