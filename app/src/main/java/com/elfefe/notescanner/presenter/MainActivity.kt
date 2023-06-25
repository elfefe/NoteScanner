package com.elfefe.notescanner.presenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.elfefe.notescanner.ui.view.Main
import com.elfefe.notescanner.ui.theme.NoteScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val _extractPicsFlow = MutableStateFlow<MutableMap<String, List<String>>>(mutableMapOf())

    fun extractPics(name: String) = _extractPicsFlow.value[name]
    fun notifiedExtractPics(onUpdate: (MutableMap<String, List<String>>) -> Unit) {
        lifecycleScope.launch(Dispatchers.Default) {
            _extractPicsFlow.collect {
                withContext(Dispatchers.Main) {
                    onUpdate(it)
                }
            }
        }
    }
    fun addExtractPics(name: String, content: List<String>) {
        val current = _extractPicsFlow.value
        current[name] = content
        _extractPicsFlow.value = current
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Main()
                }
            }
        }
    }
}