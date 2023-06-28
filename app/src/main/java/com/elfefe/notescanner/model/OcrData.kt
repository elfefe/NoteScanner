package com.elfefe.notescanner.model

import android.os.Parcelable
import com.elfefe.notescanner.controller.getText
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrData(val name: String) : Parcelable {
    @IgnoredOnParcel
    val imageName: String = "$name.png"
    @IgnoredOnParcel
    val textName: String = "$name.txt"
    @IgnoredOnParcel
    val texts: List<String> = getText(textName).run {
        readText().run { if (isBlank()) listOf() else split("\n") }
    }
    fun toText() = texts.joinToString("\n")
    fun write(text: List<String>) = getText(textName).writeText(text.joinToString("\n"))
}
