package com.taillebook
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore

data class Compte(var pseudo:String="", var mdp:String="", var isBlue:Boolean=false, var solde:Int=2000, var bio:String="", var lang:String="fr")
data class Post(val id:String="", var text:String="", val user:String="", val time:Long=0, val image:String="", val likes:MutableList<String> = mutableListOf(), val isPremium:Boolean=false, val hidden:Boolean=false)
data class Produit(val id:String="", val nom:String="", val prix:Int=0, val vendeur:String="")
data class Comment(val id:String="", val postId:String="", var text:String="", val user:String="", val time:Long=0)
data class Message(val id:String="", val from:String="", val to:String="", val text:String="", val time:Long=0)
data class Story(val id:String="", val user:String="", val url:String="", val time:Long=0, val viewers:MutableList<String> = mutableListOf())
data class Groupe(val id:String="", val nom:String="", val owner:String="", val membres:MutableList<String> = mutableListOf(), val posts:MutableList<String> = mutableListOf())
data class Call(val id:String="", val from:String="", val to:String="", val type:String="", val time:Long=0, var duree:Int=0)
data class Notif(val id:String="", val to:String="", val text:String="", val time:Long=0, var lue:Boolean=false)
data class Live(val id:String="", val titre:String="", val host:String="", val viewers:MutableList<String> = mutableListOf(), val time:Long=0)
data class Event(val id:String="", val nom:String="", val date:String="", val owner:String="", val participants:MutableList<String> = mutableListOf())

object Cache{
    var comptes = mutableListOf<Compte>()
    var posts = mutableListOf<Post>()
    var produits = mutableListOf<Produit>()
    var videos = mutableListOf<Pubslook.VideoTikTok>()
    val db = FirebaseFirestore.getInstance()

    fun initDefault(){
        if(comptes.isEmpty()){
            comptes.add(Compte("Admin","1234",true,10000,"","fr"))
            comptes.add(Compte("Thomas Bernard","1234",false,5000,"","fr"))
            db.collection("users").addSnapshotListener{ s,_ -> if(s!=null &&!s.isEmpty){ comptes = s.toObjects(Compte::class.java).toMutableList() } }
            db.collection("posts").addSnapshotListener{ s,_ -> if(s!=null &&!s.isEmpty){ posts = s.toObjects(Post::class.java).toMutableList() } }
            db.collection("products").addSnapshotListener{ s,_ -> if(s!=null &&!s.isEmpty){ produits = s.toObjects(Produit::class.java).toMutableList() } }
        }
    }
}

object Utils{ fun genId()=UUID.randomUUID().toString().take(8); fun now()=System.currentTimeMillis(); fun hashMdp(m:String)=m.hashCode().toString() }

object Analytics{
    fun nbComptes()=Cache.comptes.size; fun nbPosts()=Cache.posts.size; fun nbProduits()=Cache.produits.size
    fun moyenneLikes()=if(Cache.posts.isEmpty())0.0 else Cache.posts.map{it.likes.size}.average()
    fun topPost()=Cache.posts.maxByOrNull{it.likes.size}
    fun topUser()=Cache.comptes.maxByOrNull{it.solde}
    fun statsGlobales()=mapOf("users" to nbComptes(),"posts" to nbPosts())
    fun totalFcfa()=Cache.comptes.sumOf{it.solde}
    fun tauxEngagement()=moyenneLikes()/nbComptes().coerceAtLeast(1)
    fun rapport()="U:${nbComptes()} P:${nbPosts()} F:${totalFcfa()}"
}

object Posts{
    fun creerPost(t:String,u:String,img:String=""){ val p=Post(Utils.genId(),t,u,Utils.now(),img); Cache.posts.add(p); Cache.db.collection("posts").document(p.id).set(p) }
    fun creerPremium(t:String,p:Int,u:String){ val po=Post(Utils.genId(),t,u,Utils.now(),mutableListOf<String>().toString(),mutableListOf(),true); Cache.posts.add(po); Cache.db.collection("posts").document(po.id).set(po) }
    fun postsFeed()=Cache.posts.filter{!it.hidden}.reversed()
    fun postsUser(u:String)=Cache.posts.filter{it.user==u}
    fun supprimerPost(id:String){ Cache.posts.removeIf{it.id==id}; Cache.db.collection("posts").document(id).delete() }
    fun editerPost(id:String,nt:String){ Cache.posts.find{it.id==id}?.let{ Cache.posts[Cache.posts.indexOf(it)]=it.copy(text=nt); Cache.db.collection("posts").document(id).update("text",nt) } }
    fun nbPosts()=Cache.posts.size; fun nbPostsUser(u:String)=postsUser(u).size; fun postById(id:String)=Cache.posts.find{it.id==id}; fun estPremium(id:String)=postById(id)?.isPremium?:false
}

object Reactions{
    fun likerPost(id:String,e:String,u:String,prem:Boolean=false){ Cache.posts.find{it.id==id}?.likes?.add(u); Cache.db.collection("posts").document(id).update("likes",Cache.posts.find{it.id==id}?.likes) }
    fun unlikerPost(id:String,u:String){ Cache.posts.find{it.id==id}?.likes?.remove(u); Cache.db.collection("posts").document(id).update("likes",Cache.posts.find{it.id==id}?.likes) }
    fun compterLikes(p:Post)=p.likes.size; fun compterLikesById(id:String)=Cache.posts.find{it.id==id}?.likes?.size?:0; fun aLike(id:String,u:String)=Cache.posts.find{it.id==id}?.likes?.contains(u)?:false
    fun listerLikers(id:String)=Cache.posts.find{it.id==id}?.likes?:emptyList(); fun topLiked()=Cache.posts.maxByOrNull{it.likes.size}; fun totalLikes()=Cache.posts.sumOf{it.likes.size}
    fun reactionsParUser(u:String)=Cache.posts.count{it.likes.contains(u)}; fun clearLikes(id:String){ Cache.posts.find{it.id==id}?.likes?.clear(); Cache.db.collection("posts").document(id).update("likes",listOf<String>()) }
}

object Comments{
    val comments=mutableListOf<Comment>()
    init{ Cache.db.collection("comments").addSnapshotListener{ s,_ -> if(s!=null) comments = s.toObjects(Comment::class.java).toMutableList() } }
    fun ajouterComment(pId:String,t:String,u:String){ val c=Comment(Utils.genId(),pId,t,u,Utils.now()); comments.add(c); Cache.db.collection("comments").document(c.id).set(c) }
    fun listerComments(pId:String)=comments.filter{it.postId==pId}; fun compterComments(pId:String)=listerComments(pId).size
    fun supprimerComment(id:String){ comments.removeIf{it.id==id}; Cache.db.collection("comments").document(id).delete() }
    fun editerComment(id:String,t:String){ comments.find{it.id==id}?.let{ comments[comments.indexOf(it)]=it.copy(text=t); Cache.db.collection("comments").document(id).update("text",t) } }
    fun commentsUser(u:String)=comments.filter{it.user==u}; fun nbComments()=comments.size; fun dernierComment(pId:String)=listerComments(pId).lastOrNull()
    fun clearPost(pId:String){ comments.filter{it.postId==pId}.forEach{Cache.db.collection("comments").document(it.id).delete()}; comments.removeIf{it.postId==pId} }
    fun topCommenteur()=comments.groupBy{it.user}.maxByOrNull{it.value.size}?.key
}

object Stories{
    val list=mutableListOf<Story>()
    init{ Cache.db.collection("stories").addSnapshotListener{ s,_ -> if(s!=null) list = s.toObjects(Story::class.java).toMutableList() } }
    fun creerStory(u:String,url:String){ val s=Story(Utils.genId(),u,url,Utils.now()); list.add(s); Cache.db.collection("stories").document(s.id).set(s) }
    fun listerStories()=list.filter{Utils.now()-it.time<24*3600*1000}
    fun voirStory(id:String,v:String){ list.find{it.id==id}?.viewers?.add(v); Cache.db.collection("stories").document(id).update("viewers",list.find{it.id==id}?.viewers) }
    fun storiesUser(u:String)=listerStories().filter{it.user==u}; fun supprimerStory(id:String){ list.removeIf{it.id==id}; Cache.db.collection("stories").document(id).delete() }
    fun nbViewers(id:String)=list.find{it.id==id}?.viewers?.size?:0; fun aVu(id:String,u:String)=list.find{it.id==id}?.viewers?.contains(u)?:false
    fun clearExpired(){ list.removeIf{Utils.now()-it.time>24*3600*1000} }; fun nbStories()=listerStories().size; fun topStory()=listerStories().maxByOrNull{it.viewers.size}
    fun nomsStories()=listOf("Votre story","Lucas Martin","Émilie Roux","Yann Dupont","Sophie Lefèvre")
}

object Friends{
    val amis=mutableMapOf<String,MutableList<String>>()
    fun ajouterAmi(a:String,b:String){ amis.getOrPut(a){mutableListOf()}.add(b); amis.getOrPut(b){mutableListOf()}.add(a); Cache.db.collection("friends").document(a+"_"+b).set(mapOf("a" to a,"b" to b)) }
    fun retirerAmi(a:String,b:String){ amis[a]?.remove(b); amis[b]?.remove(a); Cache.db.collection("friends").document(a+"_"+b).delete(); Cache.db.collection("friends").document(b+"_"+a).delete() }
    fun listeAmis(u:String)=amis[u]?:emptyList(); fun estAmi(a:String,b:String)=amis[a]?.contains(b)?:false; fun nbAmis(u:String)=listeAmis(u).size
    fun suggererAmis(u:String)=Cache.comptes.map{it.pseudo}.filter{it!=u &&!estAmi(u,it)}.take(5); fun amisCommuns(a:String,b:String)=listeAmis(a).intersect(listeAmis(b).toSet()).toList()
    fun demandeAmi(f:String,t:String)=ajouterAmi(f,t); fun refuserAmi(a:String,b:String)=retirerAmi(a,b); fun toutAmis()=amis
}

object Groups{
    val groupes=mutableListOf<Groupe>()
    init{ Cache.db.collection("groups").addSnapshotListener{ s,_ -> if(s!=null) groupes = s.toObjects(Groupe::class.java).toMutableList() } }
    fun creerGroupe(n:String,o:String){ val g=Groupe(Utils.genId(),n,o,mutableListOf(o)); groupes.add(g); Cache.db.collection("groups").document(g.id).set(g) }
    fun listerGroupes()=groupes.toList(); fun groupeById(id:String)=groupes.find{it.id==id}
    fun rejoindreGroupe(id:String,u:String){ groupeById(id)?.membres?.add(u); Cache.db.collection("groups").document(id).update("membres",groupeById(id)?.membres) }
    fun quitterGroupe(id:String,u:String){ groupeById(id)?.membres?.remove(u); Cache.db.collection("groups").document(id).update("membres",groupeById(id)?.membres) }
    fun posterDansGroupe(id:String,t:String){ groupeById(id)?.posts?.add(t); Cache.db.collection("groups").document(id).update("posts",groupeById(id)?.posts) }
    fun membresGroupe(id:String)=groupeById(id)?.membres?:emptyList(); fun nbMembres(id:String)=membresGroupe(id).size; fun groupesUser(u:String)=groupes.filter{it.membres.contains(u)}; fun supprimerGroupe(id:String){ groupes.removeIf{it.id==id}; Cache.db.collection("groups").document(id).delete() }
}

object Messages{
    val msgs=mutableListOf<Message>()
    init{ Cache.db.collection("messages").addSnapshotListener{ s,_ -> if(s!=null) msgs = s.toObjects(Message::class.java).toMutableList() } }
    fun envoyerMessage(f:String,t:String,tx:String){ val m=Message(Utils.genId(),f,t,tx,Utils.now()); msgs.add(m); Cache.db.collection("messages").document(m.id).set(m) }
    fun listerMessages(a:String,b:String)=msgs.filter{(it.from==a&&it.to==b)||(it.from==b&&it.to==a)}.sortedBy{it.time}
    fun supprimerMessage(id:String){ msgs.removeIf{it.id==id}; Cache.db.collection("messages").document(id).delete() }
    fun messagesUser(u:String)=msgs.filter{it.from==u||it.to==u}; fun dernierMessage(a:String,b:String)=listerMessages(a,b).lastOrNull()
    fun nbMessages(a:String,b:String)=listerMessages(a,b).size; fun conversations(u:String)=msgs.filter{it.from==u||it.to==u}.map{if(it.from==u)it.to else it.from}.distinct()
    fun clearChat(a:String,b:String){ msgs.filter{(it.from==a&&it.to==b)||(it.from==b&&it.to==a)}.forEach{Cache.db.collection("messages").document(it.id).delete()}; msgs.removeIf{(it.from==a&&it.to==b)||(it.from==b&&it.to==a)} }
    fun nonLus(u:String)=msgs.count{it.to==u}; fun totalMessages()=msgs.size
}

object Calls{
    val calls=mutableListOf<Call>()
    fun callVocal(f:String,t:String){ val c=Call(Utils.genId(),f,t,"vocal",Utils.now()); calls.add(c); Cache.db.collection("calls").document(c.id).set(c) }
    fun callVideo(f:String,t:String){ val c=Call(Utils.genId(),f,t,"video",Utils.now()); calls.add(c); Cache.db.collection("calls").document(c.id).set(c) }
    fun terminerCall(id:String,d:Int){ calls.find{it.id==id}?.duree=d; Cache.db.collection("calls").document(id).update("duree",d) }
    fun historiqueCalls(u:String)=calls.filter{it.from==u||it.to==u}.reversed(); fun callsManques(u:String)=calls.filter{it.to==u&&it.duree==0}
    fun nbCalls(u:String)=historiqueCalls(u).size; fun dureeTotale(u:String)=historiqueCalls(u).sumOf{it.duree}; fun dernierCall(u:String)=historiqueCalls(u).firstOrNull()
    fun supprimerCall(id:String){ calls.removeIf{it.id==id}; Cache.db.collection("calls").document(id).delete() }; fun callEnCours(u:String)=calls.any{(it.from==u||it.to==u)&&it.duree==0}
}

object Products{
    fun creerProduit(n:String,p:Int,v:String){ val pr=Produit(Utils.genId(),n,p,v); Cache.produits.add(pr); Cache.db.collection("products").document(pr.id).set(pr) }
    fun listerMarket()=Cache.produits.reversed(); fun produitById(id:String)=Cache.produits.find{it.id==id}
    fun supprimerProduit(id:String){ Cache.produits.removeIf{it.id==id}; Cache.db.collection("products").document(id).delete() }
    fun produitsVendeur(v:String)=Cache.produits.filter{it.vendeur==v}
    fun acheterProduit(id:String,b:String):Boolean{ val pr=produitById(id)?:return false; val c=Profile.getCompte(b)?:return false; if(c.solde<pr.prix) return false; c.solde-=pr.prix; Profile.getCompte(pr.vendeur)?.let{it.solde+=pr.prix}; return true }
    fun chercherProduit(q:String)=Cache.produits.filter{it.nom.contains(q,true)}; fun trierPrixAsc()=Cache.produits.sortedBy{it.prix}; fun trierPrixDesc()=Cache.produits.sortedByDescending{it.prix}; fun nbProduits()=Cache.produits.size
}

object Cart{
    val paniers=mutableMapOf<String,MutableList<String>>()
    fun ajouterPanier(u:String,p:String){ paniers.getOrPut(u){mutableListOf()}.add(p) }; fun retirerPanier(u:String,p:String){ paniers[u]?.remove(p) }
    fun panierUser(u:String)=paniers[u]?:emptyList(); fun totalPanier(u:String)=panierUser(u).mapNotNull{Products.produitById(it)?.prix}.sum()
    fun clearPanier(u:String){ paniers[u]?.clear() }; fun nbPanier(u:String)=panierUser(u).size; fun estDansPanier(u:String,id:String)=panierUser(u).contains(id)
    fun checkout(u:String):Boolean{ val t=totalPanier(u); val c=Profile.getCompte(u)?:return false; if(c.solde<t) return false; c.solde-=t; clearPanier(u); return true }
    fun listerPanierDetail(u:String)=panierUser(u).mapNotNull{Products.produitById(it)}; fun prixMoyenPanier(u:String)=if(nbPanier(u)==0)0 else totalPanier(u)/nbPanier(u)
}

object Balance{
    val hist=mutableListOf<Triple<String,Int,Long>>()
    fun getSolde(p:String)=Profile.getCompte(p)?.solde?:0
    fun rechargerSolde(p:String,m:Int){ Profile.getCompte(p)?.let{it.solde+=m; hist.add(Triple(p,m,Utils.now())); Cache.db.collection("users").document(p).update("solde",it.solde)} }
    fun retirerSolde(p:String,m:Int):Boolean{ val c=Profile.getCompte(p)?:return false; if(c.solde<m) return false; c.solde-=m; hist.add(Triple(p,-m,Utils.now())); Cache.db.collection("users").document(p).update("solde",c.solde); return true }
    fun historiqueTransactions(p:String)=hist.filter{it.first==p}; fun totalRecharge(p:String)=historiqueTransactions(p).filter{it.second>0}.sumOf{it.second}; fun totalRetrait(p:String)=historiqueTransactions(p).filter{it.second<0}.sumOf{-it.second}
    fun soldeSuffisant(p:String,pr:Int)=getSolde(p)>=pr; fun transfert(f:String,t:String,m:Int):Boolean{ if(!retirerSolde(f,m)) return false; rechargerSolde(t,m); return true }
    fun topRiche()=Cache.comptes.maxByOrNull{it.solde}; fun pauvre(p:String)=getSolde(p)<500
}

object Badge{
    fun prixBadge()=2500; fun acheterBadgeBleu(p:String):Boolean{ val c=Profile.getCompte(p)?:return false; if(c.solde<prixBadge()) return false; c.solde-=prixBadge(); c.isBlue=true; Cache.db.collection("users").document(p).update("isBlue",true,"solde",c.solde); return true }
    fun estBlue(p:String)=Profile.getCompte(p)?.isBlue?:false; fun retirerBadge(p:String){ Profile.getCompte(p)?.isBlue=false; Cache.db.collection("users").document(p).update("isBlue",false) }
    fun listerBlue()=Cache.comptes.filter{it.isBlue}; fun nbBlue()=listerBlue().size; fun prixPromo()=1500
    fun offrirBadge(f:String,t:String):Boolean{ if(!Balance.retirerSolde(f,prixBadge())) return false; Profile.getCompte(t)?.isBlue=true; Cache.db.collection("users").document(t).update("isBlue",true); return true }
    fun verifierBadge(p:String)=estBlue(p); fun badgeGratuit(p:String){ Profile.getCompte(p)?.isBlue=true; Cache.db.collection("users").document(p).update("isBlue",true) }; fun revenusBadges()=nbBlue()*prixBadge()
}

object Notifs{
    val notifs=mutableListOf<Notif>()
    init{ Cache.db.collection("notifs").addSnapshotListener{ s,_ -> if(s!=null) notifs = s.toObjects(Notif::class.java).toMutableList() } }
    fun creerNotif(to:String,tx:String){ val n=Notif(Utils.genId(),to,tx,Utils.now()); notifs.add(n); Cache.db.collection("notifs").document(n.id).set(n) }
    fun listerNotifs(u:String)=notifs.filter{it.to==u}.reversed(); fun nbNonLues(u:String)=listerNotifs(u).count{!it.lue}
    fun marquerLue(id:String){ notifs.find{it.id==id}?.lue=true; Cache.db.collection("notifs").document(id).update("lue",true) }
    fun marquerToutesLues(u:String){ listerNotifs(u).forEach{Cache.db.collection("notifs").document(it.id).update("lue",true)} }
    fun supprimerNotif(id:String){ notifs.removeIf{it.id==id}; Cache.db.collection("notifs").document(id).delete() }
    fun clearNotifs(u:String){ notifs.filter{it.to==u}.forEach{Cache.db.collection("notifs").document(it.id).delete()}; notifs.removeIf{it.to==u} }
    fun notifLike(o:String,l:String){ creerNotif(o,"$l a liké") }; fun notifAmi(to:String,f:String){ creerNotif(to,"$f t'a ajouté") }; fun totalNotifs(u:String)=listerNotifs(u).size
}

object LiveM{
    val lives=mutableListOf<Live>()
    init{ Cache.db.collection("lives").addSnapshotListener{ s,_ -> if(s!=null) lives = s.toObjects(Live::class.java).toMutableList() } }
    fun creerLive(t:String,h:String){ val l=Live(Utils.genId(),t,h,mutableListOf(h),Utils.now()); lives.add(l); Cache.db.collection("lives").document(l.id).set(l) }
    fun listerLives()=lives.filter{Utils.now()-it.time<3600*1000}
    fun rejoindreLive(id:String,u:String){ lives.find{it.id==id}?.viewers?.add(u); Cache.db.collection("lives").document(id).update("viewers",lives.find{it.id==id}?.viewers) }
    fun quitterLive(id:String,u:String){ lives.find{it.id==id}?.viewers?.remove(u); Cache.db.collection("lives").document(id).update("viewers",lives.find{it.id==id}?.viewers) }
    fun nbViewers(id:String)=lives.find{it.id==id}?.viewers?.size?:0; fun terminerLive(id:String){ lives.removeIf{it.id==id}; Cache.db.collection("lives").document(id).delete() }
    fun livesHost(h:String)=listerLives().filter{it.host==h}; fun topLive()=listerLives().maxByOrNull{it.viewers.size}; fun estEnLive(h:String)=listerLives().any{it.host==h}; fun totalLives()=listerLives().size
}

object Events{
    val events=mutableListOf<Event>()
    init{ Cache.db.collection("events").addSnapshotListener{ s,_ -> if(s!=null) events = s.toObjects(Event::class.java).toMutableList() } }
    fun creerEvent(n:String,d:String,o:String){ val e=Event(Utils.genId(),n,d,o,mutableListOf(o)); events.add(e); Cache.db.collection("events").document(e.id).set(e) }
    fun listerEvents()=events.toList(); fun rejoindreEvent(id:String,u:String){ events.find{it.id==id}?.participants?.add(u); Cache.db.collection("events").document(id).update("participants",events.find{it.id==id}?.participants) }
    fun quitterEvent(id:String,u:String){ events.find{it.id==id}?.participants?.remove(u); Cache.db.collection("events").document(id).update("participants",events.find{it.id==id}?.participants) }
    fun eventById(id:String)=events.find{it.id==id}; fun nbParticipants(id:String)=eventById(id)?.participants?.size?:0; fun eventsUser(u:String)=events.filter{it.participants.contains(u)}
    fun supprimerEvent(id:String){ events.removeIf{it.id==id}; Cache.db.collection("events").document(id).delete() }; fun eventsFuturs()=events; fun topEvent()=events.maxByOrNull{it.participants.size}
}

object Search{
    fun searchAll(q:String)=Cache.comptes.filter{it.pseudo.contains(q,true)}+Cache.posts.filter{it.text.contains(q,true)}; fun searchUser(q:String)=Cache.comptes.filter{it.pseudo.contains(q,true)}
    fun searchPost(q:String)=Cache.posts.filter{it.text.contains(q,true)}; fun searchProduit(q:String)=Cache.pro
