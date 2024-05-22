import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        Column {
            //Editor(state)
            Button(
                onClick = {

                }
            ) {
                Text("Hello World!")
            }
        }
    }
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
