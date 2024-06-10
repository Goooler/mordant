package com.github.ajalt.mordant.internal.syscalls

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import com.github.ajalt.mordant.input.MouseTracking
import kotlin.time.Duration
import kotlin.time.TimeSource

internal abstract class SyscallHandlerWindows : SyscallHandler {
    private companion object {
        // https://learn.microsoft.com/en-us/windows/console/key-event-record-str
        const val RIGHT_ALT_PRESSED: UInt = 0x0001u
        const val LEFT_ALT_PRESSED: UInt = 0x0002u
        const val RIGHT_CTRL_PRESSED: UInt = 0x0004u
        const val LEFT_CTRL_PRESSED: UInt = 0x0008u
        const val SHIFT_PRESSED: UInt = 0x0010u
        val CTRL_PRESSED_MASK = (RIGHT_CTRL_PRESSED or LEFT_CTRL_PRESSED)
        val ALT_PRESSED_MASK = (RIGHT_ALT_PRESSED or LEFT_ALT_PRESSED)

        // https://learn.microsoft.com/en-us/windows/console/mouse-event-record-str
        const val MOUSE_MOVED: UInt = 0x0001u
        const val DOUBLE_CLICK: UInt = 0x0002u
        const val MOUSE_WHEELED: UInt = 0x0004u
        const val MOUSE_HWHEELED: UInt = 0x0008u
        const val FROM_LEFT_1ST_BUTTON_PRESSED: UInt = 0x0001u
        const val RIGHTMOST_BUTTON_PRESSED: UInt = 0x0002u
        const val FROM_LEFT_2ND_BUTTON_PRESSED: UInt = 0x0004u
        const val FROM_LEFT_3RD_BUTTON_PRESSED: UInt = 0x0008u
        const val FROM_LEFT_4TH_BUTTON_PRESSED: UInt = 0x0010u


        // https://learn.microsoft.com/en-us/windows/console/setconsolemode
        const val ENABLE_PROCESSED_INPUT = 0x0001u
        const val ENABLE_MOUSE_INPUT = 0x0010u
        const val ENABLE_EXTENDED_FLAGS = 0x0080u
        const val ENABLE_WINDOW_INPUT = 0x0008u
        const val ENABLE_QUICK_EDIT_MODE = 0x0040u
    }

    protected sealed class EventRecord {
        data class Key(
            val bKeyDown: Boolean,
            val wVirtualKeyCode: UShort,
            val uChar: Char,
            val dwControlKeyState: UInt,
        ) : EventRecord()

        data class Mouse(
            val dwMousePositionX: Int,
            val dwMousePositionY: Int,
            val dwButtonState: UInt,
            val dwControlKeyState: UInt,
            val dwEventFlags: UInt,
        ) : EventRecord()
    }

    protected abstract fun readRawEvent(dwMilliseconds: Int): EventRecord?
    protected abstract fun getStdinConsoleMode(): UInt?
    protected abstract fun setStdinConsoleMode(dwMode: UInt): Boolean

    final override fun enterRawMode(mouseTracking: MouseTracking): AutoCloseable? {
        val originalMode = getStdinConsoleMode() ?: return null
        // dwMode=0 means ctrl-c processing, echo, and line input modes are disabled. Could add
        // ENABLE_PROCESSED_INPUT or ENABLE_WINDOW_INPUT if we want those events.
        val dwMode = when (mouseTracking) {
            MouseTracking.Off -> 0u
            else -> ENABLE_MOUSE_INPUT or ENABLE_EXTENDED_FLAGS
        }
        if (!setStdinConsoleMode(dwMode)) return null
        return AutoCloseable { setStdinConsoleMode(originalMode) }
    }

    override fun readInputEvent(timeout: Duration, mouseTracking: MouseTracking): SysInputEvent {
        val t0 = TimeSource.Monotonic.markNow()
        val dwMilliseconds = (timeout - t0.elapsedNow()).inWholeMilliseconds
            .coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        return when (val event = readRawEvent(dwMilliseconds)) {
            null -> SysInputEvent.Fail
            is EventRecord.Key -> processKeyEvent(event)
            is EventRecord.Mouse -> processMouseEvent(event, mouseTracking)
        }
    }

    private fun processKeyEvent(event: EventRecord.Key): SysInputEvent {
        if (!event.bKeyDown) return SysInputEvent.Retry // ignore key up events
        val virtualName = WindowsVirtualKeyCodeToKeyEvent.getName(event.wVirtualKeyCode)
        val shift = event.dwControlKeyState and SHIFT_PRESSED != 0u
        val key = when {
            virtualName != null && virtualName.length == 1 && shift -> {
                if (virtualName[0] in 'a'..'z') virtualName.uppercase()
                else shiftVcodeToKey(virtualName)
            }

            virtualName != null -> virtualName
            event.uChar.code != 0 -> event.uChar.toString()
            else -> "Unidentified"
        }
        return SysInputEvent.Success(
            KeyboardEvent(
                key = key,
                ctrl = event.dwControlKeyState and CTRL_PRESSED_MASK != 0u,
                alt = event.dwControlKeyState and ALT_PRESSED_MASK != 0u,
                shift = shift,
            )
        )
    }

    private fun processMouseEvent(
        event: EventRecord.Mouse,
        tracking: MouseTracking,
    ): SysInputEvent {
        val eventFlags = event.dwEventFlags
        val buttons = event.dwButtonState
        if (tracking == MouseTracking.Off
            || tracking == MouseTracking.Normal && eventFlags == MOUSE_MOVED
            || tracking == MouseTracking.Button && eventFlags == MOUSE_MOVED && buttons == 0u
        ) return SysInputEvent.Retry
        return SysInputEvent.Success(
            MouseEvent(
                x = event.dwMousePositionX,
                y = event.dwMousePositionY,
                // TODO: mouse wheel events
                buttons = buttons.toInt(), // Windows uses the same flags for buttons as browsers do
                ctrl = event.dwControlKeyState and CTRL_PRESSED_MASK != 0u,
                alt = event.dwControlKeyState and ALT_PRESSED_MASK != 0u,
                shift = event.dwControlKeyState and SHIFT_PRESSED != 0u,
            )
        )
    }
}

private fun shiftVcodeToKey(virtualName: String): String {
    return when (virtualName[0]) {
        '1' -> "!"
        '2' -> "@"
        '3' -> "#"
        '4' -> "$"
        '5' -> "%"
        '6' -> "^"
        '7' -> "&"
        '8' -> "*"
        '9' -> "("
        '0' -> ")"
        '-' -> "_"
        '=' -> "+"
        '`' -> "~"
        '[' -> "{"
        ']' -> "}"
        '\\' -> "|"
        ';' -> ":"
        '\'' -> "\""
        ',' -> "<"
        '.' -> ">"
        '/' -> "?"
        else -> virtualName
    }
}
