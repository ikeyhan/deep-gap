package ir.atom313.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius

enum class FieldKind { TEXT, NAME, PHONE, EMAIL, USERNAME, PASSWORD, NEW_PASSWORD, NUMBER, MULTILINE, ADDRESS }

/**
 * فیلد فرم موبایل: برچسب شفاف، صفحه‌کلید مناسب هر نوع، خطای زیر فیلد،
 * نمایش/پنهان رمز و جهت متن صحیح (شماره و نام کاربری چپ‌به‌راست).
 */
@Composable
fun AtomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    kind: FieldKind = FieldKind.TEXT,
    error: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
    maxLength: Int = 500,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val isPassword = kind == FieldKind.PASSWORD || kind == FieldKind.NEW_PASSWORD
    val ltr = kind in setOf(FieldKind.PHONE, FieldKind.EMAIL, FieldKind.USERNAME, FieldKind.NUMBER) || isPassword
    val keyboard = KeyboardOptions(
        keyboardType = when (kind) {
            FieldKind.PHONE -> KeyboardType.Phone
            FieldKind.EMAIL -> KeyboardType.Email
            FieldKind.PASSWORD, FieldKind.NEW_PASSWORD -> KeyboardType.Password
            FieldKind.NUMBER -> KeyboardType.Number
            FieldKind.USERNAME -> KeyboardType.Ascii
            else -> KeyboardType.Text
        },
        capitalization = if (kind == FieldKind.NAME) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        autoCorrectEnabled = kind == FieldKind.TEXT || kind == FieldKind.MULTILINE || kind == FieldKind.ADDRESS,
        imeAction = if (kind == FieldKind.MULTILINE) ImeAction.Default else imeAction,
    )
    val multiline = kind == FieldKind.MULTILINE || kind == FieldKind.ADDRESS
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = AtomTheme.colors.textTertiary) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "پنهان کردن رمز" else "نمایش رمز",
                    )
                }
            }
        } else null,
        isError = error != null,
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        singleLine = !multiline,
        minLines = if (kind == FieldKind.MULTILINE) 4 else if (kind == FieldKind.ADDRESS) 2 else 1,
        maxLines = if (multiline) 6 else 1,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.md),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = AtomTheme.colors.border,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth().semantics { if (error != null) error(error) },
    )
}
