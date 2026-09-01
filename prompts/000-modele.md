# NNN · <nom de la fonctionnalité>

> Modèle à copier en `prompts/NNN-nom-fonctionnalite.md`, numéroté dans l'ordre.
> Rempli par l'agent **avant** d'écrire du code. Approuvé explicitement avant l'implémentation.
>
> **Rappel des six refus automatiques.** Ce plan est refusé si : aucune hypothèse n'est listée ·
> le diff annoncé dépasse 200 lignes · la section « impact sécurité » est vide · un endpoint
> renvoie une donnée d'autrui sans vérification du propriétaire · un test n'a pas d'assertion
> réelle · la conclusion dit « c'est fait » sans sortie de commande.

---

## 1 · Contexte

*La demande, en une phrase.*

## 2 · Fichiers inspectés

*Ce que j'ai réellement lu avant de proposer. Chemins précis.*

- 
- 

## 3 · Hypothèses

> **Section à relire en premier. C'est là que se trouvent les erreurs.**
> Un plan sans hypothèse est un plan qui n'a pas regardé. Minimum trois.

- 
- 
- 

## 4 · Modèle et migration

*Le schéma visé. La migration Flyway prévue, avec son numéro de version.*

## 5 · Fichiers créés ou modifiés

*La liste, avant. Avec une estimation du nombre de lignes.*

| Fichier | Créé / modifié | ~lignes |
|---|---|---|
| | | |

**Total estimé :** ___ lignes. *(Au-delà de 200, découper.)*

## 6 · Impact sécurité

> *Section propre à ce projet, ajoutée parce que c'est exactement ce qu'un agent oublie.*

- **Qui a le droit d'appeler ça ?** 
- **Où la vérification se fait-elle ?** *(fichier + méthode)*
- **La donnée renvoyée appartient-elle à quelqu'un ?** Si oui, où est vérifié le propriétaire ?
- **Une valeur envoyée par le client sert-elle à décider d'un accès ?** *(si oui, c'est un défaut)*

## 7 · Critères d'acceptation

*Observables. Pas « ça marche » — un appel, une sortie attendue.*

- [ ] 
- [ ] 

## 8 · Comment on teste

| Test | Niveau | Ce qu'il doit échouer à faire |
|---|---|---|
| | | |

*La troisième colonne est la plus importante : un test qui ne peut pas échouer ne teste rien.*

---

## Approbation

- [ ] Plan lu en entier, section « hypothèses » en premier
- [ ] Approuvé le ____________ par ____________
