package ir.atom313.app.core.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.atom313.app.BuildConfig
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.AuthApi
import ir.atom313.app.core.network.AuthInterceptor
import ir.atom313.app.core.network.ClientHeadersInterceptor
import ir.atom313.app.core.network.StatusInterceptor
import ir.atom313.app.core.network.TokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** کلاینت پایه: کش HTTP (ETag/304 برای دادهٔ عمومی)، زمان‌های انتظار و هدرهای اپ. */
    @Provides @Singleton @Named("base")
    fun baseClient(@ApplicationContext context: Context, events: ir.atom313.app.core.network.SessionEvents): OkHttpClient {
        val ua = "Atom313-Android/${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE})"
        val builder = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http"), 20L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(ClientHeadersInterceptor(ua))
            .addInterceptor(StatusInterceptor(events))
        BuildTypeInterceptors.all().forEach(builder::addInterceptor)
        return builder.build()
    }

    @Provides @Singleton @Named("authed")
    fun authedClient(
        @Named("base") base: OkHttpClient,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .build()

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun authApi(@Named("base") client: OkHttpClient, json: Json): AuthApi = retrofit(client, json).create(AuthApi::class.java)

    @Provides @Singleton
    fun atomApi(@Named("authed") client: OkHttpClient, json: Json): AtomApi = retrofit(client, json).create(AtomApi::class.java)

    @Provides @Named("apiBaseUrl")
    fun apiBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides @Named("webBaseUrl")
    fun webBaseUrl(): String = BuildConfig.WEB_BASE_URL
}
