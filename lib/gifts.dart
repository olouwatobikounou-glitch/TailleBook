import 'package:flutter/material.dart';
import 'services.dart';
import 'auth.dart';

class GiftCatalog {
  static const gifts = [
    {'type': 'Rose', 'emoji': '🌹', 'value': 10},
    {'type': 'Café', 'emoji': '☕', 'value': 50},
    {'type': 'Glace', 'emoji': '🍦', 'value': 100},
    {'type': 'Diamant', 'emoji': '💎', 'value': 500},
    {'type': 'Couronne', 'emoji': '👑', 'value': 1000},
    {'type': 'Fusée', 'emoji': '🚀', 'value': 5000},
  ];
}

class GiftSheet extends StatelessWidget {
  final String videoId;
  const GiftSheet({super.key, required this.videoId});

  static Future<void> show(BuildContext context, String videoId) {
    return showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => GiftSheet(videoId: videoId),
    );
  }

  void _send(BuildContext context, String type, int value) {
    final user = AuthService.user;
    if (user == null) return;
    final name = user.displayName ?? user.email ?? "user";
    PubslookService.envoyerGift(videoId, name, type, value);
    Navigator.pop(context);
    GiftOverlay.show(context, type);
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Text("Envoyer un cadeau",
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          GridView.count(
            crossAxisCount: 3,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            children: GiftCatalog.gifts.map((g) {
              return InkWell(
                onTap: () =>
                    _send(context, g['type'] as String, g['value'] as int),
                child: Container(
                  decoration: BoxDecoration(
                      color: const Color(0xFFF0F2F5),
                      borderRadius: BorderRadius.circular(12)),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(g['emoji'] as String,
                          style: const TextStyle(fontSize: 32)),
                      const SizedBox(height: 4),
                      Text(g['type'] as String,
                          style:
                              const TextStyle(fontWeight: FontWeight.bold)),
                      Text("${g['value']} F",
                          style: const TextStyle(
                              fontSize: 11, color: Colors.grey)),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        ]),
      ),
    );
  }
}

class GiftOverlay {
  static void show(BuildContext context, String giftType) {
    final entry = OverlayEntry(builder: (_) => const _FloatingGift());
    Overlay.of(context).insert(entry);
    Future.delayed(const Duration(seconds: 3), () {
      if (entry.mounted) entry.remove();
    });
  }
}

class _FloatingGift extends StatefulWidget {
  const _FloatingGift();
  @override
  State<_FloatingGift> createState() => _FloatingGiftState();
}

class _FloatingGiftState extends State<_FloatingGift>
    with SingleTickerProviderStateMixin {
  late AnimationController _c;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(
        vsync: this, duration: const Duration(seconds: 3))
      ..forward();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) {
        final t = _c.value;
        return Positioned(
          right: 20 + (t * 60) - (t * t * 120),
          bottom: -100 + t * (size.height + 200),
          child: Opacity(
            opacity: t < 0.8 ? 1 : (1 - t) * 5,
            child: Transform.scale(
              scale: 1 + (t < 0.2 ? t * 5 : 1),
              child: const Text("🎁", style: TextStyle(fontSize: 40)),
            ),
          ),
        );
      },
    );
  }
}
