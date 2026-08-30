# Décision d'architecture — REST, GraphQL ou SOAP ?

> **À remplir par l'étudiant, J3, après avoir écrit les trois.** Une page, pas dix.
>
> L'exercice n'est pas de recopier un comparatif trouvé en ligne : c'est d'**argumenter sur
> son propre code**, celui qu'on vient d'écrire, avec les chiffres qu'on a mesurés.
>
> Format inspiré des *Architecture Decision Records* : on écrit la décision **et** ce qu'on
> a écarté, parce que dans six mois quelqu'un demandera « pourquoi pas GraphQL partout ? »
> et que personne ne s'en souviendra.

**Auteur :** ______________________  **Date :** ____________
**Statut :** ☐ proposée ☐ acceptée ☐ remplacée par ____________

---

## 1 · Le contexte

*Trois phrases. Quel problème se pose, à qui, et pourquoi une décision est nécessaire.*

## 2 · Les trois protocoles, sur CE code

Pour chacun : où il est utilisé dans SupportDesk, ce qu'il y apporte, ce qu'il y coûte.

### REST

| | |
|---|---|
| Où, dans ce projet | |
| Ce qu'il apporte ici | |
| Ce qu'il coûte ici | |
| Chiffre observé | |

### GraphQL

| | |
|---|---|
| Où, dans ce projet | |
| Ce qu'il apporte ici | |
| Ce qu'il coûte ici | |
| Chiffre observé | |

> Piste : compare le nombre d'appels réseau pour afficher le tableau de bord agent, avant
> et après. Et compte ce qu'il a fallu ajouter pour qu'il soit exploitable en production.

### SOAP

| | |
|---|---|
| Où, dans ce projet | |
| Ce qu'il apporte ici | |
| Ce qu'il coûte ici | |
| Chiffre observé | |

> Piste : SOAP n'a pas été *choisi* ici. Qu'est-ce que ça change à l'analyse ?

## 3 · La décision

*Ce qu'on retient, et pour quel périmètre. Sois précis : « GraphQL » ne veut rien dire,
« GraphQL pour le back-office agent, REST pour le portail client » est une décision.*

## 4 · Pourquoi pas l'inverse

*La section la plus utile, et celle qu'on saute toujours.*

**Pourquoi ne pas tout mettre en GraphQL ?**

**Pourquoi ne pas tout mettre en REST ?**

**Pourquoi ne pas exposer le CRM directement au front ?**

## 5 · Ce que cette décision coûte

*Toute décision a un prix. Lequel accepte-t-on, et jusqu'à quand ?*

-
-

## 6 · Ce qui la remettrait en cause

*Le signal qui, s'il apparaissait, obligerait à rouvrir le sujet.*

-

---

## Transposition — chez moi, lundi

> La partie qui justifie la journée. Cette décision-ci concerne SupportDesk ; celle qui
> compte concerne ton propre système d'information.

**Un endroit où j'ai, ou j'aurai, le même arbitrage :**

**Ce que je ferais différemment, et pourquoi :**
