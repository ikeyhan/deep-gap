import 'package:flutter/material.dart';

import 'tokens.dart';

/// Animated page transition used across the whole app (fade + gentle slide),
/// so moving between sections always feels alive.
Route<T> fadeThroughRoute<T>(Widget page) {
  return PageRouteBuilder<T>(
    transitionDuration: Motion.normal,
    reverseTransitionDuration: Motion.fast,
    pageBuilder: (_, __, ___) => page,
    transitionsBuilder: (_, animation, __, child) {
      final curved = CurvedAnimation(parent: animation, curve: Curves.easeOutCubic);
      return FadeTransition(
        opacity: curved,
        child: SlideTransition(
          position: Tween<Offset>(
            begin: const Offset(0, 0.03),
            end: Offset.zero,
          ).animate(curved),
          child: child,
        ),
      );
    },
  );
}

extension NavX on BuildContext {
  Future<T?> pushFade<T>(Widget page) =>
      Navigator.of(this).push<T>(fadeThroughRoute<T>(page));

  Future<T?> replaceFade<T>(Widget page) =>
      Navigator.of(this).pushReplacement(fadeThroughRoute<T>(page));
}
