import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';

class AuthService {
  static final _auth = FirebaseAuth.instance;
  static final _db = FirebaseFirestore.instance;

  static User? get user => _auth.currentUser;

  static Future<String?> signUp(
      String email, String password, String pseudo) async {
    try {
      final cred = await _auth.createUserWithEmailAndPassword(
          email: email, password: password);
      await _db.collection("users").doc(cred.user!.uid).set({
        'uid': cred.user!.uid,
        'pseudo': pseudo,
        'email': email,
        'lang': 'fr',
        'createdAt': FieldValue.serverTimestamp(),
      });
      await cred.user!.updateDisplayName(pseudo);
      return null;
    } on FirebaseAuthException catch (e) {
      return _msg(e.code);
    }
  }

  static Future<String?> signIn(String email, String password) async {
    try {
      await _auth.signInWithEmailAndPassword(
          email: email, password: password);
      return null;
    } on FirebaseAuthException catch (e) {
      return _msg(e.code);
    }
  }

  static Future<void> signOut() => _auth.signOut();

  static String _msg(String code) {
    switch (code) {
      case 'user-not-found':
        return "Aucun compte avec cet email";
      case 'wrong-password':
        return "Mot de passe incorrect";
      case 'email-already-in-use':
        return "Cet email est déjà utilisé";
      case 'weak-password':
        return "Mot de passe trop faible (6 caractères min)";
      case 'invalid-email':
        return "Email invalide";
      default:
        return "Erreur: $code";
    }
  }
}

class AuthGate extends StatelessWidget {
  final Widget child;
  const AuthGate({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<User?>(
      stream: FirebaseAuth.instance.authStateChanges(),
      builder: (_, snap) {
        if (snap.connectionState == ConnectionState.waiting) {
          return const Scaffold(
              body: Center(child: CircularProgressIndicator()));
        }
        if (snap.data == null) return const LoginPage();
        return child;
      },
    );
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _email = TextEditingController();
  final _pass = TextEditingController();
  final _pseudo = TextEditingController();
  bool _signup = false;
  bool _loading = false;
  String? _err;

  Future<void> _submit() async {
    setState(() {
      _loading = true;
      _err = null;
    });
    final err = _signup
        ? await AuthService.signUp(
            _email.text.trim(), _pass.text, _pseudo.text.trim())
        : await AuthService.signIn(_email.text.trim(), _pass.text);
    if (!mounted) return;
    setState(() {
      _loading = false;
      _err = err;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF0F2F5),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Card(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const Text("TailleBook",
                    style: TextStyle(
                        color: Color(0xFFE53935),
                        fontSize: 32,
                        fontWeight: FontWeight.w900)),
                const SizedBox(height: 20),
                TextField(
                  controller: _email,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                      labelText: "Email", border: OutlineInputBorder()),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _pass,
                  obscureText: true,
                  decoration: const InputDecoration(
                      labelText: "Mot de passe",
                      border: OutlineInputBorder()),
                ),
                if (_signup) ...[
                  const SizedBox(height: 12),
                  TextField(
                    controller: _pseudo,
                    decoration: const InputDecoration(
                        labelText: "Pseudo", border: OutlineInputBorder()),
                  ),
                ],
                if (_err != null) ...[
                  const SizedBox(height: 8),
                  Text(_err!, style: const TextStyle(color: Colors.red)),
                ],
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _loading ? null : _submit,
                    style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFE53935),
                        padding: const EdgeInsets.symmetric(vertical: 14)),
                    child: _loading
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white))
                        : Text(_signup ? "Créer un compte" : "Se connecter",
                            style: const TextStyle(color: Colors.white)),
                  ),
                ),
                TextButton(
                  onPressed: () => setState(() => _signup = !_signup),
                  child: Text(_signup
                      ? "Déjà un compte ? Se connecter"
                      : "Pas de compte ? S'inscrire"),
                ),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}
