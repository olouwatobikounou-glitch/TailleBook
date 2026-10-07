package com.taillebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity(){
    override fun onCreate(b: Bundle?){
        super.onCreate(b)
        FirebaseApp.initializeApp(this)
        Cache.initDefault()

        // FIREBASE LIVE - POSTS & VIDEOS
        Cache.db.collection("posts").addSnapshotListener{ s,_ ->
            if(s!=null &&!s.isEmpty){
                Cache.posts = s.toObjects(Post::class.java).toMutableList()
            }
        }
        Cache.db.collection("videos").addSnapshotListener{ s,_ ->
            if(s!=null &&!s.isEmpty){
                Cache.videos = s.toObjects(Pubslook.VideoTikTok::class.java).toMutableList()
                Pubslook.refreshFYP()
            }
        }

        // CHARGER LANGUE USER AU DEMARRAGE
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            Cache.db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val lang = doc.getString("lang")
                if(lang!=null) TranslationCenter.langueActuelle.value = lang
            }
        }

        if(Cache.posts.isEmpty()){
            Posts.creerPost("Profite d'un pique-nique d'automne 🍂 Entre croissants frais, café chaud et bons fous rires, journée parfaite à l'extérieur! #Automne #PiqueNique","Thomas Bernard")
        }
        if(Pubslook.videos.isEmpty()){
            Pubslook.creerVideo("Formation 500k/mois #business #benin","Admin","video1.mp4","son original",listOf("#business","#benin"),200)
        }

        setContent{
            var onglet by remember { mutableStateOf(0) }
            val langue by TranslationCenter.langueActuelle
            MaterialTheme{
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = Color.White){
                            NavigationBarItem(selected = onglet==0, onClick={onglet=0}, icon={Text("🏠")}, label={Text(if(langue=="en") "Home" else "Accueil", fontSize=10.sp)})
                            NavigationBarItem(selected = onglet==1, onClick={onglet=1}, icon={Text("👥")}, label={Text(if(langue=="en") "Friends" else "Amis", fontSize=10.sp)})
                            NavigationBarItem(selected = onglet==2, onClick={onglet=2}, icon={Text("🛒")}, label={Text("Market", fontSize=10.sp)})
                            NavigationBarItem(selected = onglet==3, onClick={onglet=3}, icon={Text("🔥")}, label={Text("Pubslook", fontSize=10.sp)})
                            NavigationBarItem(selected = onglet==4, onClick={onglet=4}, icon={Text("👤")}, label={Text(if(langue=="en") "Profile" else "Profil", fontSize=10.sp)})
                        }
                    }
                ){ pad ->
                    Box(Modifier.fillMaxSize().padding(pad)){
                        when(onglet){
                            0 -> {
                                Column(Modifier.fillMaxSize().background(Color(0xFFF0F2F5))){
                                    Row(Modifier.fillMaxWidth().background(Color.White).padding(14.dp),
                                        horizontalArrangement=Arrangement.SpaceBetween,
                                        verticalAlignment=Alignment.CenterVertically){
                                        Text("TailleBook", color=Color(0xFFE53935), fontSize=28.sp, fontWeight=FontWeight.ExtraBold)
                                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                                            Text("🔍", fontSize=22.sp); Text("💬", fontSize=22.sp); Text("🔔", fontSize=22.sp)
                                        }
                                    }
                                    LazyColumn(Modifier.fillMaxSize()){
                                        item{
                                            Card(Modifier.fillMaxWidth().padding(12.dp), shape=RoundedCornerShape(28.dp),
                                                colors=CardDefaults.cardColors(Color.White), elevation=CardDefaults.cardElevation(3.dp)){
                                                Row(Modifier.padding(10.dp), verticalAlignment=Alignment.CenterVertically){
                                                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFD7CCC8)))
                                                    Box(Modifier.weight(1f).padding(horizontal=12.dp).height(36.dp)
                                                      .clip(RoundedCornerShape(20.dp)).background(Color(0xFFF0F2F5)),
                                                        contentAlignment=Alignment.CenterStart){
                                                        Text(if(langue=="en") " What's on your mind?" else " A quoi pensez-vous?", color=Color.Gray, fontSize=14.sp)
                                                    }
                                                    Text("📷", color=Color(0xFFE53935), fontSize=18.sp)
                                                }
                                            }
                                            Card(Modifier.fillMaxWidth().padding(bottom=6.dp), colors=CardDefaults.cardColors(Color.White)){
                                                Column(Modifier.padding(12.dp)){
                                                    Text("Stories", fontWeight=FontWeight.Bold, fontSize=16.sp)
                                                    Spacer(Modifier.height(8.dp))
                                                    LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                                                        items(Stories.nomsStories().size){ i->
                                                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                                                Box(Modifier.size(70.dp,110.dp).clip(RoundedCornerShape(16.dp))
                                                                  .background(Color(0xFFE0E0E0)).border(2.dp, Color(0xFFE53935), RoundedCornerShape(16.dp))){
                                                                    if(i==0) Box(Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFE53935)).align(Alignment.BottomCenter).offset(y=(-6).dp),
                                                                        contentAlignment=Alignment.Center){ Text("+", color=Color.White, fontWeight=FontWeight.Bold) }
                                                                }
                                                                Spacer(Modifier.height(4.dp))
                                                                Text(Stories.nomsStories()[i], fontSize=10.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        items(Posts.postsFeed().size){ i-> val p=Posts.postsFeed()[i]
                                            Card(Modifier.fillMaxWidth().padding(vertical=4.dp), colors=CardDefaults.cardColors(Color.White)){
                                                Column(Modifier.padding(14.dp)){
                                                    Row(verticalAlignment=Alignment.CenterVertically){
                                                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE53935)))
                                                        Column(Modifier.padding(start=8.dp)){
                                                            Text(p.user, fontWeight=FontWeight.Bold, fontSize=15.sp)
                                                            Text("Il y a 1 h • 🌍", fontSize=11.sp, color=Color.Gray)
                                                        }
                                                        Spacer(Modifier.weight(1f))
                                                        BoutonSignaler(p.id)
                                                    }
                                                    Spacer(Modifier.height(8.dp)); Text(p.text, fontSize=14.sp, lineHeight=18.sp)
                                                    Spacer(Modifier.height(10.dp))
                                                    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEEEEE)), contentAlignment=Alignment.Center){
                                                        Text("🖼️ Pique-nique automne", color=Color.Gray)
                                                    }
                                                    Spacer(Modifier.height(10.dp))
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){
                                                        Text("❤️ ${p.likes.size.coerceAtLeast(24)}", fontSize=12.sp, color=Color.Gray)
                                                        Text("8 commentaires • 3 partages", fontSize=12.sp, color=Color.Gray)
                                                    }
                                                    Divider(Modifier.padding(vertical=8.dp))
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceAround){
                                                        TextButton(onClick={Reactions.likerPost(p.id,"","Admin")}){ Text("👍 J'aime", color=Color(0xFFE53935), fontWeight=FontWeight.Bold) }
                                                        TextButton(onClick={Comments.ajouterComment(p.id,"Top!","Admin")}){ Text("💬 Commenter", color=Color.Gray) }
                                                        TextButton(onClick={Pubslook.partager(p.id,"Admin")}){ Text("↗️ Partager", color=Color.Gray) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                val vids = Pubslook.forYou()
                                Box(Modifier.fillMaxSize().background(Color.Black)){
                                    LazyColumn(Modifier.fillMaxSize()){
                                        items(vids.size){ i-> val v=vids[i]
                                            Box(Modifier.fillMaxWidth().height(750.dp).background(Color(0xFF0F0F0F))){
                                                Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){
                                                    Text("▶️ ${v.titre}", color=Color.White, fontWeight=FontWeight.Bold)
                                                }
                                                Column(Modifier.align(Alignment.CenterEnd).padding(end=12.dp, bottom=90.dp),
                                                    verticalArrangement=Arrangement.spacedBy(20.dp), horizontalAlignment=Alignment.CenterHorizontally){
                                                    Box{
                                                        Box(Modifier.size(50.dp).clip(CircleShape).background(Color.Gray).border(2.dp, Color(0xFFE53935), CircleShape))
                                                        Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFE53935)).align(Alignment.BottomEnd),
                                                            contentAlignment=Alignment.Center){ Text("+", color=Color.White, fontSize=10.sp) }
                                                    }
                                                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                                                        TextButton(onClick={Pubslook.like(v.id,"Admin")}, contentPadding=PaddingValues(0.dp)){
                                                            Text(if(Pubslook.aLike(v.id,"Admin"))"❤️" else "🤍", fontSize=28.sp)
                                                        }
                                                        Text("${Pubslook.cLikes(v.id)}", color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                                                        Text("💬", fontSize=26.sp); Text("${Pubslook.cComs(v.id)}", color=Color.White, fontSize=12.sp)
                                                    }
                                                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                                                        Text("↗️", fontSize=26.sp); Text("${Pubslook.cParts(v.id)}", color=Color.White, fontSize=12.sp)
                                                    }
                                                    Text("🔖", fontSize=24.sp)
                                                    TextButton(onClick={Pubslook.envoyerGift(v.id,"Admin","🌹",50)}){
                                                        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFC107)), contentAlignment=Alignment.Center){ Text("🎁") }
                                                    }
                                                }
                                                // ICI LA TRADUCTION VIDEO AJOUTEE
                                                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)){
                                                    Text("@${v.owner}", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.ExtraBold)
                                                    Spacer(Modifier.height(6.dp))
                                                    VideoPubslookTraduite(
                                                        captionOriginal = v.titre,
                                                        descriptionOriginal = "♫ ${v.sound} • son original"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth().background(Color.Black).padding(12.dp).align(Alignment.TopCenter),
                                        horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE53935)), contentAlignment=Alignment.Center){
                                                Text("P", color=Color.White, fontWeight=FontWeight.ExtraBold)
                                            }
                                            Spacer(Modifier.width(6.dp)); Text("Pubslook", color=Color.White, fontWeight=FontWeight.Bold, fontSize=18.sp)
                                        }
                                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF222222)).padding(10.dp,6.dp)){
                                                Text("Pour toi", color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Bold)
                                            }
                                            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF222222)).padding(10.dp,6.dp)){
                                                Text("Abonnements", color=Color.Gray, fontSize=12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                // PROFIL - AVEC LANGUES + SECURITE
                                LazyColumn(Modifier.fillMaxSize().background(Color.White)){
                                    item { EcranSecurite() }
                                }
                            }
                            else -> Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){
                                Text("${listOf("Accueil","Amis","Market","Pubslook","Profil")[onglet]} bientôt", fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
