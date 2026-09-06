import 'package:flutter/material.dart';

/// Deep Gap design tokens (independent brand — no competitor assets/branding).
class AppTheme {
  static const _seed = Color(0xFF4C5BD4); // modern, premium, AI-oriented

  static ThemeData light() => _base(Brightness.light);
  static ThemeData dark() => _base(Brightness.dark);

  static ThemeData _base(Brightness brightness) {
    final scheme = ColorScheme.fromSeed(seedColor: _seed, brightness: brightness);
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      // RTL + Persian-first: bundle a Persian font (e.g. Vazirmatn) in assets.
      fontFamily: 'Vazirmatn',
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.all(Radius.circular(14)),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          minimumSize: const Size.fromHeight(52),
        ),
      ),
    );
  }
}
