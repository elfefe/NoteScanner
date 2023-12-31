package com.elfefe.notescanner.controller

import android.os.Looper
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

val executorMain: Executor
    get() = HandlerExecutor(Looper.getMainLooper())

val applicationScope: CoroutineScope
    get() = com.elfefe.notescanner.presenter.Application.instance.scope

suspend fun onMain(block: CoroutineScope.() -> Unit) =
    withContext(Dispatchers.Main) { block() }
