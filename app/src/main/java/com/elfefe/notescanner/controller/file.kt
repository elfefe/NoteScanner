package com.elfefe.notescanner.controller

import com.elfefe.notescanner.model.OcrData
import com.elfefe.notescanner.presenter.Application
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val imagesDir: File
    get() = File(Application.instance.filesDir, "images").apply { mkdirs() }

val textsDir: File
    get() = File(Application.instance.filesDir, "texts").apply { mkdirs() }

val databaseFile: File
    get() = File(Application.instance.filesDir, "database").apply { createNewFile() }

val ocrDatabase: List<OcrData>
    get() = databaseFile.readText().let { json ->
        if (json.isEmpty()) listOf()
        else json.split("\n").filter { it.isNotBlank() }.map { OcrData(it) }
    }

fun getImage(name: String) = File(imagesDir, name).apply { createNewFile() }
fun getText(name: String) = File(textsDir, name).apply { createNewFile() }

suspend fun addData(name: String, texts: List<String>) {
    OcrData(name).write(texts)
    databaseFile.appendText("$name\n")
}

val imageName: String
    get() = "${
        LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy_AAAAAAAA")
        )
    }.png"