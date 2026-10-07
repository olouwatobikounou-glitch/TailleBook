package com.taillebook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import java.util.*

object TranslationCenter {

    // 20 LANGUES MONDIALES - Code officiel Google
    val LANGUES = linkedMapOf(
        "fr" to "🇫🇷 Français",
        "en" to "🇺🇸 English",
        "fon" to "🇧🇯 Fɔngbè",
        "yo" to "🇳🇬 Yorùbá",
        "ig" to "🇳🇬 Igbo",
        "ha" to "🇳🇬 Hausa",
        "sw" to "🇹🇿 Kiswahili",
        "ar" to "🇸🇦 العربية",
        "pt" to "🇵🇹 Português",
        "es" to "🇪🇸 Español",
        "de" to "🇩🇪 Deutsch",
        "zh" to "🇨🇳 中文",
        "hi" to "🇮🇳 हिन्दी",
        "tr" to "🇹🇷 Türkçe",
        "ru" to "🇷🇺 Русский",
        "it" to "🇮🇹 Italiano",
        "nl" to "🇳🇱 Nederlands",
        "ja" to "🇯🇵 日本語",
        "ko" to "🇰🇷 한국어",
        "ln" to "🇨🇩 Lingala"
    )

    // Langue actuelle - lisible partout dans l'app
    var langueActuelle = mutableStateOf("fr")

    init {
        // Au lancement, charge la langue sauvegardée du user
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid!= null) {
            Cache.db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val lang = doc.getString("lang")
                if (lang!= null && LANGUES.containsKey(lang)) {
                    langueActuelle.value = lang
                }
            }
        }
    }

    // SAUVEGARDE LANGUE
    fun changerLangue(code: String) {
        if (!LANGUES.containsKey(code)) return
        langueActuelle.value = code
        val uid = FirebaseAuth.getInstance().currentUser?.uid?: return
        Cache.db.collection("users").document(uid).update("lang", code)
    }

    // TRADUCTION TEXTE - Compatible avec tes Post
    fun traduireTexte(texte: String, vers: String, onResult: (String) -> Unit) {
        if (texte.isBlank()) { onResult(texte); return }
        if (vers == "fon" || vers == "yo" || vers == "ig" || vers == "ln") {
            // Langues africaines pas dans ML Kit -> on garde original pour V1
            onResult(texte)
            return
        }
        try {
            val source = TranslateLanguage.FRENCH // On part du français par défaut
            val target = when(vers){
                "fr" -> TranslateLanguage.FRENCH
                "en" -> TranslateLanguage.ENGLISH
                "ar" -> TranslateLanguage.ARABIC
                "pt" -> TranslateLanguage.PORTUGUESE
                "es" -> TranslateLanguage.SPANISH
                "de" -> TranslateLanguage.GERMAN
                "zh" -> TranslateLanguage.CHINESE
                "hi" -> TranslateLanguage.HINDI
                "tr" -> TranslateLanguage.TURKISH
                "ru" -> TranslateLanguage.RUSSIAN
                "it" -> TranslateLanguage.ITALIAN
                "nl" -> TranslateLanguage.DUTCH
                "ja" -> TranslateLanguage.JAPANESE
                "ko" -> TranslateLanguage.KOREAN
                else -> TranslateLanguage.ENGLISH
            }
            val options = TranslatorOptions.Builder()
               .setSourceLanguage(source)
               .setTargetLanguage(target)
               .build()
            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded().addOnSuccessListener {
                translator.translate(texte).addOnSuccessListener { traduit ->
                    onResult(traduit)
                }.addOnFailureListener { onResult(texte) }
            }.addOnFailureListener { onResult(texte) }
        } catch (e: Exception) {
            onResult(texte)
        }
    }
}

@Composable
fun SelecteurLangue() {
    var expanded by remember { mutableStateOf(false) }
    val currentLang by TranslationCenter.langueActuelle

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("🌍 Langue / Language", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(TranslationCenter.LANGUES[currentLang]?: currentLang, color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TranslationCenter.LANGUES.forEach { (code, nom) ->
                DropdownMenuItem(
                    text = { Text(nom) },
                    onClick = {
                        TranslationCenter.changerLangue(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VideoPubslookTraduite(captionOriginal: String, descriptionOriginal: String) {
    val context = LocalContext.current
    var captionAffichee by remember { mutableStateOf(captionOriginal) }
    var isTranslating by remember { mutableStateOf(false) }
    val langue by TranslationCenter.langueActuelle
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    // Initialise la voix
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = when(langue){
                    "fr" -> Locale.FRENCH
                    "en" -> Locale.ENGLISH
                    else -> Locale.FRENCH
                }
            }
        }
        onDispose { tts?.shutdown() }
    }

    // Quand langue change, re-traduit auto
    LaunchedEffect(langue) {
        if (langue!= "fr") {
            isTranslating = true
            TranslationCenter.traduireTexte(captionOriginal, langue) { traduit ->
                captionAffichee = traduit
                isTranslating = false
            }
        } else {
            captionAffichee = captionOriginal
        }
    }

    Card(Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(captionAffichee, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(descriptionOriginal, fontSize = 12.sp, color = Color.Gray)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // BOUTON TRADUIRE TEXTE
                Button(
                    onClick = {
                        isTranslating = true
                        TranslationCenter.traduireTexte(captionOriginal, langue) { traduit ->
                            captionAffichee = traduit
                            isTranslating = false
                        }
                    },
                    enabled =!isTranslating
                ) {
                    Text(if (isTranslating) "..." else "🌐 Traduire en ${TranslationCenter.LANGUES[langue]}")
                }
                // BOUTON ECOUTER EN FRANCAIS / ANGLAIS
                Button(
                    onClick = {
                        tts?.language = if(langue=="fr") Locale.FRENCH else Locale.ENGLISH
                        tts?.speak(captionAffichee, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("🔊 Écouter", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
