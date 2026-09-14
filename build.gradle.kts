plugins { java }

allprojects {
    group   = "com.bento"
    version = "1.0.0-SNAPSHOT"
    repositories {
        mavenCentral()
        maven("https://m2.dv8tion.net/releases")
        maven("https://maven.fabricmc.net/")
    }
}
