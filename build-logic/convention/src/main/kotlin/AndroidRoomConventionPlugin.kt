import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                add("annotationProcessor", libs.findLibrary("androidx-room-compiler").get())
                add("implementation", libs.findLibrary("androidx-room-runtime").get())
            }
        }
    }
}
