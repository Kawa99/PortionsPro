package com.example.mob_dev_portfolio.ui.create

import com.example.mob_dev_portfolio.data.Ingredient
import java.util.UUID

data class IngredientInputRow(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val id: String = UUID.randomUUID().toString()
)

internal val IngredientInputRow.isComplete: Boolean
    get() = name.isNotBlank() && quantity.isNotBlank()

private val IngredientInputRow.hasValidQuantity: Boolean
    get() = quantity.isBlank() || quantity.toDoubleOrNull()?.let { it > 0.0 } == true

data class InstructionInputRow(
    val text: String = "",
    val id: String = UUID.randomUUID().toString()
)

data class CreateEditRecipeFormState(
    val name: String = "",
    val category: String = "",
    val baseServings: String = "4",
    val instructionRows: List<InstructionInputRow> = listOf(InstructionInputRow()),
    val thumbnailUrl: String = "",
    val ingredientRows: List<IngredientInputRow> = listOf(IngredientInputRow()),
    val area: String = "",
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val sourceUrl: String = ""
)

data class CreateEditFormErrors(
    val name: Boolean = false,
    val servings: Boolean = false,
    val ingredientQuantityErrorIndexes: Set<Int> = emptySet()
) {
    val hasErrors: Boolean
        get() = name || servings || ingredientQuantityErrorIndexes.isNotEmpty()
}

data class PreparedRecipeForm(
    val name: String,
    val category: String,
    val area: String,
    val instructions: String,
    val thumbnailUrl: String,
    val sourceUrl: String,
    val baseServings: Int,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val ingredients: List<Ingredient>
)

fun CreateEditRecipeFormState.validate(): CreateEditFormErrors {
    return CreateEditFormErrors(
        name = name.isBlank(),
        servings = baseServings.toIntOrNull()?.let { it < 1 } ?: true,
        ingredientQuantityErrorIndexes = ingredientRows
            .mapIndexedNotNull { index, row ->
                if (row.name.isNotBlank() && !row.hasValidQuantity) index else null
            }
            .toSet()
    )
}

fun CreateEditRecipeFormState.toPreparedRecipeForm(): PreparedRecipeForm {
    val errors = validate()
    require(!errors.hasErrors) { "Cannot prepare invalid recipe form" }

    val ingredients = ingredientRows
        .filter { it.name.isNotBlank() }
        .mapIndexed { index, row ->
            Ingredient(
                recipeId = "",
                name = row.name.trim(),
                quantity = row.quantity.toDoubleOrNull() ?: 0.0,
                unit = row.unit.trim(),
                displayOrder = index
            )
        }

    val instructions = instructionRows
        .map { it.text.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")

    return PreparedRecipeForm(
        name = name.trim(),
        category = category.trim(),
        area = area.trim(),
        instructions = instructions,
        thumbnailUrl = thumbnailUrl,
        sourceUrl = sourceUrl.trim(),
        baseServings = baseServings.toInt(),
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        ingredients = ingredients
    )
}
