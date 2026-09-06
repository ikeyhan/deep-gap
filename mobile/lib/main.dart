import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'app.dart';
import 'core/api_client.dart';
import 'core/app_state.dart';

/// Backend base URL. Override per flavor: --dart-define=BASE_URL=...
/// Android emulator reaches host localhost via 10.0.2.2.
const String kBaseUrl =
    String.fromEnvironment('BASE_URL', defaultValue: 'http://10.0.2.2:8000');

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final api = ApiClient(baseUrl: kBaseUrl);
  final state = AppState(api)..bootstrap();
  runApp(
    ChangeNotifierProvider.value(value: state, child: const DeepGapApp()),
  );
}
