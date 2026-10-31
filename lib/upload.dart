import 'dart:io';
import 'package:flutter/material.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:image_picker/image_picker.dart';
import 'services.dart';
import 'auth.dart';

class UploadPage extends StatefulWidget {
  const UploadPage({super.key});
  @override
  State<UploadPage> createState() => _UploadPageState();
}

class _UploadPageState extends State<UploadPage> {
  final _titre = TextEditingController();
  final _tags = TextEditingController();
  File? _video;
  bool _uploading = false;
  double _progress = 0;

  Future<void> _pick() async {
    final picker = ImagePicker();
    final f = await picker.pickVideo(source: ImageSource.gallery);
    if (f != null) setState(() => _video = File(f.path));
  }

  Future<void> _publish() async {
    if (_video == null || _titre.text.isEmpty) return;
    final user = AuthService.user;
    if (user == null) return;

    setState(() {
      _uploading = true;
      _progress = 0;
    });
    try {
      final ref = FirebaseStorage.instance.ref(
          'videos/${DateTime.now().millisecondsSinceEpoch}.mp4');
      final task = ref.putFile(_video!);
      task.snapshotEvents.listen((s) {
        setState(() => _progress = s.bytesTransferred /
            (s.totalBytes == 0 ? 1 : s.totalBytes));
      });
      await task;
      final url = await ref.getDownloadURL();

      final tagsList = _tags.text
          .split(RegExp(r'[ ,#]+'))
          .where((e) => e.isNotEmpty)
          .toList();

      final ok = await PubslookService.creerVideo(
        _titre.text,
        user.displayName ?? user.email ?? "user",
        url,
        "son original",
        tagsList,
        200,
      );

      if (!mounted) return;
      if (ok) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text("Vidéo publiée !")));
        Navigator.pop(context);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text("Solde insuffisant (200 requis)")));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text("Erreur: $e")));
      }
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Nouvelle vidéo")),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        GestureDetector(
          onTap: _pick,
          child: Container(
            height: 200,
            decoration: BoxDecoration(
                color: const Color(0xFFEEEEEE),
                borderRadius: BorderRadius.circular(12)),
            child: Center(
              child: Text(_video == null
                  ? "📹 Choisir une vidéo"
                  : "✅ ${_video!.path.split('/').last}"),
            ),
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _titre,
          decoration: const InputDecoration(
              labelText: "Titre", border: OutlineInputBorder()),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _tags,
          decoration: const InputDecoration(
              labelText: "Hashtags (ex: business benin)",
              border: OutlineInputBorder()),
        ),
        const SizedBox(height: 20),
        if (_uploading) LinearProgressIndicator(value: _progress),
        const SizedBox(height: 20),
        ElevatedButton(
          onPressed: _uploading ? null : _publish,
          style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFE53935),
              padding: const EdgeInsets.symmetric(vertical: 14)),
          child: Text(_uploading ? "Upload..." : "Publier (200 F)",
              style: const TextStyle(color: Colors.white)),
        ),
      ]),
    );
  }
}
