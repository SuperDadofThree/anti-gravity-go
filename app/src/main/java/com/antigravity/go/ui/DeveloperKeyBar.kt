package com.antigravity.go.ui

import android.view.KeyEvent
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.go.theme.DarkBorder
import com.antigravity.go.theme.DarkSurfaceVariant

@Composable
fun DeveloperKeyBar(
    webView: WebView?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Function to send keycode
            fun sendKeyCode(keyCode: Int, metaState: Int = 0) {
                webView?.let { wv ->
                    wv.requestFocus()
                    val down = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
                    val up = KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState)
                    wv.dispatchKeyEvent(down)
                    wv.dispatchKeyEvent(up)
                }
            }

            // Function to insert text directly into activeElement
            fun insertText(char: String) {
                webView?.let { wv ->
                    val escaped = char.replace("\\", "\\\\").replace("'", "\\'")
                    val js = """
                        (function() {
                            var el = document.activeElement;
                            if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                                var start = el.selectionStart;
                                var end = el.selectionEnd;
                                var val = el.value;
                                el.value = val.substring(0, start) + '$escaped' + val.substring(end);
                                el.selectionStart = el.selectionEnd = start + 1;
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                            } else if (el && el.isContentEditable) {
                                document.execCommand('insertText', false, '$escaped');
                            }
                        })();
                    """.trimIndent()
                    wv.evaluateJavascript(js, null)
                }
            }

            DevKeyButton(label = "ESC") { sendKeyCode(KeyEvent.KEYCODE_ESCAPE) }
            DevKeyButton(label = "TAB") { sendKeyCode(KeyEvent.KEYCODE_TAB) }
            DevKeyButton(label = "CTRL+C") { sendKeyCode(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON) }
            DevKeyButton(label = "CTRL+V") { sendKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON) }
            DevKeyButton(label = "CTRL+Z") { sendKeyCode(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON) }
            DevKeyButton(label = "←") { sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT) }
            DevKeyButton(label = "→") { sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT) }
            DevKeyButton(label = "↑") { sendKeyCode(KeyEvent.KEYCODE_DPAD_UP) }
            DevKeyButton(label = "↓") { sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN) }
            DevKeyButton(label = "/") { insertText("/") }
            DevKeyButton(label = "\\") { insertText("\\") }
            DevKeyButton(label = "`") { insertText("`") }
            DevKeyButton(label = "{") { insertText("{") }
            DevKeyButton(label = "}") { insertText("}") }
            DevKeyButton(label = "[") { insertText("[") }
            DevKeyButton(label = "]") { insertText("]") }
            DevKeyButton(label = "|") { insertText("|") }
            DevKeyButton(label = "~") { insertText("~") }
            DevKeyButton(label = "$") { insertText("$") }
            DevKeyButton(label = "=") { insertText("=") }
        }
    }
}

@Composable
private fun DevKeyButton(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}
