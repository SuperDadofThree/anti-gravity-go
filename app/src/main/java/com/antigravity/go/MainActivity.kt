package com.antigravity.go

import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.antigravity.go.theme.AntigravityGoTheme
import com.antigravity.go.ui.ContainerScreen
import com.antigravity.go.web.WebContainerState

class MainActivity : ComponentActivity() {

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var onAccountSelectedCallback: ((String) -> Unit)? = null

    // Register file chooser activity result launcher
    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (fileUploadCallback == null) return@registerForActivityResult

            val results: Array<Uri>? = when {
                result.resultCode == RESULT_OK && result.data != null -> {
                    val data = result.data
                    val clipData = data?.clipData
                    if (clipData != null) {
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else {
                        data?.data?.let { arrayOf(it) }
                    }
                }
                else -> null
            }

            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }

    // Register device Google Account picker launcher
    private val accountPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (!accountName.isNullOrBlank()) {
                    onAccountSelectedCallback?.invoke(accountName)
                }
            }
            onAccountSelectedCallback = null
        }

    fun promptDeviceGoogleAccount(onSelected: (String) -> Unit) {
        onAccountSelectedCallback = onSelected
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
            accountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Google account picker: ${e.message}", Toast.LENGTH_SHORT).show()
            onAccountSelectedCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetUrl = intent?.data?.toString() ?: WebContainerState.DEFAULT_URL

        setContent {
            AntigravityGoTheme {
                ContainerScreen(
                    initialUrl = targetUrl,
                    onFileChooser = { callback, params ->
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = callback

                        try {
                            val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            filePickerLauncher.launch(intent)
                            true
                        } catch (e: Exception) {
                            fileUploadCallback?.onReceiveValue(null)
                            fileUploadCallback = null
                            false
                        }
                    },
                    onRequestAccountPicker = { onSelected ->
                        promptDeviceGoogleAccount(onSelected)
                    },
                    onExitApp = {
                        finish()
                    }
                )
            }
        }
    }
}
