package com.todogroup

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import java.awt.Point
import javax.swing.Icon

class TodoEditorAction : AnAction() {
    private var isEnabled = false
    private val todoNotes = mutableMapOf<String, String>()
    private val openPopups = mutableMapOf<String, PopupData>()
    private var popupCounter = 1
    private val popupIndexMap = mutableMapOf<String, Int>()
    private var hoverListener: TodoHoverListener? = null

    override fun actionPerformed(event: AnActionEvent) {
        isEnabled = !isEnabled
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        if (isEnabled) {
            addTodoHoverListener(editor)
        } else {
            closeAllPopups()
            removeTodoHoverListener(editor)
        }
        update(event)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.text = if (isEnabled) "Disable Plugin" else "Enable Plugin"
        event.presentation.icon = if (isEnabled) ENABLED_ICON else DISABLED_ICON
    }

    private fun addTodoHoverListener(editor: Editor) {
        hoverListener = TodoHoverListener(editor, todoNotes, openPopups, popupIndexMap, popupCounter)
        editor.addEditorMouseMotionListener(hoverListener!!)
    }

    private fun removeTodoHoverListener(editor: Editor) {
        hoverListener?.let { editor.removeEditorMouseMotionListener(it) }
        hoverListener = null
    }

    private fun closeAllPopups() {
        openPopups.values.forEach { it.popup.cancel() }
        openPopups.clear()
    }

    companion object {
        val ENABLED_ICON: Icon = AllIcons.General.InspectionsOK
        val DISABLED_ICON: Icon = AllIcons.Actions.Cancel
    }

    data class PopupData(val popup: JBPopup, var position: Point)
}
