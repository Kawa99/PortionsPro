package com.example.mob_dev_portfolio.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SchemaRecipeParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val scriptTagRegex = Regex(
        """<script\b(?=[^>]*\btype\s*=\s*["']application/ld\+json["'])[^>]*>(.*?)</script>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    fun parse(html: String): SchemaRecipeDto? {
        return scriptTagRegex.findAll(html)
            .mapNotNull { extractRecipeFromBlock(it.groupValues[1].trim()) }
            .firstOrNull()
    }

    fun extractServings(dto: SchemaRecipeDto): Int {
        val yield = dto.recipeYield ?: return 4
        val text = firstText(yield) ?: return 4
        return Regex("""\d+""").find(text)?.value?.toIntOrNull()?.coerceAtLeast(1) ?: 4
    }

    fun extractInstructions(dto: SchemaRecipeDto): String {
        val raw = dto.recipeInstructions ?: return ""
        return extractInstructionElements(raw)
            .mapIndexed { index, step -> "${index + 1}. $step" }
            .joinToString("\n")
    }

    fun extractImageUrl(dto: SchemaRecipeDto): String {
        return dto.image?.let(::extractImageUrl) ?: ""
    }

    fun parseDurationMinutes(iso: String): Int {
        if (iso.isBlank()) return 0
        val hours = Regex("""(\d+)H""").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("""(\d+)M""").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    private fun extractRecipeFromBlock(block: String): SchemaRecipeDto? {
        return runCatching {
            val root = json.parseToJsonElement(block)
            extractRecipeElement(root)?.let(::toDto)
        }.getOrNull()
    }

    private fun extractRecipeElement(element: JsonElement): JsonObject? {
        return when (element) {
            is JsonObject -> extractRecipeFromObject(element)
            is JsonArray -> element.firstNotNullOfOrNull { extractRecipeElement(it) }
            else -> null
        }
    }

    private fun extractRecipeFromObject(obj: JsonObject): JsonObject? {
        if (hasRecipeType(obj["@type"])) return obj

        obj["@graph"]?.let { graph ->
            extractRecipeElement(graph)?.let { return it }
        }

        obj["mainEntity"]?.let { entity ->
            extractRecipeElement(entity)?.let { return it }
        }

        return null
    }

    private fun hasRecipeType(type: JsonElement?): Boolean {
        return when (type) {
            is JsonPrimitive -> type.contentOrNull?.isRecipeType() == true
            is JsonArray -> type.any { hasRecipeType(it) }
            else -> false
        }
    }

    private fun String.isRecipeType(): Boolean {
        return trim().substringAfterLast('/').substringAfterLast('#')
            .equals("Recipe", ignoreCase = true)
    }

    private fun toDto(obj: JsonObject): SchemaRecipeDto {
        return SchemaRecipeDto(
            name = obj.text("name"),
            recipeYield = obj["recipeYield"],
            prepTime = obj.text("prepTime"),
            cookTime = obj.text("cookTime"),
            recipeCategory = textOrFirst(obj["recipeCategory"]),
            recipeCuisine = textOrFirst(obj["recipeCuisine"]),
            image = obj["image"],
            recipeIngredient = obj["recipeIngredient"]?.asStringList().orEmpty(),
            recipeInstructions = obj["recipeInstructions"],
            description = obj.text("description")
        )
    }

    private fun JsonObject.text(key: String): String {
        return textOrFirst(this[key])
    }

    private fun textOrFirst(element: JsonElement?): String {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull.orEmpty().trim()
            is JsonArray -> element.firstNotNullOfOrNull { firstText(it) }.orEmpty().trim()
            is JsonObject -> element["name"]?.let(::firstText).orEmpty().trim()
            else -> ""
        }
    }

    private fun firstText(element: JsonElement): String? {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            is JsonArray -> element.firstNotNullOfOrNull { firstText(it) }
            is JsonObject -> {
                element["text"]?.let(::firstText)
                    ?: element["name"]?.let(::firstText)
                    ?: element["url"]?.let(::firstText)
            }
        }
    }

    private fun JsonElement.asStringList(): List<String> {
        return when (this) {
            is JsonArray -> mapNotNull { firstText(it) }
            is JsonPrimitive -> contentOrNull?.lines().orEmpty()
            is JsonObject -> emptyList()
        }.map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun extractInstructionElements(element: JsonElement): List<String> {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull
                ?.split(Regex("""\r?\n+"""))
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            is JsonArray -> element.flatMap { extractInstructionElements(it) }
            is JsonObject -> extractInstructionObject(element)
        }
    }

    private fun extractInstructionObject(obj: JsonObject): List<String> {
        val type = obj["@type"]
        val nested = obj["itemListElement"] ?: obj["steps"]
        if (hasType(type, "HowToSection") && nested != null) {
            return extractInstructionElements(nested)
        }

        val text = firstText(obj["text"] ?: JsonNull)?.takeIf { it.isNotBlank() }
        if (text != null) return listOf(text)

        return nested?.let(::extractInstructionElements).orEmpty()
    }

    private fun hasType(type: JsonElement?, expected: String): Boolean {
        return when (type) {
            is JsonPrimitive -> type.contentOrNull
                ?.trim()
                ?.substringAfterLast('/')
                ?.substringAfterLast('#')
                ?.equals(expected, ignoreCase = true) == true

            is JsonArray -> type.any { hasType(it, expected) }
            else -> false
        }
    }

    private fun extractImageUrl(element: JsonElement): String {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull.orEmpty()
            is JsonArray -> element.firstOrNull()?.let(::extractImageUrl).orEmpty()
            is JsonObject -> {
                firstText(element["url"] ?: JsonNull)
                    ?: firstText(element["contentUrl"] ?: JsonNull)
                    ?: ""
            }
        }.trim()
    }

    @Suppress("unused")
    private fun JsonPrimitive.debugValue(): Any? {
        return contentOrNull ?: doubleOrNull ?: booleanOrNull
    }
}
