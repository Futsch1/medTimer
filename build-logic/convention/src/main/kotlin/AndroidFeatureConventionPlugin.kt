import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "medtimer.android.library")
            apply(plugin = "medtimer.compose.library")
            apply(plugin = "medtimer.android.hilt")

            dependencies {
                "implementation"(project(":core:database"))
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:preferences"))
            }
        }
    }
}
