package com.cd.missedcallpro.data

data class FieldTypeUi(val code: String, val label: String)

val FieldTypes = listOf(
    FieldTypeUi("text", "Text"),
    FieldTypeUi("number", "Number"),
    FieldTypeUi("integer", "Integer"),
    FieldTypeUi("mc", "Multiple choice"),
    FieldTypeUi("phone", "Phone number"),
)