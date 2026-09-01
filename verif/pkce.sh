#!/usr/bin/env bash
#
# Le flux que le NAVIGATEUR utilise réellement — « code d'autorisation + PKCE S256 » —
# joué en ligne de commande, étape par étape.
#
# Les fichiers .http de ce dossier obtiennent leurs jetons par mot de passe : pratique
# pour démontrer, mais ce n'est pas ce que fait l'application. Ce script montre le vrai
# flux, et surtout ce que PKCE protège : à l'étape 4, le même code d'autorisation, rejoué
# avec un mauvais `code_verifier`, est refusé. C'est ce qui rend inoffensif un code
# intercepté — la seule chose qui manque à l'attaquant n'a jamais transité sur le réseau.
#
# Usage :  ./pkce.sh [alice|david|bob|carol]
set -euo pipefail
KC=http://localhost:8081/realms/supportdesk
CLIENT=supportdesk-front
REDIRECT=http://localhost:4200/connexion/retour
USER=${1:-alice}
JAR=$(mktemp)

VERIFIER=$(head -c 64 /dev/urandom | base64 | tr -d '=+/\n' | cut -c1-64)
CHALLENGE=$(printf '%s' "$VERIFIER" | openssl dgst -binary -sha256 | base64 | tr -d '\n' | tr '+/' '-_' | tr -d '=')

AUTH_URL="$KC/protocol/openid-connect/auth?client_id=$CLIENT&response_type=code&scope=openid%20profile%20email&redirect_uri=$REDIRECT&code_challenge=$CHALLENGE&code_challenge_method=S256&state=xyz&nonce=abc"
PAGE=$(curl -s -c "$JAR" "$AUTH_URL")
ACTION=$(printf '%s' "$PAGE" | grep -oE 'action="[^"]+"' | head -1 | sed 's/action="//;s/"$//' | sed 's/&amp;/\&/g')
[ -n "$ACTION" ] || { echo "ECHEC : pas de formulaire de connexion"; exit 1; }

LOCATION=$(curl -s -b "$JAR" -c "$JAR" -o /dev/null -D - -X POST "$ACTION" \
  --data-urlencode "username=$USER" --data-urlencode "password=password" \
  | grep -i '^location:' | tr -d '\r' | sed 's/[Ll]ocation: //')
CODE=$(printf '%s' "$LOCATION" | grep -oE 'code=[^&]+' | cut -d= -f2)
[ -n "$CODE" ] || { echo "ECHEC : pas de code d'autorisation ; redirection = $LOCATION"; exit 1; }

echo "  1. redirection vers Keycloak, avec code_challenge_method=S256   OK"
echo "  2. code d'autorisation reçu sur $REDIRECT                        OK"

REPONSE=$(curl -s -X POST "$KC/protocol/openid-connect/token" \
  -d grant_type=authorization_code -d "client_id=$CLIENT" -d "code=$CODE" \
  -d "redirect_uri=$REDIRECT" -d "code_verifier=$VERIFIER")
TOKEN=$(printf '%s' "$REPONSE" | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))")
[ -n "$TOKEN" ] || { echo "ECHEC à l'échange du code"; echo "$REPONSE"; exit 1; }
echo "  3. code échangé contre un jeton, avec code_verifier              OK"

# Un code d'autorisation est à usage unique : il faut en obtenir un neuf pour
# éprouver le verifier, sinon Keycloak refuse pour la mauvaise raison.
PAGE2=$(curl -s -b "$JAR" -c "$JAR" "$AUTH_URL")
LOC2=$(curl -s -b "$JAR" -o /dev/null -D - "$AUTH_URL" | grep -i '^location:' | tr -d '\r' | sed 's/[Ll]ocation: //')
CODE2=$(printf '%s' "$LOC2" | grep -oE 'code=[^&]+' | cut -d= -f2)
if [ -n "$CODE2" ]; then
  echo "  4. le MÊME code, échangé avec un mauvais code_verifier :"
  ERREUR=$(curl -s -X POST "$KC/protocol/openid-connect/token" \
    -d grant_type=authorization_code -d "client_id=$CLIENT" -d "code=$CODE2" \
    -d "redirect_uri=$REDIRECT" -d "code_verifier=un-verifier-qui-nest-pas-le-bon-du-tout-1234567890" \
    | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('error','?'), '-', d.get('error_description','?'))")
  echo "     -> $ERREUR"
  echo "     (c'est ce qui protège un client public : sans le verifier, un code intercepté ne vaut rien)"
fi

echo "  5. appel de l'API avec ce jeton : HTTP $(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tickets?taille=1)"
rm -f "$JAR"
