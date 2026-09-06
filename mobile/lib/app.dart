import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'core/app_state.dart';
import 'core/theme.dart';
import 'core/tokens.dart';
import 'features/auth/auth_flow.dart';
import 'features/home/home_page.dart';
import 'features/splash/splash_page.dart';

class DeepGapApp extends StatelessWidget {
  const DeepGapApp({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    return MaterialApp(
      title: 'دیپ گپ',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: state.themeMode,
      locale: const Locale('fa'),
      supportedLocales: const [Locale('fa'), Locale('en')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      builder: (context, child) =>
          Directionality(textDirection: TextDirection.rtl, child: child!),
      home: const RootGate(),
    );
  }
}

/// Single source of truth for the top-level screen. Swaps with a soft cross-fade
/// so entering/leaving the app never feels abrupt.
class RootGate extends StatelessWidget {
  const RootGate({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final Widget screen;
    if (state.booting) {
      screen = const SplashPage(key: ValueKey('splash'));
    } else if (state.authenticated) {
      screen = const HomePage(key: ValueKey('home'));
    } else {
      screen = const AuthFlow(key: ValueKey('auth'));
    }
    return AnimatedSwitcher(
      duration: Motion.slow,
      switchInCurve: Curves.easeOutCubic,
      switchOutCurve: Curves.easeIn,
      child: screen,
    );
  }
}
