import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'services.dart';
import 'auth.dart';
import 'upload.dart';
import 'comments.dart';
import 'gifts.dart';

// ===== CACHE GLOBAL =====
class Cache {
  static List<Post> posts = [];
  static final db = FirebaseFirestore.instance;
}

class Post {
  String id, text, user;
  List<String> likes;
  Post(this.id, this.text, this.user, this.likes);
}

class TranslationCenter {
  static ValueNotifier<String> langueActuelle = ValueNotifier("fr");
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp();
  } catch (_) {}

  Cache.posts = [
    Post(
      "1",
      "Profite d'un pique-nique d'automne 🍂 Entre croissants frais, café chaud et bons fous rires, journée parfaite à l'extérieur! #Automne #PiqueNique",
      "Thomas Bernard",
      List.filled(24, "like"),
    ),
  ];
  PubslookService.videos = [
    VideoTikTokModel(
      id: "demo1",
      titre: "Formation 500k/mois #business #benin",
      owner: "Admin",
      sound: "son original",
    ),
  ];
  PubslookService.refreshFYP();

  runApp(const TailleBookApp());
}

class TailleBookApp extends StatelessWidget {
  const TailleBookApp({super.key});
  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: AuthGate(child: MainPage()),
    );
  }
}

class MainPage extends StatefulWidget {
  const MainPage({super.key});
  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int onglet = 0;

  @override
  void initState() {
    super.initState();
    _initFirebase();
  }

  Future<void> _initFirebase() async {
    try {
      Cache.db.collection("posts").snapshots().listen((s) {
        if (s.docs.isNotEmpty) {
          setState(() {
            Cache.posts = s.docs.map((d) {
              final data = d.data();
              final likesRaw = data['likes'];
              return Post(
                d.id,
                data['text'] ?? '',
                data['user'] ?? '',
                likesRaw is List ? List<String>.from(likesRaw) : <String>[],
              );
            }).toList();
          });
        }
      });

      PubslookService.streamVideos().listen((list) {
        if (list.isNotEmpty && mounted) setState(() {});
      });

      final uid = FirebaseAuth.instance.currentUser?.uid;
      if (uid != null) {
        final doc = await Cache.db.collection("users").doc(uid).get();
        final lang = doc.data()?['lang'];
        if (lang != null) TranslationCenter.langueActuelle.value = lang;
      }
    } catch (e) {
      debugPrint("Firebase init error: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<String>(
      valueListenable: TranslationCenter.langueActuelle,
      builder: (_, langue, __) {
        return Scaffold(
          bottomNavigationBar: NavigationBar(
            selectedIndex: onglet,
            onDestinationSelected: (i) => setState(() => onglet = i),
            destinations: [
              NavigationDestination(
                  icon: const Text("🏠"),
                  label: langue == "en" ? "Home" : "Accueil"),
              NavigationDestination(
                  icon: const Text("👥"),
                  label: langue == "en" ? "Friends" : "Amis"),
              const NavigationDestination(icon: Text("🛒"), label: "Market"),
              const NavigationDestination(icon: Text("🔥"), label: "Pubslook"),
              NavigationDestination(
                  icon: const Text("👤"),
                  label: langue == "en" ? "Profile" : "Profil"),
            ],
          ),
          body: <Widget>[
            _buildFeed(langue),
            _buildSoon("Amis"),
            _buildSoon("Market"),
            _buildPubslook(),
            _buildProfil(),
          ][onglet],
        );
      },
    );
  }

  Widget _buildSoon(String t) => Center(
        child: Text("$t bientôt",
            style:
                const TextStyle(fontWeight: FontWeight.bold, fontSize: 20)),
      );

  // ===== ONGLET 0 - ACCUEIL =====
  Widget _buildFeed(String langue) {
    return Column(children: [
      Container(
        color: Colors.white,
        padding: const EdgeInsets.all(14),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text("TailleBook",
                style: TextStyle(
                    color: Color(0xFFE53935),
                    fontSize: 28,
                    fontWeight: FontWeight.w900)),
            Row(children: const [
              Text("🔍", style: TextStyle(fontSize: 22)),
              SizedBox(width: 16),
              Text("💬", style: TextStyle(fontSize: 22)),
              SizedBox(width: 16),
              Text("🔔", style: TextStyle(fontSize: 22)),
            ]),
          ],
        ),
      ),
      Expanded(
        child: Container(
          color: const Color(0xFFF0F2F5),
          child: ListView.builder(
            itemCount: Cache.posts.length + 1,
            itemBuilder: (_, i) {
              if (i == 0) return _buildComposer(langue);
              final p = Cache.posts[i - 1];
              return _buildPostCard(p);
            },
          ),
        ),
      ),
    ]);
  }

  Widget _buildComposer(String langue) {
    return Column(children: [
      Card(
        margin: const EdgeInsets.all(12),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Row(children: [
            Container(
                width: 38,
                height: 38,
                decoration: const BoxDecoration(
                    color: Color(0xFFD7CCC8), shape: BoxShape.circle)),
            const SizedBox(width: 12),
            Expanded(
              child: Container(
                height: 36,
                decoration: BoxDecoration(
                    color: const Color(0xFFF0F2F5),
                    borderRadius: BorderRadius.circular(20)),
                child: Padding(
                  padding: const EdgeInsets.only(left: 12),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      langue == "en"
                          ? "What's on your mind?"
                          : "À quoi pensez-vous?",
                      style: const TextStyle(color: Colors.grey),
                    ),
                  ),
                ),
              ),
            ),
            const Text("📷"),
          ]),
        ),
      ),
      Container(
        color: Colors.white,
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("Stories",
                style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            SizedBox(
              height: 130,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: 5,
                itemBuilder: (_, j) => Column(children: [
                  Container(
                    width: 70,
                    height: 110,
                    margin: const EdgeInsets.only(right: 10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFE0E0E0),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                          color: const Color(0xFFE53935), width: 2),
                    ),
                    child: j == 0
                        ? Align(
                            alignment: Alignment.bottomCenter,
                            child: Container(
                              width: 22,
                              height: 22,
                              decoration: const BoxDecoration(
                                  color: Color(0xFFE53935),
                                  shape: BoxShape.circle),
                              child: const Center(
                                  child: Text("+",
                                      style: TextStyle(color: Colors.white))),
                            ))
                        : null,
                  ),
                  Text(
                    ["Votre story", "Lucas", "Émilie", "Yann", "Sophie"][j],
                    style: const TextStyle(fontSize: 10),
                  ),
                ]),
              ),
            ),
          ],
        ),
      ),
    ]);
  }

  Widget _buildPostCard(Post p) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Container(
                  width: 40,
                  height: 40,
                  decoration: const BoxDecoration(
                      color: Color(0xFFE53935), shape: BoxShape.circle)),
              const SizedBox(width: 8),
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(p.user,
                    style: const TextStyle(fontWeight: FontWeight.bold)),
                const Text("Il y a 1 h • 🌍",
                    style: TextStyle(fontSize: 11, color: Colors.grey)),
              ]),
              const Spacer(),
              const Icon(Icons.more_horiz),
            ]),
            const SizedBox(height: 8),
            Text(p.text),
            const SizedBox(height: 10),
            Container(
              height: 200,
              width: double.infinity,
              decoration: BoxDecoration(
                  color: const Color(0xFFEEEEEE),
                  borderRadius: BorderRadius.circular(12)),
              child: const Center(child: Text("🖼️ Image")),
            ),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text("❤️ ${p.likes.length}",
                    style: const TextStyle(fontSize: 12, color: Colors.grey)),
                const Text("8 commentaires • 3 partages",
                    style: TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
            const Divider(),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: const [
                Text("👍 J'aime",
                    style: TextStyle(
                        color: Color(0xFFE53935),
                        fontWeight: FontWeight.bold)),
                Text("💬 Commenter"),
                Text("↗️ Partager"),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // ===== ONGLET 3 - PUBSLOOK =====
  Widget _buildPubslook() {
    final videos = PubslookService.forYou();
    if (videos.isEmpty) {
      return const Center(child: Text("Aucune vidéo pour l'instant"));
    }
    return Stack(children: [
      PageView.builder(
        scrollDirection: Axis.vertical,
        itemCount: videos.length,
        itemBuilder: (_, i) {
          final v = videos[i];
          return Container(
            color: const Color(0xFF0F0F0F),
            child: Stack(children: [
              Center(
                child: Text("▶️ ${v.titre}",
                    style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 18)),
              ),
              Positioned(
                right: 12,
                bottom: 90,
                child: Column(children: [
                  Container(
                    width: 50,
                    height: 50,
                    decoration: BoxDecoration(
                      color: Colors.grey,
                      shape: BoxShape.circle,
                      border: Border.all(
                          color: const Color(0xFFE53935), width: 2),
                    ),
                  ),
                  const SizedBox(height: 20),
                  GestureDetector(
                    onTap: () =>
                        setState(() => PubslookService.like(v.id, v.owner)),
                    child: const Text("❤️", style: TextStyle(fontSize: 28)),
                  ),
                  Text("${PubslookService.cLikes(v.id)}",
                      style: const TextStyle(
                          color: Colors.white, fontSize: 12)),
                  const SizedBox(height: 20),
                  GestureDetector(
                    onTap: () => CommentsSheet.show(context, v.id),
                    child: const Text("💬", style: TextStyle(fontSize: 26)),
                  ),
                  Text("${PubslookService.cComs(v.id)}",
                      style: const TextStyle(
                          color: Colors.white, fontSize: 12)),
                  const SizedBox(height: 20),
                  const Text("↗️", style: TextStyle(fontSize: 26)),
                  const SizedBox(height: 20),
                  GestureDetector(
                    onTap: () => GiftSheet.show(context, v.id),
                    child: Container(
                      width: 38,
                      height: 38,
                      decoration: const BoxDecoration(
                          color: Color(0xFFFFC107), shape: BoxShape.circle),
                      child: const Center(child: Text("🎁")),
                    ),
                  ),
                ]),
              ),
              Positioned(
                left: 14,
                bottom: 20,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("@${v.owner}",
                        style: const TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.w900)),
                    const SizedBox(height: 6),
                    Text(v.titre,
                        style: const TextStyle(color: Colors.white)),
                    Text("♫ ${v.sound}",
                        style: const TextStyle(
                            color: Colors.white70, fontSize: 12)),
                  ],
                ),
              ),
            ]),
          );
        },
      ),
      SafeArea(
        child: Container(
          color: Colors.black,
          padding: const EdgeInsets.all(12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                      color: const Color(0xFFE53935),
                      borderRadius: BorderRadius.circular(8)),
                  child: const Center(
                      child: Text("P",
                          style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w900))),
                ),
                const SizedBox(width: 6),
                const Text("Pubslook",
                    style: TextStyle(
                        color: Colors.white, fontWeight: FontWeight.bold)),
              ]),
              Row(children: [
                GestureDetector(
                  onTap: () => Navigator.push(context,
                      MaterialPageRoute(builder: (_) => const UploadPage())),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                        color: const Color(0xFFE53935),
                        borderRadius: BorderRadius.circular(10)),
                    child: const Text("+ Créer",
                        style: TextStyle(color: Colors.white, fontSize: 12)),
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                      color: const Color(0xFF222222),
                      borderRadius: BorderRadius.circular(10)),
                  child: const Text("Pour toi",
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.bold)),
                ),
                const SizedBox(width: 8),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                      color: const Color(0xFF222222),
                      borderRadius: BorderRadius.circular(10)),
                  child: const Text("Abonnements",
                      style: TextStyle(color: Colors.grey, fontSize: 12)),
                ),
              ]),
            ],
          ),
        ),
      ),
    ]);
  }

  // ===== ONGLET 4 - PROFIL =====
  Widget _buildProfil() {
    return ListView(padding: const EdgeInsets.all(20), children: [
      const SizedBox(height: 40),
      const Text("Sécurité & Langues",
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
      const SizedBox(height: 20),
      ...["fr: Français", "en: English", "fon: Fon", "yo: Yoruba"].map(
        (e) => ListTile(
          title: Text(e),
          onTap: () =>
              TranslationCenter.langueActuelle.value = e.split(":")[0],
        ),
      ),
      const SizedBox(height: 20),
      ElevatedButton(
        style: ElevatedButton.styleFrom(backgroundColor: Colors.black),
        onPressed: () async {
          await AuthService.signOut();
        },
        child: const Text("Se déconnecter",
            style: TextStyle(color: Colors.white)),
      ),
      const SizedBox(height: 10),
      ElevatedButton(
        style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
        onPressed: () {},
        child: const Text("Supprimer compte",
            style: TextStyle(color: Colors.white)),
      ),
      const SizedBox(height: 20),
      const Text("Privacy: https://sites.google.com/view/taillebook-privacy",
          style: TextStyle(color: Colors.red, fontSize: 11)),
    ]);
  }
}
