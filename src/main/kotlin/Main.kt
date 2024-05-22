import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File


val testText by lazy {
    val file = File("/home/gaeqs/IdeaProjects/CodeTest/build.gradle.kts")
    return@lazy file.readText()
}

@Composable
@Preview
fun App() {
    val state by remember { mutableStateOf(EditorState().apply { setText(testText) }) }

    MaterialTheme {
        Surface {
            Editor(state)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
