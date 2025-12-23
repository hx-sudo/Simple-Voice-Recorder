pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 本地 Maven 仓库（优先）
        mavenLocal()
        // Google 仓库优先（包含最新的 AndroidX）
        google()
        maven { 
            url = uri("https://jitpack.io")
            isAllowInsecureProtocol = false
        }
        // JitPack 备用地址
        maven { url = uri("https://www.jitpack.io") }
        // 阿里云镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        mavenCentral()
    }
}
include(":app")
