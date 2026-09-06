import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

/// Minimal line icons as inline SVG (spec: "use SVG for icons").
/// Rendered monochrome and recolored to the current theme's ink color.
class AppIcons {
  static const _head =
      '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" '
      'stroke="#111" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">';
  static const _tail = '</svg>';

  static String _wrap(String body) => '$_head$body$_tail';

  static final chat = _wrap(
      '<path d="M4 5h16v11H8l-4 4V5z"/>');
  static final sparkle = _wrap(
      '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3z"/>'
      '<path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z"/>');
  static final image = _wrap(
      '<rect x="3" y="4" width="18" height="16" rx="2"/>'
      '<circle cx="8.5" cy="9.5" r="1.6"/>'
      '<path d="M21 16l-5-5-8 8"/>');
  static final wallet = _wrap(
      '<rect x="3" y="6" width="18" height="13" rx="2.5"/>'
      '<path d="M3 9h18"/><circle cx="17" cy="13.5" r="1.4"/>');
  static final settings = _wrap(
      '<circle cx="12" cy="12" r="3.2"/>'
      '<path d="M12 3v2.5M12 18.5V21M4.2 6.5l1.8 1.8M18 15.7l1.8 1.8M3 12h2.5M18.5 12H21M4.2 17.5l1.8-1.8M18 8.3l1.8-1.8"/>');
  static final send = _wrap(
      '<path d="M21 3L3 11l7 2 2 7 9-17z"/>');
  static final add = _wrap('<path d="M12 5v14M5 12h14"/>');
  static final file = _wrap(
      '<path d="M7 3h7l5 5v13H7V3z"/><path d="M14 3v5h5"/>');
  static final mic = _wrap(
      '<rect x="9" y="3" width="6" height="11" rx="3"/>'
      '<path d="M5 11a7 7 0 0014 0M12 18v3"/>');
  static final search = _wrap(
      '<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4-4"/>');
  static final chevronRight = _wrap('<path d="M9 5l7 7-7 7"/>');
  static final chevronLeft = _wrap('<path d="M15 5l-7 7 7 7"/>');
  static final user = _wrap(
      '<circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/>');
  static final crown = _wrap(
      '<path d="M4 8l4 4 4-6 4 6 4-4v9H4V8z"/>');
  static final bolt = _wrap('<path d="M13 3L5 13h5l-1 8 8-11h-5l1-7z"/>');
  static final brain = _wrap(
      '<path d="M9 4a3 3 0 00-3 3 3 3 0 00-1 5 3 3 0 003 4h1V4H9z"/>'
      '<path d="M15 4a3 3 0 013 3 3 3 0 011 5 3 3 0 01-3 4h-1V4h1z"/>');
  static final telescope = _wrap(
      '<path d="M3 14l11-5 2 4-11 5-2-4z"/><path d="M14 9l4-2 1.5 3-4 2"/>'
      '<path d="M8 15l-2 6M12 13l2 8"/>');
  static final palette = _wrap(
      '<path d="M12 3a9 9 0 100 18c1.5 0 2-1 2-2s-1-1.5-1-2.5S14 13 16 13h1a4 4 0 004-4c0-3.5-4-6-9-6z"/>'
      '<circle cx="7.5" cy="10.5" r="1"/><circle cx="12" cy="7.5" r="1"/><circle cx="16" cy="10" r="1"/>');
  static final check = _wrap('<path d="M5 12l5 5L20 6"/>');
  static final logout = _wrap(
      '<path d="M14 4H6v16h8"/><path d="M17 8l4 4-4 4M21 12H9"/>');
  static final sun = _wrap(
      '<circle cx="12" cy="12" r="4"/>'
      '<path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M19 5l-1.5 1.5M6.5 17.5L5 19"/>');
}

class AppIcon extends StatelessWidget {
  const AppIcon(this.svg, {super.key, this.size = 22, this.color});

  final String svg;
  final double size;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final c = color ?? Theme.of(context).colorScheme.onSurface;
    return SvgPicture.string(
      svg,
      width: size,
      height: size,
      colorFilter: ColorFilter.mode(c, BlendMode.srcIn),
    );
  }
}

/// Theme-aware logo — recolored to the ink color so it reads on black or white.
class AppLogo extends StatelessWidget {
  const AppLogo({super.key, this.size = 96, this.color});

  final double size;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final c = color ?? Theme.of(context).colorScheme.onSurface;
    return SvgPicture.asset(
      'assets/logo/deepgap_logo.svg',
      width: size,
      height: size,
      colorFilter: ColorFilter.mode(c, BlendMode.srcIn),
    );
  }
}
