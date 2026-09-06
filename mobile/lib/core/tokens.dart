import 'package:flutter/widgets.dart';

/// Design tokens — spacing, radius, durations. Standard, restrained sizing
/// (not too large, not too small) for a premium minimal feel.
class Insets {
  static const double xs = 6;
  static const double sm = 10;
  static const double md = 16;
  static const double lg = 24;
  static const double xl = 36;
}

class Radii {
  static const double sm = 10;
  static const double md = 16;
  static const double lg = 22;
  static const double pill = 999;

  static const BorderRadius card = BorderRadius.all(Radius.circular(md));
  static const BorderRadius button = BorderRadius.all(Radius.circular(14));
}

class Motion {
  static const Duration fast = Duration(milliseconds: 180);
  static const Duration normal = Duration(milliseconds: 320);
  static const Duration slow = Duration(milliseconds: 520);
}

/// Standard text sizes (logical px). Balanced hierarchy.
class TextSizes {
  static const double display = 30;
  static const double title = 20;
  static const double subtitle = 16;
  static const double body = 15;
  static const double label = 13;
  static const double caption = 12;
}
