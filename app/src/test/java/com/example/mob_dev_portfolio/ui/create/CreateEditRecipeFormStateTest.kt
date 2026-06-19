package com.example.mob_dev_portfolio.ui.create

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateEditRecipeFormStateTest {

    @Test
    fun validateReturnsErrorsForMissingNameInvalidServingsAndBadIngredientQuantity() {
        val formState = CreateEditRecipeFormState(
            name = "",
            category = "Dinner",
            baseServings = "0",
            instructionRows = emptyList(),
            thumbnailUrl = "",
            ingredientRows = listOf(
                IngredientInputRow(name = "Flour", quantity = "abc", unit = "g"),
                IngredientInputRow(name = "", quantity = "", unit = "")
            )
        )

        val errors = formState.validate()

        assertTrue(errors.name)
        assertTrue(errors.servings)
        assertEquals(setOf(0), errors.ingredientQuantityErrorIndexes)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun validateAllowsNamedIngredientsWithoutExplicitQuantity() {
        val formState = validFormState(
            ingredientRows = listOf(
                IngredientInputRow(name = "Butter, for greasing", quantity = "", unit = "")
            )
        )

        val prepared = formState.toPreparedRecipeForm()

        assertFalse(formState.validate().hasErrors)
        assertEquals(0.0, prepared.ingredients[0].quantity, 0.0)
        assertEquals("Butter, for greasing", prepared.ingredients[0].name)
    }

    @Test
    fun validateIgnoresBlankIngredientRows() {
        val formState = validFormState(
            ingredientRows = listOf(
                IngredientInputRow(name = "", quantity = "", unit = ""),
                IngredientInputRow(name = "Flour", quantity = "250", unit = "g")
            )
        )

        val errors = formState.validate()

        assertFalse(errors.hasErrors)
    }

    @Test
    fun toPreparedRecipeFormTrimsFieldsAndBuildsIngredientsAndInstructions() {
        val prepared = validFormState(
            name = "  Pancakes  ",
            category = "  Breakfast  ",
            instructionRows = listOf(
                InstructionInputRow(" Mix "),
                InstructionInputRow(""),
                InstructionInputRow(" Cook ")
            ),
            ingredientRows = listOf(
                IngredientInputRow(name = " Flour ", quantity = "250", unit = " g "),
                IngredientInputRow(name = "", quantity = "", unit = "")
            )
        ).toPreparedRecipeForm()

        assertEquals("Pancakes", prepared.name)
        assertEquals("Breakfast", prepared.category)
        assertEquals("Mix\nCook", prepared.instructions)
        assertEquals(4, prepared.baseServings)
        assertEquals(1, prepared.ingredients.size)
        assertEquals("Flour", prepared.ingredients[0].name)
        assertEquals(250.0, prepared.ingredients[0].quantity, 0.0)
        assertEquals("g", prepared.ingredients[0].unit)
        assertEquals(0, prepared.ingredients[0].displayOrder)
    }

    private fun validFormState(
        name: String = "Pancakes",
        category: String = "Breakfast",
        baseServings: String = "4",
        instructionRows: List<InstructionInputRow> = listOf(InstructionInputRow("Mix")),
        ingredientRows: List<IngredientInputRow> = listOf(
            IngredientInputRow(name = "Flour", quantity = "250", unit = "g")
        )
    ) = CreateEditRecipeFormState(
        name = name,
        category = category,
        baseServings = baseServings,
        instructionRows = instructionRows,
        thumbnailUrl = "",
        ingredientRows = ingredientRows
    )
}
