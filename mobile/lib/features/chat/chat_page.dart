import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/icons.dart';
import '../../core/tokens.dart';
import '../auth/auth_flow.dart' show errorMessage;

class _Msg {
  _Msg(this.role, this.content, {this.pending = false});
  final String role; // user | assistant
  String content;
  bool pending;
}

class ChatPage extends StatefulWidget {
  const ChatPage({super.key, required this.conversationId, required this.modelCode});
  final String conversationId;
  final String modelCode;

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  final _input = TextEditingController();
  final _scroll = ScrollController();
  final List<_Msg> _messages = [];
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  @override
  void dispose() {
    _input.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _loadHistory() async {
    try {
      final res = await context.read<AppState>().api.messages(widget.conversationId);
      final items = (res.data as List).cast<Map<String, dynamic>>();
      setState(() {
        _messages
          ..clear()
          ..addAll(items.map((m) => _Msg(m['role'] as String, m['content'] as String)));
      });
      _jump();
    } catch (_) {/* new conversation: no history yet */}
  }

  void _jump() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(_scroll.position.maxScrollExtent,
            duration: Motion.normal, curve: Curves.easeOut);
      }
    });
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (text.isEmpty || _sending) return;
    _input.clear();
    setState(() {
      _messages.add(_Msg('user', text));
      _messages.add(_Msg('assistant', '', pending: true));
      _sending = true;
    });
    _jump();
    try {
      final res = await context
          .read<AppState>()
          .api
          .sendMessage(widget.conversationId, text, modelCode: widget.modelCode);
      final reply = res.data['assistant_message']['content'] as String;
      setState(() {
        _messages.last
          ..content = reply
          ..pending = false;
      });
    } catch (e) {
      setState(() {
        _messages.last
          ..content = errorMessage(e)
          ..pending = false;
      });
    } finally {
      if (mounted) setState(() => _sending = false);
      _jump();
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
        title: Text(_modelName(widget.modelCode)),
      ),
      body: Column(
        children: [
          Expanded(
            child: _messages.isEmpty
                ? _empty(t)
                : ListView.builder(
                    controller: _scroll,
                    padding: const EdgeInsets.all(Insets.md),
                    itemCount: _messages.length,
                    itemBuilder: (_, i) => _Bubble(msg: _messages[i]),
                  ),
          ),
          _composer(t),
        ],
      ),
    );
  }

  Widget _empty(ThemeData t) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const AppLogo(size: 72),
          const SizedBox(height: Insets.md),
          Text('پیام خود را بنویسید', style: t.textTheme.bodyMedium),
        ],
      ).animate().fadeIn(),
    );
  }

  Widget _composer(ThemeData t) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(Insets.md, Insets.xs, Insets.md, Insets.sm),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Expanded(
              child: TextField(
                controller: _input,
                minLines: 1,
                maxLines: 5,
                textInputAction: TextInputAction.newline,
                decoration: const InputDecoration(hintText: 'بنویسید…'),
              ),
            ),
            const SizedBox(width: Insets.sm),
            _SendButton(enabled: !_sending, onTap: _send),
          ],
        ),
      ),
    );
  }

  String _modelName(String code) => const {
        'fast': 'سریع',
        'smart': 'هوشمند',
        'researcher': 'پژوهشگر',
        'designer': 'طراح',
      }[code] ??
      'گفت‌وگو';
}

class _SendButton extends StatelessWidget {
  const _SendButton({required this.enabled, required this.onTap});
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Material(
      color: t.colorScheme.onSurface,
      shape: const CircleBorder(),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: enabled ? onTap : null,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: AppIcon(AppIcons.send, size: 22, color: t.colorScheme.surface),
        ),
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.msg});
  final _Msg msg;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final isUser = msg.role == 'user';
    final ink = t.colorScheme.onSurface;
    final bg = isUser ? ink : t.colorScheme.surfaceContainerHighest;
    final fg = isUser ? t.colorScheme.surface : ink;

    final content = msg.pending
        ? const _TypingDots()
        : (isUser
            ? Text(msg.content, style: t.textTheme.bodyLarge?.copyWith(color: fg))
            : MarkdownBody(
                data: msg.content,
                styleSheet: MarkdownStyleSheet(
                  p: t.textTheme.bodyLarge?.copyWith(color: fg),
                  code: TextStyle(color: fg, backgroundColor: t.colorScheme.surface),
                ),
              ));

    return Align(
      alignment: isUser ? Alignment.centerLeft : Alignment.centerRight,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 5),
        padding: const EdgeInsets.symmetric(horizontal: Insets.md, vertical: Insets.sm),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.82),
        decoration: BoxDecoration(
          color: bg,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(Radii.md),
            topRight: const Radius.circular(Radii.md),
            bottomLeft: Radius.circular(isUser ? 4 : Radii.md),
            bottomRight: Radius.circular(isUser ? Radii.md : 4),
          ),
          border: isUser ? null : Border.all(color: t.colorScheme.outline),
        ),
        child: content,
      ),
    )
        .animate()
        .fadeIn(duration: Motion.fast)
        .moveY(begin: 8, end: 0, curve: Curves.easeOut);
  }
}

class _TypingDots extends StatefulWidget {
  const _TypingDots();
  @override
  State<_TypingDots> createState() => _TypingDotsState();
}

class _TypingDotsState extends State<_TypingDots> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 900))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ink = Theme.of(context).colorScheme.onSurface;
    return SizedBox(
      width: 44,
      height: 18,
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, __) => Row(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(3, (i) {
            final v = ((_c.value + i * 0.2) % 1.0);
            final o = (0.3 + 0.7 * (1 - (v - 0.5).abs() * 2)).clamp(0.3, 1.0);
            return Container(
              margin: const EdgeInsets.symmetric(horizontal: 3),
              width: 7,
              height: 7,
              decoration: BoxDecoration(
                color: ink.withOpacity(o),
                shape: BoxShape.circle,
              ),
            );
          }),
        ),
      ),
    );
  }
}
