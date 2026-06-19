package com.example.mob_dev_portfolio.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SchemaRecipeParserTest {

    @Test
    fun parseFindsRootRecipeJsonLd() {
        val dto = SchemaRecipeParser.parse(
            """
            <html><head>
            <script type="application/ld+json">
            {
              "@context": "https://schema.org",
              "@type": "Recipe",
              "name": "Chicken Tikka Masala",
              "recipeYield": "4 servings",
              "prepTime": "PT20M",
              "cookTime": "PT1H10M",
              "recipeCategory": "Chicken",
              "recipeCuisine": "Indian",
              "image": {"url": "https://example.com/image.jpg"},
              "recipeIngredient": ["700g chicken thighs", "2 tsp cumin"],
              "recipeInstructions": [
                {"@type": "HowToStep", "text": "Marinate the chicken."},
                {"@type": "HowToStep", "text": "Cook until done."}
              ]
            }
            </script>
            </head></html>
            """.trimIndent()
        )

        assertNotNull(dto)
        assertEquals("Chicken Tikka Masala", dto!!.name)
        assertEquals(4, SchemaRecipeParser.extractServings(dto))
        assertEquals(20, SchemaRecipeParser.parseDurationMinutes(dto.prepTime))
        assertEquals(70, SchemaRecipeParser.parseDurationMinutes(dto.cookTime))
        assertEquals("https://example.com/image.jpg", SchemaRecipeParser.extractImageUrl(dto))
        assertEquals(listOf("700g chicken thighs", "2 tsp cumin"), dto.recipeIngredient)
        assertEquals(
            "1. Marinate the chicken.\n2. Cook until done.",
            SchemaRecipeParser.extractInstructions(dto)
        )
    }

    @Test
    fun parseFindsRecipeInsideGraphAndTypeArray() {
        val dto = SchemaRecipeParser.parse(
            """
            <script data-test="x" type='application/ld+json'>
            {
              "@graph": [
                {"@type": "WebPage", "name": "Page"},
                {
                  "@type": ["Thing", "Recipe"],
                  "name": "Dal",
                  "recipeYield": ["6", "6 servings"],
                  "recipeCategory": ["Dinner"],
                  "recipeCuisine": ["Indian"],
                  "image": ["https://example.com/dal.jpg"],
                  "recipeIngredient": ["1½ cups lentils"],
                  "recipeInstructions": [
                    {
                      "@type": "HowToSection",
                      "itemListElement": [
                        {"@type": "HowToStep", "text": "Rinse lentils."},
                        {"@type": "HowToStep", "text": "Simmer gently."}
                      ]
                    }
                  ]
                }
              ]
            }
            </script>
            """.trimIndent()
        )

        assertNotNull(dto)
        assertEquals("Dal", dto!!.name)
        assertEquals(6, SchemaRecipeParser.extractServings(dto))
        assertEquals("Dinner", dto.recipeCategory)
        assertEquals("Indian", dto.recipeCuisine)
        assertEquals("https://example.com/dal.jpg", SchemaRecipeParser.extractImageUrl(dto))
        assertEquals("1. Rinse lentils.\n2. Simmer gently.", SchemaRecipeParser.extractInstructions(dto))
    }
}
