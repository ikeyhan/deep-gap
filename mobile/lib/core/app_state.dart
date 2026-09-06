import 'package:flutter/material.dart';

import 'api_client.dart';

/// App-wide state: theme mode and authentication/session.
class AppState extends ChangeNotifier {
  AppState(this.api);

  final ApiClient api;

  ThemeMode _themeMode = ThemeMode.system;
  ThemeMode get themeMode => _themeMode;

  bool _authenticated = false;
  bool get authenticated => _authenticated;

  bool _booting = true;
  bool get booting => _booting;

  Future<void> bootstrap() async {
    final results = await Future.wait([
      api.hasSession(),
      Future.delayed(const Duration(milliseconds: 1100)), // let the splash breathe
    ]);
    _authenticated = results.first as bool;
    _booting = false;
    notifyListeners();
  }

  void toggleTheme() {
    final isDark = _themeMode == ThemeMode.dark ||
        (_themeMode == ThemeMode.system &&
            WidgetsBinding.instance.platformDispatcher.platformBrightness == Brightness.dark);
    _themeMode = isDark ? ThemeMode.light : ThemeMode.dark;
    notifyListeners();
  }

  Future<void> onLoggedIn(String access, String refresh) async {
    await api.saveTokens(access, refresh);
    _authenticated = true;
    notifyListeners();
  }

  Future<void> logout() async {
    await api.clearTokens();
    _authenticated = false;
    notifyListeners();
  }
}
