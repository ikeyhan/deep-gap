import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'core/api_client.dart';
import 'core/theme.dart';

/// Backend base URL. Override per-flavor (dev/staging/prod).
const String kBaseUrl =
    String.fromEnvironment('BASE_URL', defaultValue: 'http://10.0.2.2:8000');

final apiClient = ApiClient(baseUrl: kBaseUrl);

void main() => runApp(const DeepGapApp());

class DeepGapApp extends StatelessWidget {
  const DeepGapApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'دیپ گپ',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      locale: const Locale('fa'),
      supportedLocales: const [Locale('fa'), Locale('en')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      // Persian-first: force RTL as the default text direction.
      builder: (context, child) =>
          Directionality(textDirection: TextDirection.rtl, child: child!),
      home: const SplashPage(),
    );
  }
}

/// Splash — checks version / maintenance / login state (spec §5). Placeholder UI.
class SplashPage extends StatelessWidget {
  const SplashPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('دیپ گپ',
                style: Theme.of(context).textTheme.displaySmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: Theme.of(context).colorScheme.primary)),
            const SizedBox(height: 8),
            const Text('دستیار هوش مصنوعی فارسی'),
            const SizedBox(height: 24),
            const CircularProgressIndicator(),
          ],
        ),
      ),
    );
  }
}
