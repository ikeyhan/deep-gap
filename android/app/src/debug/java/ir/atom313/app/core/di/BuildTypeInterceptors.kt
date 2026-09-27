package ir.atom313.app.core.di

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/** فقط بیلد debug: لاگ خط‌به‌خط درخواست‌ها (بدون بدنه و هدرها، پس توکن در لاگ نمی‌آید). */
object BuildTypeInterceptors {
    fun all(): List<Interceptor> = listOf(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
}
