import 'dart:convert';
import 'dart:math';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:http/http.dart' as http;

// ===== BALANCE =====
class BalanceService {
  static final Map<String, int> _soldes = {};
  static int solde(String u) => _soldes[u] ?? 0;
  static bool retirerSolde(String u, int m) {
    if (solde(u) < m) return false;
    _soldes[u] = solde(u) - m;
    return true;
  }
  static void rechargerSolde(String u, int m) => _soldes[u] = solde(u) + m;
}

// ===== TRADUCTION =====
class TranslationService {
  static final Map<String, String> _cache = {};

  static Future<void> traduireTexte(
      String texte, String langue, Function(String) cb) async {
    final key = "$langue|$texte";
    if (_cache.containsKey(key)) {
      cb(_cache[key]!);
      return;
    }
    try {
      final url = Uri.parse(
        "https://translate.googleapis.com/translate_a/single"
        "?client=gtx&sl=auto&tl=$langue&dt=t&q=${Uri.encodeComponent(texte)}",
      );
      final res = await http.get(url);
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        final translated =
            (data[0] as List).map((e) => e[0] as String).join();
        _cache[key] = translated;
        cb(translated);
      } else {
        cb(texte);
      }
    } catch (_) {
      cb(texte);
    }
  }
}

// ===== MODELES =====
class CommentTikTokModel {
  String id, videoId, text, user;
  int time;
  List<String> likes;
  List<CommentTikTokModel> replies;
  CommentTikTokModel({
    this.id = "",
    this.videoId = "",
    this.text = "",
    this.user = "",
    this.time = 0,
    List<String>? likes,
    List<CommentTikTokModel>? replies,
  })  : likes = likes ?? [],
        replies = replies ?? [];

  Map<String, dynamic> toMap() => {
        'id': id,
        'videoId': videoId,
        'text': text,
        'user': user,
        'time': time,
        'likes': likes,
      };

  static CommentTikTokModel fromMap(Map m) => CommentTikTokModel(
        id: m['id'] ?? '',
        videoId: m['videoId'] ?? '',
        text: m['text'] ?? '',
        user: m['user'] ?? '',
        time: m['time'] ?? 0,
        likes: List<String>.from(m['likes'] ?? []),
      );
}

class GiftModel {
  String from, type;
  int value, time;
  GiftModel({this.from = "", this.type = "", this.value = 0, this.time = 0});

  Map<String, dynamic> toMap() =>
      {'from': from, 'type': type, 'value': value, 'time': time};

  static GiftModel fromMap(Map m) => GiftModel(
        from: m['from'] ?? '',
        type: m['type'] ?? '',
        value: m['value'] ?? 0,
        time: m['time'] ?? 0,
      );
}

class VideoTikTokModel {
  String id, titre, owner, url, image, sound, challenge;
  List<String> hashtags;
  int prix;
  List<String> vues, likes, wouah, haha, triste, colere, saves, partages, duets;
  List<CommentTikTokModel> comments;
  List<GiftModel> gifts;
  bool isLive;
  int time;

  VideoTikTokModel({
    this.id = "",
    this.titre = "",
    this.owner = "",
    this.url = "",
    this.image = "",
    this.sound = "",
    this.challenge = "",
    this.prix = 200,
    List<String>? hashtags,
    List<String>? vues,
    List<String>? likes,
    List<String>? wouah,
    List<String>? haha,
    List<String>? triste,
    List<String>? colere,
    List<String>? saves,
    List<String>? partages,
    List<String>? duets,
    List<CommentTikTokModel>? comments,
    List<GiftModel>? gifts,
    this.isLive = false,
    this.time = 0,
  })  : hashtags = hashtags ?? [],
        vues = vues ?? [],
        likes = likes ?? [],
        wouah = wouah ?? [],
        haha = haha ?? [],
        triste = triste ?? [],
        colere = colere ?? [],
        saves = saves ?? [],
        partages = partages ?? [],
        duets = duets ?? [],
        comments = comments ?? [],
        gifts = gifts ?? [];

  Map<String, dynamic> toMap() => {
        'id': id,
        'titre': titre,
        'owner': owner,
        'url': url,
        'image': image,
        'sound': sound,
        'hashtags': hashtags,
        'challenge': challenge,
        'prix': prix,
        'vues': vues,
        'likes': likes,
        'wouah': wouah,
        'haha': haha,
        'triste': triste,
        'colere': colere,
        'saves': saves,
        'partages': partages,
        'duets': duets,
        'comments': comments.map((e) => e.toMap()).toList(),
        'gifts': gifts.map((e) => e.toMap()).toList(),
        'isLive': isLive,
        'time': time,
      };

  static VideoTikTokModel fromMap(Map m) => VideoTikTokModel(
        id: m['id'] ?? '',
        titre: m['titre'] ?? '',
        owner: m['owner'] ?? '',
        url: m['url'] ?? '',
        image: m['image'] ?? '',
        sound: m['sound'] ?? '',
        hashtags: List<String>.from(m['hashtags'] ?? []),
        challenge: m['challenge'] ?? '',
        prix: m['prix'] ?? 200,
        vues: List<String>.from(m['vues'] ?? []),
        likes: List<String>.from(m['likes'] ?? []),
        wouah: List<String>.from(m['wouah'] ?? []),
        haha: List<String>.from(m['haha'] ?? []),
        triste: List<String>.from(m['triste'] ?? []),
        colere: List<String>.from(m['colere'] ?? []),
        saves: List<String>.from(m['saves'] ?? []),
        partages: List<String>.from(m['partages'] ?? []),
        duets: List<String>.from(m['duets'] ?? []),
        comments: (m['comments'] as List? ?? [])
            .map((e) => CommentTikTokModel.fromMap(e))
            .toList(),
        gifts: (m['gifts'] as List? ?? [])
            .map((e) => GiftModel.fromMap(e))
            .toList(),
        isLive: m['isLive'] ?? false,
        time: m['time'] ?? 0,
      );
}

// ===== SERVICE PUBSLOOK =====
class PubslookService {
  static List<VideoTikTokModel> videos = [];
  static List<VideoTikTokModel> fyp = [];
  static int currentIndex = 0;
  static final db = FirebaseFirestore.instance;

  static Stream<List<VideoTikTokModel>> streamVideos() {
    return db.collection("videos").snapshots().map((s) {
      videos = s.docs
          .map((d) => VideoTikTokModel.fromMap({...d.data(), 'id': d.id}))
          .toList();
      refreshFYP();
      return videos;
    });
  }

  static Future<bool> creerVideo(String titre, String owner, String url,
      String sound, List<String> tags, int prix) async {
    if (!BalanceService.retirerSolde(owner, prix)) return false;
    final v = VideoTikTokModel(
      id: genId(),
      titre: titre,
      owner: owner,
      url: url,
      sound: sound,
      hashtags: tags,
      prix: prix,
      time: now(),
    );
    videos.insert(0, v);
    await db.collection("videos").doc(v.id).set(v.toMap());
    refreshFYP();
    BalanceService.rechargerSolde("Admin", prix);
    return true;
  }

  static void supprimer(String id) {
    videos.removeWhere((e) => e.id == id);
    db.collection("videos").doc(id).delete();
    refreshFYP();
  }

  static VideoTikTokModel? videoById(String id) {
    try {
      return videos.firstWhere((e) => e.id == id);
    } catch (_) {
      return null;
    }
  }

  static void refreshFYP() {
    fyp.clear();
    final sorted = List<VideoTikTokModel>.from(videos);
    int score(VideoTikTokModel v) =>
        v.vues.length * 3 +
        v.likes.length * 5 +
        v.comments.length * 4 +
        v.partages.length * 6;
    sorted.sort((a, b) => score(b).compareTo(score(a)));
    fyp.addAll(sorted.take(100));
  }

  static List<VideoTikTokModel> forYou() {
    if (fyp.isEmpty) refreshFYP();
    return fyp;
  }

  static VideoTikTokModel? next() {
    if (fyp.isEmpty) return null;
    currentIndex = (currentIndex + 1) % fyp.length;
    return fyp[currentIndex];
  }

  static VideoTikTokModel? prev() {
    if (fyp.isEmpty) return null;
    currentIndex = currentIndex - 1 < 0 ? fyp.length - 1 : currentIndex - 1;
    return fyp[currentIndex];
  }

  static void voir(String id, String u) {
    final v = videoById(id);
    if (v == null) return;
    if (!v.vues.contains(u)) {
      v.vues.add(u);
      db.collection("videos").doc(id).update({'vues': v.vues});
      BalanceService.rechargerSolde(u, 5);
      BalanceService.rechargerSolde(v.owner, 15);
    }
  }

  static void like(String id, String u) {
    final v = videoById(id);
    if (v != null && !v.likes.contains(u)) {
      v.likes.add(u);
      db.collection("videos").doc(id).update({'likes': v.likes});
      BalanceService.rechargerSolde(v.owner, 3);
    }
  }

  static void unlike(String id, String u) {
    final v = videoById(id);
    v?.likes.remove(u);
    if (v != null) db.collection("videos").doc(id).update({'likes': v.likes});
  }

  static bool aLike(String id, String u) =>
      videoById(id)?.likes.contains(u) ?? false;

  static void commenter(String id, String u, String tx) {
    final v = videoById(id);
    if (v == null) return;
    v.comments.add(CommentTikTokModel(
        id: genId(), videoId: id, text: tx, user: u, time: now()));
    db.collection("videos").doc(id).update({
      'comments': v.comments.map((e) => e.toMap()).toList(),
    });
    BalanceService.rechargerSolde(v.owner, 15);
  }

  static List<CommentTikTokModel> listerComments(String id) =>
      videoById(id)?.comments ?? [];

  static void partager(String id, String u) {
    final v = videoById(id);
    if (v == null) return;
    if (!v.partages.contains(u)) {
      v.partages.add(u);
      db.collection("videos").doc(id).update({'partages': v.partages});
      BalanceService.rechargerSolde(u, 10);
      BalanceService.rechargerSolde(v.owner, 20);
    }
  }

  static void save(String id, String u) {
    final v = videoById(id);
    if (v == null) return;
    if (!v.saves.contains(u)) {
      v.saves.add(u);
      db.collection("videos").doc(id).update({'saves': v.saves});
    }
  }

  static void envoyerGift(String vId, String from, String type, int value) {
    final v = videoById(vId);
    if (v == null) return;
    v.gifts.add(GiftModel(from: from, type: type, value: value, time: now()));
    db.collection("videos").doc(vId).update({
      'gifts': v.gifts.map((e) => e.toMap()).toList(),
    });
    BalanceService.retirerSolde(from, value);
    BalanceService.rechargerSolde(v.owner, (value * 80 / 100).toInt());
  }

  static int cVues(String id) => videoById(id)?.vues.length ?? 0;
  static int cLikes(String id) => videoById(id)?.likes.length ?? 0;
  static int cComs(String id) => videoById(id)?.comments.length ?? 0;
  static int cParts(String id) => videoById(id)?.partages.length ?? 0;
  static int totalGifts(String id) =>
      videoById(id)?.gifts.fold<int>(0, (s, e) => s + e.value) ?? 0;
  static int engagement(String id) =>
      cVues(id) + cLikes(id) * 3 + cComs(id) * 4;

  static List<VideoTikTokModel> videosUser(String o) =>
      videos.where((e) => e.owner == o).toList();

  static List<VideoTikTokModel> search(String q) => videos
      .where((e) =>
          e.titre.toLowerCase().contains(q.toLowerCase()) ||
          e.hashtags
              .any((t) => t.toLowerCase().contains(q.toLowerCase())))
      .take(20)
      .toList();

  static void traduireVideoTitre(
      String id, String langueCible, Function(String) onResult) {
    final v = videoById(id);
    if (v == null) return;
    TranslationService.traduireTexte(v.titre, langueCible, onResult);
  }

  static String genId() =>
      DateTime.now().millisecondsSinceEpoch.toString() +
      Random().nextInt(999).toString();

  static int now() => DateTime.now().millisecondsSinceEpoch;
}
