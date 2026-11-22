package com.phpnuxbill.mobile.core.network

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonElement?.asObjOrNull(): JsonObject? =
    if (this != null && isJsonObject) asJsonObject else null

fun JsonElement?.asArrOrNull(): JsonArray? =
    if (this != null && isJsonArray) asJsonArray else null

fun JsonObject?.str(key: String): String {
    if (this == null || !has(key) || get(key).isJsonNull) return ""
    return runCatching { get(key).asString.trim() }.getOrDefault("")
}

fun JsonObject?.intVal(key: String): Int {
    if (this == null || !has(key) || get(key).isJsonNull) return 0
    return runCatching { get(key).asInt }
        .getOrElse { str(key).toIntOrNull() ?: 0 }
}

fun JsonObject?.longVal(key: String): Long {
    if (this == null || !has(key) || get(key).isJsonNull) return 0L
    return runCatching { get(key).asLong }
        .getOrElse { str(key).toLongOrNull() ?: 0L }
}

fun JsonObject?.doubleVal(key: String): Double {
    if (this == null || !has(key) || get(key).isJsonNull) return 0.0
    return runCatching { get(key).asDouble }
        .getOrElse { str(key).toDoubleOrNull() ?: 0.0 }
}

fun JsonObject?.boolVal(key: String): Boolean {
    if (this == null || !has(key) || get(key).isJsonNull) return false
    return runCatching {
        val prim = get(key).asJsonPrimitive
        when {
            prim.isBoolean -> prim.asBoolean
            prim.isNumber -> prim.asInt != 0
            else -> prim.asString.equals("true", true) || prim.asString == "1" || prim.asString.equals("yes", true)
        }
    }.getOrDefault(false)
}

fun JsonArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return mapNotNull { el ->
        if (el.isJsonPrimitive) el.asString.trim().takeIf { it.isNotBlank() } else null
    }
}
