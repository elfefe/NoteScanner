package com.elfefe.notescanner.controller

import android.os.Looper
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.util.concurrent.Executor


val executorMain: Executor
    get() = HandlerExecutor(Looper.getMainLooper())