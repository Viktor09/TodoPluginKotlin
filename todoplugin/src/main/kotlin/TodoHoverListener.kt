package com.todogroup

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JScrollPane
import javax.swing.JTextArea

class TodoHoverListener(
    private val editor: Editor,
    private val todoNotes: MutableMap<String, String>,
    private val openPopups: MutableMap<String, TodoEditorAction.PopupData>,
    private val popupIndexMap: MutableMap<String, Int>,
    private var popupCounter: Int
) : EditorMouseMotionListener {

    override fun mouseMoved(e: EditorMouseEvent) {
        val document = editor.document
        val offset = e.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))

        val todoRegex = Regex("""// TODO\(\)\s(\d+)""")
        val matchResult = todoRegex.find(lineText)

        if (matchResult != null) {
            val todoKey = matchResult.value
            val numberMatch = matchResult.groups[1]?.range

            if (numberMatch != null) {
                val numberOffset = lineStartOffset + numberMatch.start
                showEditablePopup(e, todoKey, numberOffset)
            }
        }
    }

    private fun showEditablePopup(e: EditorMouseEvent, todoKey: String, positionOffset: Int) {
        val existingPopupData = openPopups[todoKey]
        if (existingPopupData != null) {
            existingPopupData.popup.setRequestFocus(true)
            return
        }

        val popupIndex = popupIndexMap.getOrPut(todoKey) {
            popupCounter++
            popupCounter - 1
        }
        val popupTitle = "TODO #$popupIndex Notes"
        val existingNote = todoNotes[todoKey] ?: ""
        val textArea = JTextArea(existingNote, 10, 20).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPane = JScrollPane(textArea)

        closePopupWithEnter(textArea, todoKey, e)
        val popup: JBPopup = jbPopup(scrollPane, textArea, popupTitle, todoKey)

        val point = existingPopupData?.position ?: e.editor.offsetToXY(positionOffset)
        popup.showInScreenCoordinates(e.editor.contentComponent, point)
        openPopups[todoKey] = TodoEditorAction.PopupData(popup, point)
    }

    private fun jbPopup(
        scrollPane: JScrollPane,
        textArea: JTextArea,
        popupTitle: String,
        todoKey: String
    ): JBPopup {
        val popup: JBPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, textArea)
            .setTitle(popupTitle)
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .setCancelCallback {
                todoNotes[todoKey] = textArea.text
                openPopups.remove(todoKey)
                true
            }
            .createPopup()
        return popup
    }

    private fun closePopupWithEnter(
        textArea: JTextArea,
        todoKey: String,
        e: EditorMouseEvent
    ) {
        textArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(keyEvent: KeyEvent) {
                if (keyEvent.keyCode == KeyEvent.VK_ENTER && !keyEvent.isShiftDown) {
                    todoNotes[todoKey] = textArea.text
                    openPopups[todoKey]?.popup?.cancel()
                    openPopups.remove(todoKey)
                    e.editor.contentComponent.requestFocus()
                }
            }
        })
    }
}
