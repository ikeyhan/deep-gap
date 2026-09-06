import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/icons.dart';
import '../../core/router.dart';
import '../../core/tokens.dart';
import '../auth/auth_flow.dart' show errorMessage;
import '../chat/chat_page.dart';
import '../wallet/wallet_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<Map<String, dynamic>> _models = [];
  String? _selected;
  String _balance = '—';
  bool _loading = true;
  bool _starting = false;

  final Map<String, String> _iconFor = {
    'fast': AppIcons.bolt,
    'smart': AppIcons.brain,
    'researcher': AppIcons.telescope,
    'designer': AppIcons.palette,
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = context.read<AppState>().api;
    try {
      final results = await Future.wait([api.models(), api.wallet()]);
      final models = (results[0].data as List).cast<Map<String, dynamic>>();
      setState(() {
        _models = models;
        _selected = models.isNotEmpty ? models.first['code'] as String : null;
        _balance = (results[1].data['balance']).toString();
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _startChat() async {
    if (_selected == null || _starting) return;
    setState(() => _starting = true);
    try {
      final api = context.read<AppState>().api;
      final res = await api.createConversation(modelCode: _selected);
      final id = res.data['id'] as String;
      if (!mounted) return;
      await context.pushFade(ChatPage(conversationId: id, modelCode: _selected!));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
      }
    } finally {
      if (mounted) setState(() => _starting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : CustomScrollView(
                slivers: [
                  SliverToBoxAdapter(child: _header(t)),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(Insets.lg, 0, Insets.lg, Insets.md),
                    sliver: SliverToBoxAdapter(
                      child: Text('چه کاری انجام دهیم؟', style: t.textTheme.titleLarge)
                          .animate()
                          .fadeIn(delay: 120.ms),
                    ),
                  ),
                  SliverPadding(
                    padding: const EdgeInsets.symmetric(horizontal: Insets.lg),
                    sliver: SliverGrid(
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 2,
                        mainAxisSpacing: Insets.md,
                        crossAxisSpacing: Insets.md,
                        childAspectRatio: 1.35,
                      ),
                      delegate: SliverChildBuilderDelegate(
                        (context, i) {
                          final m = _models[i];
                          return _PersonaCard(
                            model: m,
                            svg: _iconFor[m['code']] ?? AppIcons.sparkle,
                            selected: _selected == m['code'],
                            onTap: () => setState(() => _selected = m['code'] as String),
                          ).animate().fadeIn(delay: (80 * i).ms).scale(
                                begin: const Offset(0.96, 0.96),
                                end: const Offset(1, 1),
                                curve: Curves.easeOut,
                              );
                        },
                        childCount: _models.length,
                      ),
                    ),
                  ),
                  const SliverToBoxAdapter(child: SizedBox(height: Insets.xl)),
                ],
              ),
      ),
      bottomNavigationBar: _loading
          ? null
          : SafeArea(
              minimum: const EdgeInsets.all(Insets.lg),
              child: FilledButton(
                onPressed: _starting ? null : _startChat,
                child: _starting
                    ? const SizedBox(
                        height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.4))
                    : const Text('گفت‌وگوی جدید'),
              ),
            ),
    );
  }

  Widget _header(ThemeData t) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Insets.lg, Insets.md, Insets.lg, Insets.lg),
      child: Row(
        children: [
          const AppLogo(size: 34),
          const SizedBox(width: Insets.sm),
          Text('دیپ گپ', style: t.textTheme.titleLarge),
          const Spacer(),
          _CreditChip(balance: _balance, onTap: () => context.pushFade(const WalletPage())),
          IconButton(
            onPressed: context.read<AppState>().toggleTheme,
            icon: AppIcon(AppIcons.sun, size: 20),
          ),
          IconButton(
            onPressed: () => context.read<AppState>().logout(),
            icon: AppIcon(AppIcons.logout, size: 20),
          ),
        ],
      ),
    ).animate().fadeIn().moveY(begin: -8, end: 0);
  }
}

class _CreditChip extends StatelessWidget {
  const _CreditChip({required this.balance, required this.onTap});
  final String balance;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return InkWell(
      borderRadius: BorderRadius.circular(Radii.pill),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: Insets.md, vertical: 8),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(Radii.pill),
          border: Border.all(color: t.colorScheme.outline),
        ),
        child: Row(
          children: [
            AppIcon(AppIcons.wallet, size: 16),
            const SizedBox(width: 6),
            Text(balance, style: t.textTheme.labelMedium?.copyWith(color: t.colorScheme.onSurface)),
          ],
        ),
      ),
    );
  }
}

class _PersonaCard extends StatelessWidget {
  const _PersonaCard({
    required this.model,
    required this.svg,
    required this.selected,
    required this.onTap,
  });

  final Map<String, dynamic> model;
  final String svg;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final ink = t.colorScheme.onSurface;
    final bg = t.colorScheme.surface;
    final premium = model['is_premium'] == true;
    return AnimatedContainer(
      duration: Motion.fast,
      curve: Curves.easeOut,
      decoration: BoxDecoration(
        color: selected ? ink : t.colorScheme.surfaceContainerHighest,
        borderRadius: Radii.card,
        border: Border.all(color: selected ? ink : t.colorScheme.outline, width: selected ? 2 : 1),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: Radii.card,
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(Insets.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    AppIcon(svg, size: 26, color: selected ? bg : ink),
                    const Spacer(),
                    if (premium) AppIcon(AppIcons.crown, size: 16, color: selected ? bg : ink),
                  ],
                ),
                const Spacer(),
                Text(
                  model['display_name'] as String,
                  style: t.textTheme.titleMedium?.copyWith(color: selected ? bg : ink),
                ),
                const SizedBox(height: 2),
                Text(
                  (model['description'] ?? '') as String,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: t.textTheme.bodySmall?.copyWith(
                    color: selected ? bg.withOpacity(0.85) : t.colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
