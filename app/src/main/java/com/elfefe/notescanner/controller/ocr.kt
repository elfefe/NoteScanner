package com.elfefe.notescanner.controller

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.InternalAPI
import io.ktor.util.decodeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(InternalAPI::class)
fun extractData(scope: CoroutineScope, file: File, onData: (String) -> Unit) {
    scope.launch(Dispatchers.IO) {
        val client = HttpClient(CIO) {
            install(HttpTimeout)
        }
        val response =
            client.post("http://82.65.136.91:5000/extract?Authorization=$authorization") {
                body = file.readBytes()
                contentType(ContentType.parse("image/x-png"))
                header("filename", file.name)
                timeout { requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS }
            }
        response.content.read {
            onData(it.decodeString())
        }
        client.close()
    }
}

val authorization: String
    get() = File(
        com.elfefe.notescanner.presenter.Application.instance.filesDir,
        "Authorization"
    ).readText()