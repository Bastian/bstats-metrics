import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.printer.PrettyPrinter
import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.google.googlejavaformat.java.Formatter

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(group = "com.github.javaparser", name = "javaparser-symbol-solver-core", version = "3.24.7")
        classpath(group = "com.google.googlejavaformat", name = "google-java-format", version = "1.15.0")
    }
}

tasks.register("generateMetrics") {
    doLast {
        generatePlatformMetricsClass("bukkit", false)
        generatePlatformMetricsClass("bungeecord", false)
        generatePlatformMetricsClass("sponge", false)
        generatePlatformMetricsClass("velocity", true)
    }
}

/**
 * Generates the Metrics class for the given platform.
 */
fun generatePlatformMetricsClass(platform: String, withMetricsConfig: Boolean) =
        file("${project(":$platform").buildDir}/generated/Metrics.java")
                .apply { parentFile.mkdirs() }
                .writeText(enrichPlatformMetricsClass(platform, withMetricsConfig))

/**
 * Enriches the platform Metrics class with all dependent classes as inner classes.
 */
fun enrichPlatformMetricsClass(platform: String, withMetricsConfig: Boolean): String {
    val metrics = file("${platform}/src/main/java/org/bstats/${platform}/Metrics.java").readText()
            .let {
                convertClassToInnerClass(it, file("base/src/main/java/org/bstats/MetricsBase.java").readText())
            }.let {
                fileTree("base/src/main/java/org/bstats/charts").files
                        .map { file -> file.readText() }
                        .fold(it) { acc, file -> convertClassToInnerClass(acc, file) }
            }.let {
                convertClassToInnerClass(it, file("base/src/main/java/org/bstats/json/JsonObjectBuilder.java").readText())
            }.let {
                if (withMetricsConfig) {
                    convertClassToInnerClass(it, file("base/src/main/java/org/bstats/config/MetricsConfig.java").readText())
                } else it
            }

    val sourceCode = Formatter().formatSource(metrics)
    val header = """
        /*
         * This Metrics class was auto-generated and can be copied into your project if you are
         * not using a build tool like Gradle or Maven for dependency management.
         *
         * IMPORTANT: You are not allowed to modify this class, except changing the package.
         *
         * Disallowed modifications include but are not limited to:
         *  - Remove the option for users to opt-out
         *  - Change the frequency for data submission
         *  - Obfuscate the code (every obfuscator should allow you to make an exception for specific files)
         *  - Reformat the code (if you use a linter, add an exception)
         *
         * Violations will result in a ban of your plugin and account from bStats.
         */
    """.trimIndent()
    return header + "\n" + sourceCode
}

/**
 * Takes the code of two class files (where `outer` is using `inner`) and adds the stand-alone
 * `inner` class as an inner class of the `outer` class.
 *
 * This method properly handles imports by combining the imports from the `outer` and `inner`,
 * removing duplicated and removing a potential import of `inner` from `outer`.
 *
 * @param outer The code of the class that should become the outer class.
 * @param inner The code of the class that should become the inner class.
 * @param makeInnerPrivate Whether or not the inner class should be made private.
 * @return The combined class.
 */
fun convertClassToInnerClass(outer: String, inner: String, makeInnerPrivate: Boolean = false): String {
    val outerCU = StaticJavaParser.parse(outer)
    val innerCU = StaticJavaParser.parse(inner)

    val outerClass = outerCU
            .findFirst(ClassOrInterfaceDeclaration::class.java)
            .orElseThrow { AssertionError() }
    val innerClass = innerCU
            .findFirst(ClassOrInterfaceDeclaration::class.java)
            .orElseThrow { AssertionError() }
            .setStatic(true)

    if (makeInnerPrivate) {
        // It's necessary to remove the static modifier and re-add it afterwards. Otherwise it will
        // generate "static private" instead of "private static".
        innerClass.setStatic(false)
                .setPublic(false)
                .setPrivate(true)
                .setStatic(true)
    }

    // First remove the import to the inner class (if it exists)
    outerCU.imports.stream()
            .filter { it.nameAsString == innerClass.fullyQualifiedName.orElseThrow { AssertionError() } }
            .findFirst()
            .ifPresent { it.remove() }

    // Then add the inner class as a member to the outer class
    outerClass.addMember(innerClass)

    // And add all imports of the inner class to the outer class.
    // JavaParser already takes care for us, that nothing is imported twice
    innerCU.imports.forEach { outerCU.addImport(it) }

    return PrettyPrinter(
            PrettyPrinterConfiguration()
                    .setOrderImports(true)
                    .setEndOfLineCharacter("\n")
    ).print(outerCU)
}
