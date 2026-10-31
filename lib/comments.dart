import 'package:flutter/material.dart';
import 'services.dart';
import 'auth.dart';

class CommentsSheet extends StatefulWidget {
  final String videoId;
  const CommentsSheet({super.key, required this.videoId});

  static Future<void> show(BuildContext context, String videoId) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => CommentsSheet(videoId: videoId),
    );
  }

  @override
  State<CommentsSheet> createState() => _CommentsSheetState();
}

class _CommentsSheetState extends State<CommentsSheet> {
  final _ctrl = TextEditingController();

  void _send() {
    final user = AuthService.user;
    if (user == null) return;
    final tx = _ctrl.text.trim();
    if (tx.isEmpty) return;
    PubslookService.commenter(
      widget.videoId,
      user.displayName ?? user.email ?? "user",
      tx,
    );
    _ctrl.clear();
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final comments = PubslookService.listerComments(widget.videoId);
    final bottom = MediaQuery.of(context).viewInsets.bottom;

    return Padding(
      padding: EdgeInsets.only(bottom: bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.7,
        child: Column(children: [
          Container(
            width: 40,
            height: 4,
            margin: const EdgeInsets.symmetric(vertical: 10),
            decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(2)),
          ),
          Text("${comments.length} commentaires",
              style: const TextStyle(fontWeight: FontWeight.bold)),
          const Divider(),
          Expanded(
            child: comments.isEmpty
                ? const Center(
                    child: Text("Aucun commentaire, sois le premier !"))
                : ListView.builder(
                    itemCount: comments.length,
                    itemBuilder: (_, i) {
                      final c = comments[i];
                      return ListTile(
                        leading: CircleAvatar(
                          backgroundColor: const Color(0xFFE53935),
                          child: Text(
                            c.user.isNotEmpty
                                ? c.user[0].toUpperCase()
                                : "?",
                            style: const TextStyle(color: Colors.white),
                          ),
                        ),
                        title: Text(c.user,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold)),
                        subtitle: Text(c.text),
                      );
                    },
                  ),
          ),
          const Divider(height: 1),
          Padding(
            padding: const EdgeInsets.all(8),
            child: Row(children: [
              Expanded(
                child: TextField(
                  controller: _ctrl,
                  decoration: InputDecoration(
                    hintText: "Ajouter un commentaire...",
                    filled: true,
                    fillColor: const Color(0xFFF0F2F5),
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(24),
                        borderSide: BorderSide.none),
                    contentPadding:
                        const EdgeInsets.symmetric(horizontal: 16),
                  ),
                  onSubmitted: (_) => _send(),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.send, color: Color(0xFFE53935)),
                onPressed: _send,
              ),
            ]),
          ),
        ]),
      ),
    );
  }
}
