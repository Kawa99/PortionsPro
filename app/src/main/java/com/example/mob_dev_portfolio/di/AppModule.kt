package com.example.mob_dev_portfolio.di

import android.content.Context
import com.example.mob_dev_portfolio.data.AppDatabase
import com.example.mob_dev_portfolio.data.RecipeDao
import com.example.mob_dev_portfolio.data.RecipeRepository
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.SettingsRepository
import com.example.mob_dev_portfolio.data.SettingsRepositoryInterface
import com.example.mob_dev_portfolio.data.ShoppingListDao
import com.example.mob_dev_portfolio.ui.create.RecipeImageStorage
import com.example.mob_dev_portfolio.ui.create.RecipeImageStorageInterface
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepository): RecipeRepositoryInterface

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepository): SettingsRepositoryInterface

    @Binds
    @Singleton
    abstract fun bindRecipeImageStorage(impl: RecipeImageStorage): RecipeImageStorageInterface

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.getInstance(context)
        }

        @Provides
        @Singleton
        fun provideRecipeDao(database: AppDatabase): RecipeDao {
            return database.recipeDao()
        }

        @Provides
        @Singleton
        fun provideShoppingListDao(database: AppDatabase): ShoppingListDao {
            return database.shoppingListDao()
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (compatible; PortionsPro/1.0; recipe importer)"
                        )
                        .header("Accept", "text/html,application/xhtml+xml")
                        .build()
                    chain.proceed(request)
                }
                .build()
        }
    }
}
