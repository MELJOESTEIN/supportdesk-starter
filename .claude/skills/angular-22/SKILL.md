---
name: angular-22
description: SQUELETTE À COMPLÉTER PAR L'ÉTUDIANT (J1). À charger pour toute écriture de code Angular sur ce projet.
---

# Angular 22 — écarts avec les versions précédentes

> **Ce fichier est volontairement incomplet.**
>
> C'est ton exercice du J1. Une compétence contient **ce que le modèle ignore ou se trompe**,
> pas une copie de la documentation. Recopier la doc gaspille du contexte et n'améliore rien.
>
> Méthode : demande à l'agent d'écrire un composant, repère ce qu'il produit d'une génération
> antérieure, et écris ici la correction. Un point par erreur réellement constatée.
>
> Modèle à imiter : `.claude/skills/spring-boot-4/SKILL.md`.

Ce projet utilise **Angular 22** (juin 2026) avec **TypeScript 6**.

## Ce que l'agent produit spontanément et qui est faux ici

| Il écrit | Il faut | Constaté le |
|---|---|---|
| `NgModule` | composants standalone | |
| `@Input()` / `@Output()` | `input()` / `output()` | |
| | | |
| | | |

## Points à documenter (à compléter)

- **Détection de changement** : Angular 22 est en `OnPush` par défaut. Conséquence sur le code ?
- **Zoneless** : `zone.js` n'est plus installé. Qu'est-ce que ça change concrètement ?
- **Signal Forms** : stables en v22, API différente de la précédente. Laquelle ?
- **`resource()` / `httpResource()`** : quand les préférer à un service + `BehaviorSubject` ?
- **`HttpClient` sur Fetch** : conséquence sur les interceptors ou les erreurs ?
- **TypeScript 6** : quelles erreurs de compilation nouvelles as-tu rencontrées ?

## Le serveur MCP Angular

Le CLI expose `ng mcp`. Branché sur l'agent, il donne le contexte réel du workspace au lieu de
ses souvenirs d'entraînement. **À utiliser pour tout travail front.**

Note ici la différence observée avec et sans :
