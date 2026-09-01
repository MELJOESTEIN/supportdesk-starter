---
paths:
  - "backend/src/main/java/com/supportdesk/**/securite/**"
  - "backend/src/main/java/com/supportdesk/**/*Controller.java"
  - "backend/src/main/java/com/supportdesk/**/*Config.java"
---

# Règles de sécurité — chargées uniquement sur les fichiers concernés

1. **Passe en mode plan.** Aucune modification directe dans ces fichiers sans plan approuvé.

2. **Toute méthode qui renvoie une donnée appartenant à quelqu'un doit vérifier le
   propriétaire**, explicitement, dans le code. Pas dans un commentaire, pas dans le front.

3. **La référence client vient du jeton, jamais de la requête.** Aucun `@RequestParam`,
   `@PathVariable` ou champ de corps ne doit servir à décider *à quelles données* un
   utilisateur a accès. Il peut servir à décider *ce qu'il demande*, pas *ce qu'il a le droit
   d'obtenir*.

4. **Aucun secret en dur.** Ni mot de passe, ni clé, ni URL contenant des identifiants.

5. **CORS** : origines listées explicitement. Jamais `*` avec credentials.

6. Dans le plan, la section « impact sécurité » doit nommer **le fichier et la méthode** où la
   vérification se fait. « C'est géré par Spring Security » n'est pas une réponse.
