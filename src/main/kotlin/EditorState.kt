import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.min

data class CaretPosition(val row: Int = 0, val column: Int = 0) {
    operator fun compareTo(o: CaretPosition): Int {
        val r = row.compareTo(o.row)
        if (r == 0) return column.compareTo(o.column)
        return r
    }
}

data class CaretSelection(val from: CaretPosition, val to: CaretPosition) {

    constructor(position: CaretPosition = CaretPosition()) : this(position, position) {
    }

    fun isSingleLine() = from.row == to.row

    fun isLineSelected(i: Int): Boolean {
        return i >= from.row && i <= to.row
    }

    fun lineSelection(i: Int, text: String): TextRange {
        return when {
            i < from.row || i > to.row -> TextRange.Zero
            i == from.row && i == to.row -> TextRange(from.column, to.column)
            i == from.row -> TextRange(from.column, text.length)
            i == to.row -> TextRange(0, to.column)
            else -> TextRange(0, text.length)
        }
    }

    fun normalized(): CaretSelection {
        return if (from < to) this else CaretSelection(to, from)
    }
}

data class Line(val text: String) {
    val requester = FocusRequester()
    var layout: TextLayoutResult? = null
    var coordinates: LayoutCoordinates? = null

    fun getCharAtPosition(position: Offset) = layout?.getOffsetForPosition(position)

}

class EditorState {
    private var _selection by mutableStateOf(CaretSelection())

    var selection: CaretSelection
        get() = _selection
        set(value) {
            _selection = value
            normalizedSelection = _selection.normalized()
        }

    var normalizedSelection = CaretSelection()
        private set

    var currentHoveredElement by mutableStateOf(Pair(0, Offset(0.0f, 0.0f)))
    val keyEvent: (EditorState, ClipboardManager, KeyEvent) -> Boolean = ::onKeyEvent

    val lines = mutableStateListOf<Line>(Line(""))

    fun getText(lineSeparator: String = "\n") = lines.joinToString(separator = lineSeparator) { it.text }

    fun setText(text: String) {
        lines.clear()
        if (text.isEmpty()) {
            lines.add(Line(""))
            return
        }

        text.lines().forEach { lines.add(Line(it)) }
    }

    fun clearSelected() {
        val selection = normalizedSelection
        val firstLine = selection.from.row

        selection.lineSelection(firstLine, lines[firstLine].text).let {
            lines[firstLine] = Line(lines[firstLine].text.removeRange(it.start, it.end))
        }

        if (!selection.isSingleLine()) {
            val lastLine = selection.to.row
            selection.lineSelection(lastLine, lines[lastLine].text).let {
                lines[lastLine] = Line(lines[lastLine].text.removeRange(it.start, it.end))
            }
        }

        if (selection.from.row < selection.to.row) {
            lines.removeRange(selection.from.row + 1, selection.to.row)
        }

        println(selection.from)
        this.selection = CaretSelection(selection.from)
    }

    fun newLine() {
        clearSelected()
        val pos = selection.from
        val line = lines[pos.row].text
        val first = line.substring(0, pos.column)
        val second = line.substring(pos.column)
        lines[pos.row] = Line(first)
        lines.add(pos.row + 1, Line(second))
        selection = CaretSelection(CaretPosition(pos.row + 1, 0))
    }

    fun moveUp() {
        val from = selection.from
        if (from.row == 0) return
        val text = lines[from.row - 1].text
        selection = CaretSelection(CaretPosition(from.row - 1, min(text.length, from.column)))
    }

    fun moveDown() {
        val from = selection.to
        if (from.row == lines.size - 1) return
        val text = lines[from.row + 1].text
        selection = CaretSelection(CaretPosition(from.row + 1, min(text.length, from.column)))
    }

    fun moveToStart() {
        selection = CaretSelection(CaretPosition(0, 0))
    }

    fun moveToEnd() {
        val text = lines.last().text
        selection = CaretSelection(CaretPosition(lines.size - 1, text.length))
    }

    fun paste(clipboard: ClipboardManager) {
        if (clipboard.hasText()) {
            val clipboardLines = clipboard.getText()?.lines()?.toMutableList() ?: return
            if (clipboardLines.isEmpty()) return
            clearSelected()

            val selection = selection.normalized()

            val firstLine = lines[selection.from.row].text
            val start = firstLine.substring(0, selection.from.column)
            val end = firstLine.substring(selection.from.column)

            clipboardLines[0] = start + clipboardLines[0]
            clipboardLines[clipboardLines.size - 1] = clipboardLines[clipboardLines.size - 1] + end

            lines.removeAt(selection.from.row)
            lines.addAll(selection.from.row, clipboardLines.map { Line(it) })
        }
    }
}

private fun onKeyEvent(state: EditorState, clipboard: ClipboardManager, event: KeyEvent): Boolean {
    if (event.isTypedEvent) {
        state.clearSelected()
    }
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key == Key.Enter) {
        state.newLine()
        return true
    }
    if (event.key == Key.DirectionUp) {
        state.moveUp()
        return true
    }
    if (event.key == Key.DirectionDown) {
        state.moveDown()
        return true
    }
    if (event.key == Key.PageUp) {
        state.moveToStart()
        return true
    }

    if (event.key == Key.PageDown) {
        state.moveToEnd()
        return true
    }
    if (event.key == Key.V && event.isCtrlPressed) {
        state.paste(clipboard)
        return true
    }
    return false
}
