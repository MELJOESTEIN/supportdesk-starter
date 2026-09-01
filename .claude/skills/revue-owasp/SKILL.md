---
name: revue-owasp
description: Audit de sécurité d'une API en cinq risques. À charger avant toute revue de sécurité ou avant d'écrire un endpoint qui expose des données appartenant à un utilisateur.
---

# Revue OWASP API — cinq risques

Les défauts de sécurité que produit un agent sont, très majoritairement, ceux que l'OWASP
recense en tête de son classement API. Cette compétence les liste dans l'ordre où ils
apparaissent réellement dans du code généré.

## 1. Autorisation au niveau objet cassée (BOLA)

**Le plus fréquent, et de loin.** Un endpoint prend un identifiant et renvoie la ressource
correspondante — sans vérifier que l'appelant a le droit de la voir.

Test mental : *si je change l'identifiant dans l'URL par celui de quelqu'un d'autre, que se
passe-t-il ?* Si la réponse n'est pas « 403 ou 404 », c'est une faille.

Sur ce projet : `alice` (CLI-0001) ne doit jamais lire un ticket de `david` (CLI-0002).

## 2. Autorisation au niveau fonction cassée

Un endpoint réservé aux agents ou aux administrateurs, accessible sans le rôle. Souvent parce
que la protection existe côté front — un bouton masqué, une route protégée par un guard —
et nulle part côté serveur.

**Un guard Angular est de l'UX. Il ne protège rien.** Vérifie toujours côté backend.

## 3. Exposition excessive de données

L'entité est renvoyée telle quelle, avec des champs que l'appelant ne devrait pas voir :
commentaires internes, données d'un autre client, champs techniques. Le front n'affiche que
ce qu'il veut, mais la donnée est bien partie sur le réseau.

Règle du projet : jamais d'entité JPA dans un contrôleur, toujours un DTO ou une projection.

## 4. Affectation en masse (mass assignment)

Un DTO d'entrée accepte des champs que le client ne devrait pas pouvoir fixer : un statut, un
propriétaire, un rôle. L'utilisateur crée un ticket et se l'attribue lui-même comme agent.

Vérifie ce que le DTO d'entrée accepte, pas seulement ce que le contrôleur en fait.

## 5. Configuration de sécurité défaillante

CORS en `*`, secret en dur, en-têtes de sécurité absents, endpoint Actuator ouvert,
aucune limitation de débit sur l'authentification, messages d'erreur qui révèlent la stack.

## Comment mener la revue

Pour chaque endpoint modifié, réponds aux cinq questions dans l'ordre. Ne conclus pas « RAS »
sans avoir dit ce que tu as vérifié.

L'outillage (scan OWASP ZAP) complète cette revue, il ne la remplace pas : un scanner ne
connaît pas les règles métier, donc il ne détecte pas une BOLA.
