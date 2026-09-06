import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../core/icons.dart';
import '../../core/tokens.dart';

class SplashPage extends StatelessWidget {
  const SplashPage({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const AppLogo(size: 108)
                .animate()
                .scale(
                  duration: Motion.slow,
                  curve: Curves.easeOutBack,
                  begin: const Offset(0.7, 0.7),
                  end: const Offset(1, 1),
                )
                .fadeIn(duration: Motion.normal),
            const SizedBox(height: Insets.lg),
            Text('دیپ گپ', style: t.textTheme.displaySmall)
                .animate()
                .fadeIn(delay: 220.ms, duration: Motion.normal)
                .moveY(begin: 10, end: 0),
            const SizedBox(height: Insets.xs),
            Text('دستیار هوش مصنوعی فارسی', style: t.textTheme.bodyMedium)
                .animate()
                .fadeIn(delay: 360.ms, duration: Motion.normal),
          ],
        ),
      ),
    );
  }
}
