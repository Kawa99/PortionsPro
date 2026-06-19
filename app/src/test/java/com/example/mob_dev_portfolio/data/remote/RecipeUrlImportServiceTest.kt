package com.example.mob_dev_portfolio.data.remote

import com.example.mob_dev_portfolio.data.remote.RecipeUrlImportService.ImportResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeUrlImportServiceTest {

    @Test
    fun importFromUrlRejectsHttpBeforeNetworkRequest() = runTest {
        val service = RecipeUrlImportService(
            OkHttpClient.Builder()
                .addInterceptor {
                    throw AssertionError("HTTP URL should be rejected before network execution")
                }
                .build()
        )

        val result = service.importFromUrl("http://example.com/recipe")

        assertEquals(ImportResult.InvalidUrl, result)
    }

    @Test
    fun importFromUrlRejectsHttpsRedirectToHttp() = runTest {
        val service = RecipeUrlImportService(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(302)
                        .message("Found")
                        .header("Location", "http://example.com/insecure-recipe")
                        .body("".toResponseBody(null))
                        .build()
                }
                .build()
        )

        val result = service.importFromUrl("https://example.com/recipe")

        assertEquals(ImportResult.InvalidUrl, result)
    }

    @Test
    fun importFromUrlAcceptsHttpsRecipeResponse() = runTest {
        val service = RecipeUrlImportService(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(recipeHtml.toResponseBody("text/html".toMediaType()))
                        .build()
                }
                .build()
        )

        val result = service.importFromUrl("https://example.com/recipe")

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals("Test Recipe", success.recipe.name)
        assertEquals("https://example.com/recipe", success.recipe.sourceUrl)
    }

    private companion object {
        val recipeHtml = """
            <html>
              <head>
                <script type="application/ld+json">
                  {
                    "@context": "https://schema.org",
                    "@type": "Recipe",
                    "name": "Test Recipe",
                    "recipeCategory": "Dinner",
                    "recipeCuisine": "Italian",
                    "recipeYield": "2 servings",
                    "recipeIngredient": ["100g pasta"],
                    "recipeInstructions": ["Boil pasta"]
                  }
                </script>
              </head>
            </html>
        """.trimIndent()
    }
}
