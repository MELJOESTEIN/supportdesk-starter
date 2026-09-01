# SupportDesk — modèle produit

> **À remplir par l'étudiant, J1, avant toute ligne de code.** Une page, pas dix.
>
> L'agent **critique** ce document une fois rempli. Il ne le rédige pas. Si le modèle de domaine
> sort de l'agent, c'est le framework qui choisit le schéma — et les règles d'accès n'existent
> nulle part.
>
> Ce document sera rouvert au J2, au moment de la faille BOLA. Ce n'est pas un hasard.

---

## 1 · Qui fait quoi

*Qui déclare un ticket, qui l'instruit, qui le clôture ?*

| Rôle | Peut créer | Peut lire | Peut modifier | Peut clôturer |
|---|---|---|---|---|
| CLIENT | | | | |
| AGENT | | | | |
| ADMIN | | | | |

## 2 · Ce que chacun a le droit de voir

*Quelles données un client peut-il voir, et lesquelles lui sont interdites ?*

Sois précis. « Ses tickets » ne suffit pas : un commentaire interne posé par un agent sur le
ticket d'un client est-il visible par ce client ?

- Un CLIENT voit : 
- Un CLIENT ne voit jamais : 
- Un AGENT voit : 

## 3 · Cycle de vie d'un ticket

*Quelles transitions sont légales, et lesquelles ne le sont pas ?*

```
OUVERT → ?
EN_COURS → ?
RESOLU → ?
FERME → ?
```

Transitions **interdites** — et qui les empêche :

- 

## 4 · Ce qui ne doit jamais dépendre du client

> **La question qui rapporte le plus de toute la formation.**

*Quelles opérations ne doivent jamais se décider à partir d'une donnée envoyée par le client ?*

Exemple du type de réponse attendue : « la référence client utilisée pour filtrer les tickets
vient du jeton, jamais d'un paramètre de requête ».

- 
- 

*Relis cette section au J2. La faille que tu vas exploiter en est la violation littérale.*

## 5 · Les écrans, et ce que chacun affiche

| Écran | Accessible à | Affiche | N'affiche jamais |
|---|---|---|---|
| Liste de mes tickets | | | |
| Détail d'un ticket | | | |
| Tableau de bord agent | | | |

---

## Critique de l'agent

*Coller ici ce que l'agent a relevé sur ce document, et ce que tu as décidé d'en faire.*

| Remarque de l'agent | Retenue ? | Pourquoi |
|---|---|---|
| | | |
