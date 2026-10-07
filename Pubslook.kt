package com.taillebook

object Pubslook {

    data class VideoTikTok(
        val id:String="",
        val titre:String="",
        val owner:String="",
        val url:String="",
        val image:String="",
        val sound:String="",
        val hashtags:List<String> = emptyList(),
        val challenge:String="",
        val prix:Int=200,
        val vues:MutableList<String> = mutableListOf(),
        val likes:MutableList<String> = mutableListOf(),
        val wouah:MutableList<String> = mutableListOf(),
        val haha:MutableList<String> = mutableListOf(),
        val triste:MutableList<String> = mutableListOf(),
        val colere:MutableList<String> = mutableListOf(),
        val saves:MutableList<String> = mutableListOf(),
        val partages:MutableList<String> = mutableListOf(),
        val comments:MutableList<CommentTikTok> = mutableListOf(),
        val duets:MutableList<String> = mutableListOf(),
        val gifts:MutableList<Gift> = mutableListOf(),
        val isLive:Boolean=false,
        val time:Long=0
    )

    data class CommentTikTok(
        val id:String="",
        val videoId:String="",
        var text:String="",
        val user:String="",
        val time:Long=0,
        val likes:MutableList<String> = mutableListOf(),
        val replies:MutableList<CommentTikTok> = mutableListOf()
    )

    data class Gift(val from:String="", val type:String="", val value:Int=0, val time:Long=0)
    data class Sound(val id:String="", val nom:String="", val owner:String="", val uses:Int=0)
    data class Hashtag(val tag:String="", var vues:Int=0, var videos:MutableList<String> = mutableListOf())

    var videos = mutableListOf<VideoTikTok>()
    var fyp = mutableListOf<VideoTikTok>()
    var sounds = mutableListOf<Sound>()
    var hashtags = mutableListOf<Hashtag>()
    var currentIndex = 0

    init {
        Cache.db.collection("videos").addSnapshotListener{ s,_ ->
            if(s!=null &&!s.isEmpty){
                videos = s.toObjects(VideoTikTok::class.java).toMutableList()
                refreshFYP()
            }
        }
    }

    // ===== CREATION =====
    fun creerVideo(titre:String, owner:String, url:String, sound:String="", tags:List<String> = emptyList(), prix:Int=200):Boolean{
        if(!Balance.retirerSolde(owner,prix)) return false
        val v = VideoTikTok(Utils.genId(), titre, owner, url,"", sound, tags,"", prix)
        videos.add(0,v)
        Cache.db.collection("videos").document(v.id).set(v)
        tags.forEach{ addHashtag(it, v.id) }
        if(sound.isNotBlank()) addSound(sound, owner)
        refreshFYP()
        Balance.rechargerSolde("Admin", prix)
        return true
    }

    fun supprimer(id:String){
        videos.removeIf{it.id==id}
        Cache.db.collection("videos").document(id).delete()
        refreshFYP()
    }

    fun editer(id:String, nt:String){
        videos.find{it.id==id}?.let{
            videos[videos.indexOf(it)] = it.copy(titre=nt)
            Cache.db.collection("videos").document(id).update("titre", nt)
        }
    }

    fun videoById(id:String) = videos.find{it.id==id}

    // ===== FEED MONDIAL =====
    fun refreshFYP(){
        fyp.clear()
        fyp.addAll(videos.sortedByDescending{it.vues.size*3+it.likes.size*5+it.comments.size*4+it.partages.size*6}.take(100))
    }

    fun forYou():List<VideoTikTok>{
        if(fyp.isEmpty()) refreshFYP()
        return fyp
    }

    fun following(user:String):List<VideoTikTok>{
        val amis = Friends.amis[user]?: emptyList()
        return videos.filter{amis.contains(it.owner)}.reversed()
    }

    fun next():VideoTikTok?{
        if(fyp.isEmpty()) return null
        currentIndex = (currentIndex+1)%fyp.size
        return fyp[currentIndex]
    }

    fun prev():VideoTikTok?{
        if(fyp.isEmpty()) return null
        currentIndex = if(currentIndex-1<0) fyp.size-1 else currentIndex-1
        return fyp[currentIndex]
    }

    fun random() = videos.randomOrNull()

    // ===== VUES & ENGAGEMENT =====
    fun voir(id:String, u:String){
        val v = videoById(id)?: return
        if(!v.vues.contains(u)){
            v.vues.add(u)
            Cache.db.collection("videos").document(id).update("vues", v.vues)
            Balance.rechargerSolde(u,5)
            Balance.rechargerSolde(v.owner,15)
        }
    }

    fun voirComplete(id:String, u:String){ voir(id,u); Balance.rechargerSolde(videoById(id)?.owner?:"",5) }

    fun like(id:String, u:String){
        videoById(id)?.let{
            if(!it.likes.contains(u)){
                it.likes.add(u)
                Cache.db.collection("videos").document(id).update("likes", it.likes)
                Balance.rechargerSolde(it.owner,3)
            }
        }
    }

    fun unlike(id:String, u:String){
        videoById(id)?.likes?.remove(u)
        Cache.db.collection("videos").document(id).update("likes", videoById(id)?.likes)
    }

    fun wouah(id:String, u:String){ videoById(id)?.wouah?.add(u); Cache.db.collection("videos").document(id).update("wouah",videoById(id)?.wouah) }
    fun haha(id:String, u:String){ videoById(id)?.haha?.add(u); Cache.db.collection("videos").document(id).update("haha",videoById(id)?.haha) }
    fun triste(id:String, u:String){ videoById(id)?.triste?.add(u); Cache.db.collection("videos").document(id).update("triste",videoById(id)?.triste) }
    fun colere(id:String, u:String){ videoById(id)?.colere?.add(u); Cache.db.collection("videos").document(id).update("colere",videoById(id)?.colere) }

    fun aLike(id:String, u:String) = videoById(id)?.likes?.contains(u)?:false

    // ===== COMMENTS =====
    fun commenter(id:String, u:String, tx:String){
        videoById(id)?.let{
            it.comments.add(CommentTikTok(Utils.genId(), id, tx, u, Utils.now()))
            Cache.db.collection("videos").document(id).update("comments", it.comments)
            Balance.rechargerSolde(it.owner,15)
        }
    }

    fun repondreComment(vId:String, cId:String, u:String, tx:String){
        videoById(vId)?.comments?.find{it.id==cId}?.replies?.add(CommentTikTok(Utils.genId(), vId, tx, u, Utils.now()))
        Cache.db.collection("videos").document(vId).update("comments", videoById(vId)?.comments)
    }

    fun likerComment(vId:String, cId:String, u:String){
        videoById(vId)?.comments?.find{it.id==cId}?.likes?.add(u)
        Cache.db.collection("videos").document(vId).update("comments", videoById(vId)?.comments)
    }

    fun supprimerComment(vId:String, cId:String){
        videoById(vId)?.comments?.removeIf{it.id==cId}
        Cache.db.collection("videos").document(vId).update("comments", videoById(vId)?.comments)
    }

    fun listerComments(id:String) = videoById(id)?.comments?: emptyList()

    // ===== PARTAGE / SAVE =====
    fun partager(id:String, u:String){
        videoById(id)?.let{
            if(!it.partages.contains(u)){
                it.partages.add(u)
                Cache.db.collection("videos").document(id).update("partages", it.partages)
                Balance.rechargerSolde(u,10)
                Balance.rechargerSolde(it.owner,20)
            }
        }
    }

    fun save(id:String, u:String){ videoById(id)?.saves?.add(u); Cache.db.collection("videos").document(id).update("saves",videoById(id)?.saves) }
    fun unsave(id:String, u:String){ videoById(id)?.saves?.remove(u); Cache.db.collection("videos").document(id).update("saves",videoById(id)?.saves) }

    // ===== DUET / LIVE =====
    fun duet(origId:String, url:String, owner:String):Boolean{
        val o = videoById(origId)?: return false
        if(!creerVideo("Duet avec @${o.owner}", owner, url, o.sound, o.hashtags,100)) return false
        o.duets.add(videos[0].id)
        Cache.db.collection("videos").document(origId).update("duets", o.duets)
        return true
    }

    fun stitch(origId:String, url:String, owner:String) = duet(origId, url, owner)

    fun goLive(owner:String, titre:String){
        val v = VideoTikTok(Utils.genId(), titre, owner, isLive=true)
        videos.add(0,v)
        Cache.db.collection("videos").document(v.id).set(v)
    }

    fun endLive(id:String){ supprimer(id) }

    // ===== GIFTS =====
    fun envoyerGift(vId:String, from:String, type:String, value:Int){
        videoById(vId)?.gifts?.add(Gift(from,type,value, Utils.now()))
        Cache.db.collection("videos").document(vId).update("gifts", videoById(vId)?.gifts)
        Balance.retirerSolde(from,value)
        Balance.rechargerSolde(videoById(vId)?.owner?:"", value*80/100)
        Balance.rechargerSolde("Admin", value*20/100)
    }

    fun totalGifts(id:String) = videoById(id)?.gifts?.sumOf{it.value}?:0

    // ===== SOUNDS & HASHTAGS =====
    fun addSound(nom:String, owner:String){
        val s = sounds.find{it.nom==nom}
        if(s==null) sounds.add(Sound(Utils.genId(),nom,owner,1)) else sounds[sounds.indexOf(s)] = s.copy(uses=s.uses+1)
        Cache.db.collection("sounds").document(nom).set(Sound(Utils.genId(),nom,owner,1))
    }

    fun topSounds() = sounds.sortedByDescending{it.uses}.take(10)
    fun videosAvecSound(sound:String) = videos.filter{it.sound==sound}

    fun addHashtag(tag:String, vId:String){
        val h = hashtags.find{it.tag==tag}
        if(h==null) hashtags.add(Hashtag(tag,1, mutableListOf(vId))) else {h.vues++; h.videos.add(vId)}
        Cache.db.collection("hashtags").document(tag).set(Hashtag(tag, hashtags.find{it.tag==tag}?.vues?:1, hashtags.find{it.tag==tag}?.videos?:mutableListOf(vId)))
    }

    fun topHashtags() = hashtags.sortedByDescending{it.vues}.take(10)
    fun videosAvecHashtag(tag:String) = hashtags.find{it.tag==tag}?.videos?.mapNotNull{videoById(it)}?: emptyList()
    fun creerChallenge(nom:String, owner:String){
        hashtags.add(Hashtag("#$nom",0, mutableListOf()))
        Cache.db.collection("hashtags").document("#$nom").set(Hashtag("#$nom",0, mutableListOf()))
    }

    // ===== STATS =====
    fun cVues(id:String) = videoById(id)?.vues?.size?:0
    fun cLikes(id:String) = videoById(id)?.likes?.size?:0
    fun cComs(id:String) = videoById(id)?.comments?.size?:0
    fun cParts(id:String) = videoById(id)?.partages?.size?:0
    fun cSaves(id:String) = videoById(id)?.saves?.size?:0
    fun engagement(id:String) = cVues(id)+cLikes(id)*3+cComs(id)*4
    fun isViral(id:String) = cVues(id)>100 && cLikes(id)>20
    fun topViral() = videos.maxByOrNull{engagement(it.id)}

    fun videosUser(o:String) = videos.filter{it.owner==o}
    fun revenus(id:String) = cVues(id)*10+cLikes(id)*2+totalGifts(id)
    fun revenusUser(o:String) = videosUser(o).sumOf{revenus(it.id)}
    fun revenusTotal() = videos.sumOf{revenus(it.id)}
    fun search(q:String) = videos.filter{it.titre.contains(q,true)||it.hashtags.any{tag->tag.contains(q,true)}}.take(20)

    // ===== AJOUT MONDIAL - TRADUCTION TITRE =====
    fun traduireVideoTitre(id:String, langueCible:String, onResult:(String)->Unit){
        val v = videoById(id)?: return
        TranslationCenter.traduireTexte(v.titre, langueCible){ traduit ->
            onResult(traduit)
        }
    }
}a
