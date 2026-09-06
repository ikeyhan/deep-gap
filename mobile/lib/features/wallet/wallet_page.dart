import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/icons.dart';
import '../../core/tokens.dart';
import '../auth/auth_flow.dart' show errorMessage;

class WalletPage extends StatefulWidget {
  const WalletPage({super.key});

  @override
  State<WalletPage> createState() => _WalletPageState();
}

class _WalletPageState extends State<WalletPage> {
  Map<String, dynamic>? _wallet;
  List<Map<String, dynamic>> _plans = [];
  List<Map<String, dynamic>> _packages = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = context.read<AppState>().api;
    try {
      final res = await Future.wait([api.wallet(), api.plans(), api.packages()]);
      setState(() {
        _wallet = res[0].data as Map<String, dynamic>;
        _plans = (res[1].data as List).cast<Map<String, dynamic>>();
        _packages = (res[2].data as List).cast<Map<String, dynamic>>();
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _buy(String type, String code) async {
    try {
      final res = await context.read<AppState>().api.dio.post(
        '/api/v1/payments/initiate',
        data: {'product_type': type, 'code': code},
      );
      final url = res.data['payment_url'];
      if (!mounted) return;
      showModalBottomSheet(
        context: context,
        builder: (_) => _PaySheet(url: url?.toString() ?? ''),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: AppIcon(AppIcons.chevronRight, size: 22),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: const Text('کیف پول و اشتراک'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(Insets.lg),
              children: [
                _balanceCard(t).animate().fadeIn().moveY(begin: 10, end: 0),
                const SizedBox(height: Insets.xl),
                Text('ارتقای اشتراک', style: t.textTheme.titleLarge),
                const SizedBox(height: Insets.md),
                ..._plans.asMap().entries.map((e) => _PlanCard(
                      plan: e.value,
                      onBuy: () => _buy('subscription', e.value['code'] as String),
                    ).animate().fadeIn(delay: (80 * e.key).ms).moveX(begin: 12, end: 0)),
                const SizedBox(height: Insets.xl),
                Text('خرید اعتبار', style: t.textTheme.titleLarge),
                const SizedBox(height: Insets.md),
                ..._packages.asMap().entries.map((e) => _PackCard(
                      pack: e.value,
                      onBuy: () => _buy('credit_package', e.value['code'] as String),
                    ).animate().fadeIn(delay: (80 * e.key).ms).moveX(begin: 12, end: 0)),
              ],
            ),
    );
  }

  Widget _balanceCard(ThemeData t) {
    final ink = t.colorScheme.onSurface;
    final bg = t.colorScheme.surface;
    final balance = _wallet?['balance']?.toString() ?? '—';
    return Container(
      padding: const EdgeInsets.all(Insets.lg),
      decoration: BoxDecoration(color: ink, borderRadius: Radii.card),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              AppIcon(AppIcons.wallet, size: 20, color: bg),
              const SizedBox(width: Insets.sm),
              Text('موجودی اعتبار', style: t.textTheme.labelMedium?.copyWith(color: bg)),
            ],
          ),
          const SizedBox(height: Insets.md),
          Text(balance,
              style: t.textTheme.displaySmall?.copyWith(color: bg, fontSize: 40)),
        ],
      ),
    );
  }
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({required this.plan, required this.onBuy});
  final Map<String, dynamic> plan;
  final VoidCallback onBuy;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final free = (num.tryParse(plan['price'].toString()) ?? 0) == 0;
    return Container(
      margin: const EdgeInsets.only(bottom: Insets.md),
      padding: const EdgeInsets.all(Insets.md),
      decoration: BoxDecoration(
        color: t.colorScheme.surfaceContainerHighest,
        borderRadius: Radii.card,
        border: Border.all(color: t.colorScheme.outline),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(plan['name'] as String, style: t.textTheme.titleMedium),
                Text('${plan['monthly_credit']} اعتبار در ماه', style: t.textTheme.bodySmall),
              ],
            ),
          ),
          free
              ? Text('فعلی', style: t.textTheme.labelMedium)
              : FilledButton(
                  onPressed: onBuy,
                  style: FilledButton.styleFrom(minimumSize: const Size(96, 44)),
                  child: Text('${plan['price']}'),
                ),
        ],
      ),
    );
  }
}

class _PackCard extends StatelessWidget {
  const _PackCard({required this.pack, required this.onBuy});
  final Map<String, dynamic> pack;
  final VoidCallback onBuy;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final bonus = (num.tryParse(pack['bonus_credit'].toString()) ?? 0) > 0;
    return Container(
      margin: const EdgeInsets.only(bottom: Insets.md),
      padding: const EdgeInsets.all(Insets.md),
      decoration: BoxDecoration(
        color: t.colorScheme.surfaceContainerHighest,
        borderRadius: Radii.card,
        border: Border.all(color: t.colorScheme.outline),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(pack['name'] as String, style: t.textTheme.titleMedium),
                Text(
                  '${pack['credit_amount']} اعتبار${bonus ? ' + ${pack['bonus_credit']} هدیه' : ''}',
                  style: t.textTheme.bodySmall,
                ),
              ],
            ),
          ),
          FilledButton(
            onPressed: onBuy,
            style: FilledButton.styleFrom(minimumSize: const Size(96, 44)),
            child: Text('${pack['price']}'),
          ),
        ],
      ),
    );
  }
}

class _PaySheet extends StatelessWidget {
  const _PaySheet({required this.url});
  final String url;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(Insets.lg),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('پرداخت', style: t.textTheme.titleLarge),
          const SizedBox(height: Insets.sm),
          Text(
            'برای تکمیل خرید، درگاه پرداخت باز می‌شود. پس از پرداخت، اعتبار به‌صورت خودکار و با تأیید سرور اضافه می‌شود.',
            style: t.textTheme.bodyMedium,
          ),
          const SizedBox(height: Insets.md),
          SelectableText(url, style: t.textTheme.bodySmall),
          const SizedBox(height: Insets.lg),
          FilledButton(
            onPressed: () => Navigator.of(context).maybePop(),
            child: const Text('باشه'),
          ),
        ],
      ),
    );
  }
}
