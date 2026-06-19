package com.example.mob_dev_portfolio.data

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeImportDraftStore @Inject constructor() {
    private val drafts = mutableMapOf<String, RecipeImportDraft>()

    @Synchronized
    fun put(draft: RecipeImportDraft): String {
        val id = UUID.randomUUID().toString()
        drafts[id] = draft
        return id
    }

    @Synchronized
    fun consume(id: String): RecipeImportDraft? {
        return drafts.remove(id)
    }
}
