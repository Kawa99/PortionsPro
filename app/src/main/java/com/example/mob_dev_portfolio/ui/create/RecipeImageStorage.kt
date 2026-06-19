package com.example.mob_dev_portfolio.ui.create

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

interface RecipeImageStorageInterface {
    suspend fun copyToInternalStorage(sourceUri: Uri): String
    suspend fun downloadToInternalStorage(imageUrl: String): String
}

class RecipeImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : RecipeImageStorageInterface {
    private val noRedirectClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun copyToInternalStorage(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val imageDir = File(context.filesDir, "recipe_images").apply { mkdirs() }
        val imageFile = File(imageDir, "recipe-${UUID.randomUUID()}.jpg")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(imageFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext ""

        Uri.fromFile(imageFile).toString()
    }

    override suspend fun downloadToInternalStorage(imageUrl: String): String = withContext(Dispatchers.IO) {
        val httpUrl = imageUrl.trim().toHttpUrlOrNull() ?: return@withContext ""
        if (!httpUrl.isHttps) return@withContext ""

        runCatching { downloadHttpsImage(httpUrl) }.getOrDefault("")
    }

    private fun downloadHttpsImage(startUrl: HttpUrl): String {
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
                    if (redirectCount >= MAX_REDIRECTS) return ""
                    val location = response.header("Location") ?: return ""
                    val redirectUrl = currentUrl.resolve(location) ?: return ""
                    if (!redirectUrl.isHttps) return ""
                    currentUrl = redirectUrl
                    redirectCount += 1
                    continue
                }

                if (!response.isSuccessful) return ""
                val body = response.body ?: return ""
                val contentType = body.contentType()?.type
                if (contentType != null && contentType != "image") return ""

                val imageDir = File(context.filesDir, "recipe_images").apply { mkdirs() }
                val imageFile = File(imageDir, "recipe-${UUID.randomUUID()}.jpg")
                body.byteStream().use { input ->
                    FileOutputStream(imageFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return Uri.fromFile(imageFile).toString()
            }
        }
    }

    private fun okhttp3.Response.isHttpRedirect(): Boolean {
        return code in setOf(300, 301, 302, 303, 307, 308)
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
