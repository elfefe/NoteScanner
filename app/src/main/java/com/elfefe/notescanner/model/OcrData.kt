package com.elfefe.notescanner.model

import android.os.Parcelable
import com.elfefe.notescanner.controller.databaseFile
import com.elfefe.notescanner.controller.getImage
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

    val newName: String
        get() {
            val split = name.split(newNameSeparator)
            return if (split.size > 1) "${split[0]}$newNameSeparator${split[1].toInt() + 1}"
            else "${split[0]}${newNameSeparator}1"
        }

    fun toText() = texts.joinToString("\n")
    fun write(text: List<String>) = getText(textName).writeText(text.joinToString("\n"))
    fun delete() {
        getText(textName).delete()
        getImage(imageName).delete()
        databaseFile.writeText(databaseFile.readText().replace("$name\n", ""))
    }
}

val newNameSeparator = "+"
