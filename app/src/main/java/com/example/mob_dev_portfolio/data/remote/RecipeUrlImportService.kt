package com.example.mob_dev_portfolio.data.remote

import com.example.mob_dev_portfolio.data.Ingredient
import com.example.mob_dev_portfolio.data.Recipe
import com.example.mob_dev_portfolio.util.CanonicalUnit
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class RecipeUrlImportService @Inject constructor(
    private val client: OkHttpClient
) {
    private val noRedirectClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    sealed class ImportResult {
        data class Success(val recipe: Recipe, val ingredients: List<Ingredient>) : ImportResult()
        data object NoSchemaFound : ImportResult()
        data class NetworkError(val message: String) : ImportResult()
        data object InvalidUrl : ImportResult()
    }

    private sealed class HtmlFetchResult {
        data class Success(val html: String, val finalUrl: String) : HtmlFetchResult()
        data class Failure(val result: ImportResult) : HtmlFetchResult()
    }

    suspend fun importFromUrl(url: String): ImportResult = withContext(Dispatchers.IO) {
        val httpUrl = url.trim().toHttpUrlOrNull()
            ?: return@withContext ImportResult.InvalidUrl
        if (!httpUrl.isHttps) {
            return@withContext ImportResult.InvalidUrl
        }

        val (html, finalUrl) = try {
            when (val fetchResult = fetchHtml(httpUrl)) {
                is HtmlFetchResult.Success -> fetchResult.html to fetchResult.finalUrl
                is HtmlFetchResult.Failure -> return@withContext fetchResult.result
            }
        } catch (e: IOException) {
            return@withContext ImportResult.NetworkError(e.message ?: "Network request failed")
        } catch (e: IllegalArgumentException) {
            return@withContext ImportResult.InvalidUrl
        }

        val dto = SchemaRecipeParser.parse(html)
            ?: return@withContext ImportResult.NoSchemaFound

        val recipeId = UUID.randomUUID().toString()
        val recipe = Recipe(
            id = recipeId,
            name = dto.name.ifBlank { "Imported Recipe" },
            category = dto.recipeCategory,
            area = dto.recipeCuisine,
            instructions = SchemaRecipeParser.extractInstructions(dto),
            thumbnailUrl = SchemaRecipeParser.extractImageUrl(dto),
            sourceUrl = finalUrl,
            baseServings = SchemaRecipeParser.extractServings(dto),
            prepTimeMinutes = SchemaRecipeParser.parseDurationMinutes(dto.prepTime),
            cookTimeMinutes = SchemaRecipeParser.parseDurationMinutes(dto.cookTime),
            isUserCreated = true,
            isCached = false
        )

        val ingredients = dto.recipeIngredient.mapIndexedNotNull { index, raw ->
            parseIngredientString(raw, recipeId, index)
        }

        ImportResult.Success(recipe, ingredients)
    }

    private fun fetchHtml(startUrl: HttpUrl): HtmlFetchResult {
        var currentUrl = startUrl
        var redirectCount = 0

        while (true) {
            val request = Request.Builder()
                .url(currentUrl)
                .get()
                .build()

            val response = noRedirectClient.newCall(request).execute()
            response.use {
                if (response.isHttpRedirect()) {
                    if (redirectCount >= MAX_REDIRECTS) {
                        return HtmlFetchResult.Failure(
                            ImportResult.NetworkError("Too many redirects")
                        )
                    }
                    val location = response.header("Location")
                        ?: return HtmlFetchResult.Failure(
                            ImportResult.NetworkError("Redirect location was missing")
                        )
                    val redirectUrl = currentUrl.resolve(location)
                        ?: return HtmlFetchResult.Failure(ImportResult.InvalidUrl)
                    if (!redirectUrl.isHttps) {
                        return HtmlFetchResult.Failure(ImportResult.InvalidUrl)
                    }
                    currentUrl = redirectUrl
                    redirectCount += 1
                    continue
                }

                if (!response.isSuccessful) {
                    return HtmlFetchResult.Failure(
                        ImportResult.NetworkError("Server returned ${response.code}")
                    )
                }
                val body = response.body?.string()
                    ?: return HtmlFetchResult.Failure(
                        ImportResult.NetworkError("The page was empty")
                    )
                return HtmlFetchResult.Success(body, response.request.url.toString())
            }
        }
    }

    private fun okhttp3.Response.isHttpRedirect(): Boolean {
        return code in setOf(300, 301, 302, 303, 307, 308)
    }

    private fun parseIngredientString(raw: String, recipeId: String, order: Int): Ingredient? {
        if (raw.isBlank()) return null
        val parsed = IngredientTextParser.parse(raw) ?: return null
        val canonicalUnit = CanonicalUnit.fromSymbol(parsed.unit)
        return Ingredient(
            recipeId = recipeId,
            name = parsed.name,
            quantity = parsed.quantity,
            unit = canonicalUnit.symbol,
            displayOrder = order
        )
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
