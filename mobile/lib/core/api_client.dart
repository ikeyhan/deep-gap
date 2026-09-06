import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Thin API client for the Deep Gap backend.
///
/// The app NEVER holds provider API keys — it only talks to our backend, which
/// injects and rotates the JWT access/refresh tokens transparently.
class ApiClient {
  ApiClient({required this.baseUrl, FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage() {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 60),
    ));
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await _storage.read(key: _accessKey);
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (e, handler) async {
        // On 401, try a single refresh then retry the original request.
        if (e.response?.statusCode == 401 && !_isRefreshing) {
          final refreshed = await _refresh();
          if (refreshed) {
            final req = e.requestOptions;
            final token = await _storage.read(key: _accessKey);
            req.headers['Authorization'] = 'Bearer $token';
            final clone = await _dio.fetch(req);
            return handler.resolve(clone);
          }
        }
        handler.next(e);
      },
    ));
  }

  final String baseUrl;
  final FlutterSecureStorage _storage;
  late final Dio _dio;
  bool _isRefreshing = false;

  static const _accessKey = 'access_token';
  static const _refreshKey = 'refresh_token';

  Dio get dio => _dio;

  Future<void> saveTokens(String access, String refresh) async {
    await _storage.write(key: _accessKey, value: access);
    await _storage.write(key: _refreshKey, value: refresh);
  }

  Future<void> clearTokens() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }

  Future<bool> hasSession() async =>
      (await _storage.read(key: _accessKey)) != null;

  Future<bool> _refresh() async {
    _isRefreshing = true;
    try {
      final refresh = await _storage.read(key: _refreshKey);
      if (refresh == null) return false;
      final res = await Dio(BaseOptions(baseUrl: baseUrl))
          .post('/api/v1/auth/refresh', data: {'refresh_token': refresh});
      await saveTokens(
          res.data['access_token'] as String, res.data['refresh_token'] as String);
      return true;
    } catch (_) {
      await clearTokens();
      return false;
    } finally {
      _isRefreshing = false;
    }
  }

  // ---- Endpoints ----
  Future<Response> requestOtp(String phone) =>
      _dio.post('/api/v1/auth/otp/request', data: {'phone': phone});

  Future<Response> verifyOtp(String phone, String code, {String? deviceId}) =>
      _dio.post('/api/v1/auth/otp/verify',
          data: {'phone': phone, 'code': code, 'device_id': deviceId});

  Future<Response> me() => _dio.get('/api/v1/users/me');

  Future<Response> models() => _dio.get('/api/v1/models');

  Future<Response> wallet() => _dio.get('/api/v1/wallet');

  Future<Response> createConversation({String? title, String? modelCode}) =>
      _dio.post('/api/v1/conversations',
          data: {'title': title, 'model_code': modelCode});

  Future<Response> conversations() => _dio.get('/api/v1/conversations');

  Future<Response> sendMessage(String conversationId, String content,
          {String modelCode = 'fast'}) =>
      _dio.post('/api/v1/conversations/$conversationId/messages',
          data: {'content': content, 'model_code': modelCode});
}
