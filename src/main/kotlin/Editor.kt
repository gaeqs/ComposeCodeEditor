import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp

fun lineAtOffset(state: EditorState, lazyListState: LazyListState, offset: Offset):
        Pair<Int, Offset>? {
    var index = lazyListState.firstVisibleItemIndex
    var currentLine: Line? = state.lines.getOrNull(index)

    while (
        currentLine != null &&
        currentLine.coordinates != null &&
        currentLine.coordinates?.isAttached == true
    ) {
        val bounds = currentLine.coordinates!!.boundsInParent()
        if (currentLine.coordinates!!.boundsInParent().contains(offset)) {
            return Pair(index, offset - bounds.topLeft)
        }
        ++index
        currentLine = state.lines.getOrNull(index)
    }

    return null
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Editor(state: EditorState, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val lazyState = rememberLazyListState()

    Box(
        modifier = modifier
            .onPreviewKeyEvent { state.keyEvent(state, clipboard, it) }
            .onPointerEvent(PointerEventType.Move, PointerEventPass.Initial) {
                val position = it.changes[0].position
                val pair = lineAtOffset(state, lazyState, position)
                if (pair != null) {
                    state.currentHoveredElement = pair
                }
            }
            .onPointerEvent(PointerEventType.Press, PointerEventPass.Main) {
                val hover = state.currentHoveredElement
                val line = state.lines[hover.first]
                val char = line.getCharAtPosition(hover.second) ?: return@onPointerEvent
                state.selection = CaretSelection(CaretPosition(hover.first, char))
            }.onPointerEvent(PointerEventType.Move, PointerEventPass.Main) {
                if (it.buttons.isPrimaryPressed) {
                    val hover = state.currentHoveredElement
                    val line = state.lines[hover.first]
                    val char = line.getCharAtPosition(hover.second) ?: return@onPointerEvent
                    state.selection = CaretSelection(
                        state.selection.from,
                        CaretPosition(hover.first, char)
                    )
                }
            }
    ) {
        LazyColumn(
            state = lazyState,
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
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EditorLine(state: EditorState, line: Line, index: Int) {
    BasicTextField(
        value = TextFieldValue(
            annotatedString = buildAnnotatedString {
                pushStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                )
                append(line.text)
            },
            selection = state.normalizedSelection.lineSelection(index, line.text)
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        onValueChange = {
            state.lines[index] = Line(it.text)
            if (state.selection.from == state.selection.to) {
                state.selection = CaretSelection(CaretPosition(index, it.selection.start))
            }
        },
        onTextLayout = {
            line.layout = it
        },
        minLines = 1,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth()
            .focusRequester(line.requester)
            .onGloballyPositioned {
                line.coordinates = it
            }
    )

}