// Dans Post.java - on ajoute juste 2 lignes
public class Post {
   String videoUrl;
   boolean isBoosted = false; // NOUVEAU
   long boostEndTime; // NOUVEAU - jusqu'à quand il reste en haut
}

// Dans MainActivity.java - on change 3 lignes
// Avant: tu affichais les posts par date
// Maintenant: tu affiches d'abord ceux qui sont boostés!
query = Firebase.orderBy("isBoosted", DESC).orderBy("date");
