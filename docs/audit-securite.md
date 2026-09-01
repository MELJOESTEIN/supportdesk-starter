# Audit de sécurité — API SupportDesk

> **À remplir par l'étudiant, J2.** Ce document est son livrable, pas celui de l'agent.
>
> L'agent peut exécuter les requêtes, montrer une sortie, expliquer un code de statut.
> **Il ne rédige pas les conclusions.** Un rapport écrit par l'outil qu'on audite ne vaut
> rien — et le jour où un client demandera « qui a signé cet audit ? », la réponse doit
> être un nom.
>
> Un corrigé existe. Ton formateur te le donnera **après** que tu aies rempli celui-ci.

**Auditeur :** ______________________  **Date :** ____________
**Périmètre :** ______________________________________________
**Version auditée :** commit ________________

---

## Méthode

Pour chaque risque : ce que j'ai **cherché**, ce que j'ai **fait**, ce que j'ai **obtenu**,
et ce que j'en **conclus**.

> Une conclusion sans manipulation n'est pas un constat, c'est une impression. Colle la
> sortie réelle — un `curl`, un code de statut, un extrait de corps. Trois lignes suffisent.

---

## API1:2023 — Autorisation au niveau objet (BOLA)

*Un endpoint prend un identifiant et renvoie la ressource, sans vérifier que l'appelant y a droit.*

**Ce que j'ai cherché :**

**Ce que j'ai fait :**

```

```

**Ce que j'ai obtenu :**

```

```

**Conclusion :** ☐ conforme ☐ défaut ☐ non vérifié

**Si défaut — où est la correction, et quel test la verrouille ?**

---

## API2:2023 — Authentification défaillante

*Jeton non vérifié, audience ignorée, expiration non contrôlée, secret devinable.*

**Ce que j'ai cherché :**

**Ce que j'ai fait :**

**Ce que j'ai obtenu :**

**Conclusion :** ☐ conforme ☐ défaut ☐ non vérifié

> Piste : un jeton émis par le même realm pour une **autre** application ouvre-t-il cette API ?

---

## API3:2023 — Autorisation au niveau des propriétés

*Fusion, en 2023, de « exposition excessive » et « affectation en masse ». Deux sens :
la réponse en dit trop ; la requête permet d'écrire trop.*

**En lecture — ce que la réponse contient de trop :**

**En écriture — ce que le DTO d'entrée accepte de trop :**

**Ce que j'ai obtenu :**

**Conclusion :** ☐ conforme ☐ défaut ☐ non vérifié

> Le test qui compte se fait sur le **corps de la réponse HTTP**, pas sur l'affichage. Une
> donnée masquée par le front est une donnée qui est arrivée dans le navigateur.

---

## API5:2023 — Autorisation au niveau fonction

*Un endpoint réservé, accessible sans le rôle. Souvent parce que la protection est un bouton masqué.*

**Ce que j'ai cherché :**

**Ce que j'ai fait :**

**Ce que j'ai obtenu :**

**Conclusion :** ☐ conforme ☐ défaut ☐ non vérifié

> Un `curl` ne rencontre jamais un guard Angular. Vérifie ce que l'API fait, pas ce que
> l'interface montre.

---

## API8:2023 — Mauvaise configuration

| Point | Vérifié par | Constat |
|---|---|---|
| CORS — origines listées, pas de `*` | | |
| Secrets — aucun en dur, aucun dans le dépôt | | |
| Actuator — quels endpoints répondent ? | | |
| Documentation d'API en production | | |
| En-têtes de réponse | | |
| Messages d'erreur — révèlent-ils la pile ? | | |
| Taille de page / profondeur de requête bornées | | |

**Conclusion :** ☐ conforme ☐ défaut ☐ non vérifié

---

## Synthèse

| Risque | Constat | Corrigé ? | Test de non-régression |
|---|---|---|---|
| API1 — objet | | | |
| API2 — authentification | | | |
| API3 — propriétés | | | |
| API5 — fonction | | | |
| API8 — configuration | | | |

## Ce que cet audit NE prouve pas

> La section qui distingue un rapport honnête d'un tampon. Qu'est-ce que tu n'as pas
> regardé ? Qu'est-ce qui pourrait avoir changé depuis ? Sur quoi ta conclusion repose-t-elle
> uniquement sur une lecture de code, sans manipulation ?

-
-

## Ce que j'ai appris

> Une ligne. Celle que tu voudras avoir sous les yeux dans six mois, devant du code
> qui n'est pas celui-ci.
