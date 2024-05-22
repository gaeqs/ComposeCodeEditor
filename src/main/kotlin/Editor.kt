import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun Editor(state: EditorState, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val lazyState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.onPreviewKeyEvent { state.keyEvent(state, clipboard, it) },
        state = lazyState
    ) {
        items(state.lines.size) { index ->
            EditorLine(state, state.lines[index], index)
        }
    }
    LaunchedEffect(state.selection) {
        val row = state.selection.to.row
        if (lazyState.layoutInfo.visibleItemsInfo.none { it.index == row }) {
            lazyState.scrollToItem(row)
        }
        state.lines[row].requester.requestFocus()
    }
}

@Composable
private fun EditorLine(state: EditorState, line: Line, index: Int) {
    BasicTextField(
        value = TextFieldValue(
            text = line.text,
            selection = state.selection.lineSelection(index, line.text)
        ),
        onValueChange = {
            state.lines[index] = Line(it.text)
            state.selection = CaretSelection(
                CaretPosition(index, it.selection.start),
                CaretPosition(index, it.selection.end)
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().focusRequester(line.requester)
    )
}