import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/icons.dart';
import '../../core/tokens.dart';
import '../auth/auth_flow.dart' show errorMessage;

class ImagePage extends StatefulWidget {
  const ImagePage({super.key, this.modelCode = 'designer'});
  final String modelCode;

  @override
  State<ImagePage> createState() => _ImagePageState();
}

class _ImagePageState extends State<ImagePage> {
  final _prompt = TextEditingController();
  bool _loading = false;
  List<String> _urls = [];
  String? _charge;

  @override
  void dispose() {
    _prompt.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    final prompt = _prompt.text.trim();
    if (prompt.isEmpty || _loading) return;
    setState(() => _loading = true);
    try {
      final res = await context
          .read<AppState>()
          .api
          .generateImage(prompt, modelCode: widget.modelCode);
      setState(() {
        _urls = (res.data['urls'] as List).cast<String>();
        _charge = res.data['charged_credit'].toString();
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
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
        title: const Text('طراح — تولید تصویر'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(Insets.lg),
        children: [
          Text('چه تصویری بسازیم؟', style: t.textTheme.titleLarge),
          const SizedBox(height: Insets.md),
          TextField(
            controller: _prompt,
            minLines: 2,
            maxLines: 5,
            decoration: const InputDecoration(
              hintText: 'مثال: یک گربهٔ فضانورد با سبک مینیمال سیاه‌وسفید',
            ),
          ),
          const SizedBox(height: Insets.md),
          FilledButton(
            onPressed: _loading ? null : _generate,
            child: _loading
                ? const SizedBox(
                    height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.4))
                : const Text('تولید تصویر'),
          ),
          if (_charge != null) ...[
            const SizedBox(height: Insets.sm),
            Center(child: Text('$_charge اعتبار کسر شد', style: t.textTheme.bodySmall)),
          ],
          const SizedBox(height: Insets.lg),
          ..._urls.map(
            (u) => Padding(
              padding: const EdgeInsets.only(bottom: Insets.md),
              child: ClipRRect(
                borderRadius: Radii.card,
                child: AspectRatio(
                  aspectRatio: 1,
                  child: Container(
                    color: t.colorScheme.surfaceContainerHighest,
                    child: Image.network(
                      u,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Center(
                        child: AppIcon(AppIcons.image, size: 40, color: t.colorScheme.onSurfaceVariant),
                      ),
                      loadingBuilder: (_, child, p) =>
                          p == null ? child : const Center(child: CircularProgressIndicator()),
                    ),
                  ),
                ),
              ),
            ).animate().fadeIn().scale(begin: const Offset(0.97, 0.97), end: const Offset(1, 1)),
          ),
        ],
      ),
    );
  }
}
