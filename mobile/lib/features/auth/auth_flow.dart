import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/icons.dart';
import '../../core/tokens.dart';

enum _Step { onboarding, phone, otp }

String errorMessage(Object e) {
  if (e is DioException) {
    final data = e.response?.data;
    if (data is Map && data['message'] is String) return data['message'] as String;
    return 'ارتباط با سرور برقرار نشد';
  }
  return 'خطای غیرمنتظره رخ داد';
}

class AuthFlow extends StatefulWidget {
  const AuthFlow({super.key});

  @override
  State<AuthFlow> createState() => _AuthFlowState();
}

class _AuthFlowState extends State<AuthFlow> {
  _Step _step = _Step.onboarding;
  final _phoneCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();
  bool _loading = false;
  String _phone = '';

  @override
  void dispose() {
    _phoneCtrl.dispose();
    _codeCtrl.dispose();
    super.dispose();
  }

  void _go(_Step s) => setState(() => _step = s);

  void _snack(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _requestOtp() async {
    final phone = _phoneCtrl.text.trim();
    if (phone.length != 11 || !phone.startsWith('09')) {
      _snack('شماره موبایل معتبر نیست (نمونه: 09xxxxxxxxx)');
      return;
    }
    setState(() => _loading = true);
    try {
      final api = context.read<AppState>().api;
      final res = await api.requestOtp(phone);
      _phone = phone;
      final dev = res.data['dev_code'];
      if (dev != null) _codeCtrl.text = dev.toString(); // dev convenience
      _go(_Step.otp);
    } catch (e) {
      _snack(errorMessage(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _verify() async {
    final code = _codeCtrl.text.trim();
    if (code.length < 4) {
      _snack('کد تأیید را کامل وارد کنید');
      return;
    }
    setState(() => _loading = true);
    try {
      final state = context.read<AppState>();
      final res = await state.api.verifyOtp(_phone, code);
      await state.onLoggedIn(
        res.data['access_token'] as String,
        res.data['refresh_token'] as String,
      );
    } catch (e) {
      _snack(errorMessage(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            onPressed: context.read<AppState>().toggleTheme,
            icon: AppIcon(AppIcons.sun, size: 20),
            tooltip: 'تغییر تم',
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Insets.lg),
          child: AnimatedSwitcher(
            duration: Motion.normal,
            switchInCurve: Curves.easeOutCubic,
            transitionBuilder: (child, anim) => FadeTransition(
              opacity: anim,
              child: SlideTransition(
                position: Tween(begin: const Offset(0, 0.04), end: Offset.zero).animate(anim),
                child: child,
              ),
            ),
            child: _buildStep(),
          ),
        ),
      ),
    );
  }

  Widget _buildStep() {
    switch (_step) {
      case _Step.onboarding:
        return _Onboarding(key: const ValueKey('ob'), onStart: () => _go(_Step.phone));
      case _Step.phone:
        return _PhoneStep(
          key: const ValueKey('ph'),
          controller: _phoneCtrl,
          loading: _loading,
          onSubmit: _requestOtp,
        );
      case _Step.otp:
        return _OtpStep(
          key: const ValueKey('otp'),
          controller: _codeCtrl,
          phone: _phone,
          loading: _loading,
          onSubmit: _verify,
          onBack: () => _go(_Step.phone),
        );
    }
  }
}

class _Onboarding extends StatelessWidget {
  const _Onboarding({super.key, required this.onStart});
  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final items = [
      ('چندین هوش مصنوعی، یک اپ', 'سریع، هوشمند، پژوهشگر و طراح — بدون دانستن نام مدل‌ها.'),
      ('شفاف و منصفانه', 'هر کار چند اعتبار می‌برد را دقیق می‌بینید.'),
      ('فارسی، تمیز، سریع', 'راست‌به‌چپ کامل و تجربه‌ای مینیمال.'),
    ];
    return Column(
      key: const ValueKey('ob-col'),
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Spacer(),
        const Center(child: AppLogo(size: 92)),
        const SizedBox(height: Insets.xl),
        ...items.asMap().entries.map((e) {
          final (title, sub) = e.value;
          return Padding(
            padding: const EdgeInsets.only(bottom: Insets.md),
            child: Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  margin: const EdgeInsets.only(top: 6, left: Insets.sm),
                  decoration: BoxDecoration(
                    color: t.colorScheme.onSurface,
                    shape: BoxShape.circle,
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: t.textTheme.titleMedium),
                      Text(sub, style: t.textTheme.bodyMedium),
                    ],
                  ),
                ),
              ],
            ),
          ).animate().fadeIn(delay: (120 * e.key).ms).moveX(begin: 12, end: 0);
        }),
        const Spacer(),
        FilledButton(onPressed: onStart, child: const Text('شروع'))
            .animate()
            .fadeIn(delay: 400.ms),
        const SizedBox(height: Insets.lg),
      ],
    );
  }
}

class _PhoneStep extends StatelessWidget {
  const _PhoneStep({
    super.key,
    required this.controller,
    required this.loading,
    required this.onSubmit,
  });
  final TextEditingController controller;
  final bool loading;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Column(
      key: const ValueKey('ph-col'),
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: Insets.xl),
        Text('ورود / ثبت‌نام', style: t.textTheme.displaySmall),
        const SizedBox(height: Insets.xs),
        Text('شمارهٔ موبایل خود را وارد کنید', style: t.textTheme.bodyMedium),
        const SizedBox(height: Insets.xl),
        TextField(
          controller: controller,
          keyboardType: TextInputType.phone,
          textDirection: TextDirection.ltr,
          textAlign: TextAlign.center,
          maxLength: 11,
          style: t.textTheme.titleLarge,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: const InputDecoration(counterText: '', hintText: '09xxxxxxxxx'),
        ),
        const SizedBox(height: Insets.lg),
        FilledButton(
          onPressed: loading ? null : onSubmit,
          child: loading ? const _Spinner() : const Text('دریافت کد تأیید'),
        ),
      ],
    ).animate().fadeIn();
  }
}

class _OtpStep extends StatelessWidget {
  const _OtpStep({
    super.key,
    required this.controller,
    required this.phone,
    required this.loading,
    required this.onSubmit,
    required this.onBack,
  });
  final TextEditingController controller;
  final String phone;
  final bool loading;
  final VoidCallback onSubmit;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Column(
      key: const ValueKey('otp-col'),
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: Insets.xl),
        Text('کد تأیید', style: t.textTheme.displaySmall),
        const SizedBox(height: Insets.xs),
        Text('کد ارسال‌شده به $phone را وارد کنید', style: t.textTheme.bodyMedium),
        const SizedBox(height: Insets.xl),
        TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          textDirection: TextDirection.ltr,
          textAlign: TextAlign.center,
          maxLength: 6,
          style: t.textTheme.displaySmall?.copyWith(letterSpacing: 12),
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: const InputDecoration(counterText: '', hintText: '·····'),
        ),
        const SizedBox(height: Insets.lg),
        FilledButton(
          onPressed: loading ? null : onSubmit,
          child: loading ? const _Spinner() : const Text('تأیید و ورود'),
        ),
        const SizedBox(height: Insets.sm),
        TextButton(onPressed: onBack, child: const Text('تغییر شماره')),
      ],
    ).animate().fadeIn();
  }
}

class _Spinner extends StatelessWidget {
  const _Spinner();
  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 22,
      height: 22,
      child: CircularProgressIndicator(
        strokeWidth: 2.4,
        valueColor: AlwaysStoppedAnimation(Theme.of(context).colorScheme.onPrimary),
      ),
    );
  }
}
