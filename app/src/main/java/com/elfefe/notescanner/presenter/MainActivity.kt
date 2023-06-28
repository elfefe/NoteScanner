package com.elfefe.notescanner.presenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.elfefe.notescanner.controller.addData
import com.elfefe.notescanner.controller.onMain
import com.elfefe.notescanner.ui.composable.Main
import com.elfefe.notescanner.ui.theme.NoteScannerTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    var notifiedExtractPics: CoroutineScope.() -> Unit = {}

    private val isUpdatingDatabase = AtomicBoolean(false)
    private val updateQueue = ConcurrentHashMap<String, String>()

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

    fun updateDatabase(image: String, data: String) {
        println("updateDatabase: $image")
        updateQueue[image] = data
        lifecycleScope.launch(Dispatchers.IO) {
            if (!isUpdatingDatabase.getAndSet(true))
                try {
                    onMain { println("Empty queue: ${updateQueue.isNotEmpty()}") }
                    while (updateQueue.isNotEmpty())
                        updateQueue.forEach { (image, data) ->
                            onMain { println("remove: ${updateQueue.remove(image)}") }

                            addData(image, Gson().fromJson(
                                data, object : TypeToken<List<String>>() {}.type
                            ))

                            notifiedExtractPics()
                        }
                } catch (e: Exception) { onMain { e.printStackTrace() } }
            isUpdatingDatabase.set(false)
        }
    }
}