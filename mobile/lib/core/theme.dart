import 'package:flutter/material.dart';

import 'tokens.dart';

/// Deep Gap — strictly black & white, minimal.
/// Dark:  black surfaces, white ink.
/// Light: white surfaces, black ink (a clean inversion).
class AppTheme {
  static const _fontFamily = 'YekanBakh';
  static const _fallback = ['Yekan', 'Roboto'];

  // Neutral ramp shared by both themes (inverted via role assignment).
  static const black = Color(0xFF000000);
  static const white = Color(0xFFFFFFFF);
  static const near = Color(0xFF0B0B0C); // near-black
  static const ink80 = Color(0xFFEDEDED);

  static ThemeData dark() => _build(Brightness.dark);
  static ThemeData light() => _build(Brightness.light);

  static ThemeData _build(Brightness brightness) {
    final isDark = brightness == Brightness.dark;
    final bg = isDark ? black : white;
    final surface = isDark ? const Color(0xFF0E0E10) : const Color(0xFFF4F4F5);
    final ink = isDark ? white : black;
    final inkSoft = isDark ? const Color(0xFFB6B6BA) : const Color(0xFF5B5B60);
    final border = isDark ? const Color(0xFF26262A) : const Color(0xFFE1E1E4);

    final scheme = ColorScheme(
      brightness: brightness,
      primary: ink,
      onPrimary: bg,
      secondary: ink,
      onSecondary: bg,
      surface: bg,
      onSurface: ink,
      surfaceContainerHighest: surface,
      onSurfaceVariant: inkSoft,
      error: isDark ? white : black,
      onError: bg,
      outline: border,
    );

    final baseText = _textTheme(ink, inkSoft);

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      scaffoldBackgroundColor: bg,
      colorScheme: scheme,
      fontFamily: _fontFamily,
      fontFamilyFallback: _fallback,
      textTheme: baseText,
      dividerColor: border,
      splashFactory: InkSparkle.splashFactory,
      appBarTheme: AppBarTheme(
        backgroundColor: bg,
        foregroundColor: ink,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: baseText.titleLarge,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surface,
        hintStyle: TextStyle(color: inkSoft),
        contentPadding: const EdgeInsets.symmetric(horizontal: Insets.md, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: Radii.button,
          borderSide: BorderSide(color: border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: Radii.button,
          borderSide: BorderSide(color: border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: Radii.button,
          borderSide: BorderSide(color: ink, width: 1.6),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: ink,
          foregroundColor: bg,
          disabledBackgroundColor: inkSoft.withOpacity(0.3),
          minimumSize: const Size.fromHeight(52),
          textStyle: baseText.labelLarge,
          shape: const RoundedRectangleBorder(borderRadius: Radii.button),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: ink,
          side: BorderSide(color: border),
          minimumSize: const Size.fromHeight(52),
          shape: const RoundedRectangleBorder(borderRadius: Radii.button),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: ink,
        contentTextStyle: TextStyle(color: bg),
        behavior: SnackBarBehavior.floating,
        shape: const RoundedRectangleBorder(borderRadius: Radii.button),
      ),
    );
  }

  static TextTheme _textTheme(Color ink, Color inkSoft) {
    TextStyle s(double size, FontWeight w, {Color? c, double h = 1.35}) =>
        TextStyle(fontSize: size, fontWeight: w, color: c ?? ink, height: h);
    return TextTheme(
      displaySmall: s(TextSizes.display, FontWeight.w800, h: 1.15),
      titleLarge: s(TextSizes.title, FontWeight.w700),
      titleMedium: s(TextSizes.subtitle, FontWeight.w600),
      bodyLarge: s(TextSizes.body, FontWeight.w400),
      bodyMedium: s(TextSizes.body, FontWeight.w400, c: inkSoft),
      labelLarge: s(TextSizes.subtitle, FontWeight.w700),
      labelMedium: s(TextSizes.label, FontWeight.w500, c: inkSoft),
      bodySmall: s(TextSizes.caption, FontWeight.w400, c: inkSoft),
    );
  }
}
