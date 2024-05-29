import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File


val testText by lazy {
    val file = File("build.gradle.kts").absoluteFile
    return@lazy file.readText()
}

@Composable
@Preview
fun App() {
    val state by remember { mutableStateOf(EditorState().apply { setText(testText) }) }

    MaterialTheme(darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Editor(state)

        }
    }
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
