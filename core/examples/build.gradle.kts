plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jogamp.org/deployment/maven") }
}

dependencies {
    implementation(project(":core"))
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val joglNativeTag = when {
    osName.contains("mac") -> "macosx-universal"
    osName.contains("win") -> "windows-amd64"
    osArch.contains("aarch64") || osArch.contains("arm") -> "linux-aarch64"
    else -> "linux-amd64"
}
val joglNativesDir = layout.buildDirectory.dir("jogl-natives")

// jogamp's TempJarCache can't find the nativewindow/newt natives under Gradle's
// cache layout, so extract the platform dylibs and hand them to java.library.path.
val extractJoglNatives by tasks.registering(Copy::class) {
    from(configurations.runtimeClasspath.map { cfg ->
        cfg.files.filter { it.name.contains("natives-$joglNativeTag") }.map { zipTree(it) }
    })
    include("natives/$joglNativeTag/*")
    eachFile { path = name }
    includeEmptyDirs = false
    into(joglNativesDir)
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    if (name.startsWith("webgpu.")) {
        // The NEWT surface is AWT-cooperative (AWT pumps NSApp on macOS thread 0),
        // so it must NOT run under -XstartOnFirstThread — leave the main thread free.
        dependsOn(extractJoglNatives)
        jvmArgs("-Djava.library.path=" +
            joglNativesDir.get().asFile.absolutePath +
            System.getProperty("path.separator") +
            System.getProperty("java.library.path"))
    }
}
