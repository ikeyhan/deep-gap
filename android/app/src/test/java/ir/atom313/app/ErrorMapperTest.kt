package ir.atom313.app

import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.network.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** پاسخ‌های خطای سرور باید به پیام فارسی قابل‌نمایش تبدیل شوند، نه متن فنی. */
class ErrorMapperTest {

    private fun body(code: String, message: String, fields: String = "") =
        """{"error":{"code":"$code","message":"$message"${if (fields.isEmpty()) "" else ",\"fields\":$fields"}}}"""

    @Test
    fun `validation errors keep the per-field messages`() {
        val error = ErrorMapper.fromHttp(400, body("invalid_input", "خطا در فرم", """{"phone":"شماره نامعتبر است"}"""))
        assertTrue(error is AppError.Validation)
        assertEquals("خطا در فرم", error.message)
        assertEquals("شماره نامعتبر است", (error as AppError.Validation).fields["phone"])
    }

    @Test
    fun `wrong credentials stay on the form instead of logging the user out`() {
        val error = ErrorMapper.fromHttp(401, body("invalid_credentials", "نام کاربری یا رمز عبور اشتباه است."))
        assertTrue(error is AppError.Validation)
    }

    @Test
    fun `an expired session maps to unauthorized`() {
        val error = ErrorMapper.fromHttp(401, body("token_invalid", "نشست منقضی شده است."))
        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `maintenance mode is recognised`() {
        val error = ErrorMapper.fromHttp(503, body("maintenance", "در حال به‌روزرسانی"))
        assertTrue(error is AppError.Maintenance)
        assertEquals("در حال به‌روزرسانی", error.message)
    }

    @Test
    fun `out of stock conflicts keep their code for the cart screen`() {
        val error = ErrorMapper.fromHttp(409, body("out_of_stock", "موجودی کافی نیست."))
        assertEquals("out_of_stock", (error as AppError.Conflict).code)
    }

    @Test
    fun `server errors never leak technical details`() {
        val error = ErrorMapper.fromHttp(500, "<html>Internal Server Error at line 42</html>")
        assertTrue(error is AppError.Server)
        assertFalse(error.message.contains("line 42"))
        assertFalse(error.message.contains("html"))
    }

    @Test
    fun `network failures map to offline and timeout`() {
        assertEquals(AppError.Offline, ErrorMapper.fromThrowable(UnknownHostException()))
        assertEquals(AppError.Offline, ErrorMapper.fromThrowable(IOException("reset")))
        assertEquals(AppError.Timeout, ErrorMapper.fromThrowable(SocketTimeoutException()))
    }

    @Test
    fun `retryable errors are the transient ones`() {
        assertTrue(AppError.Offline.isRetryable)
        assertTrue(AppError.Server().isRetryable)
        assertFalse(AppError.Validation("x", "y").isRetryable)
        assertFalse(AppError.NotFound().isRetryable)
    }
}
