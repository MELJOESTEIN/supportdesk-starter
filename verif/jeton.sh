#!/usr/bin/env bash
#
# Obtient un jeton d'accès pour un utilisateur de test, et l'affiche.
#
#   ./jeton.sh alice            -> le jeton brut, à coller dans un curl
#   ./jeton.sh alice --claims   -> les claims décodés, lisibles
#   eval $(./jeton.sh alice --export)   -> exporte $TOKEN dans le shell courant
#
# Utilisateurs : alice (CLIENT/CLI-0001), david (CLIENT/CLI-0002), bob (AGENT),
#                carol (ADMIN+AGENT). Mot de passe : password.
#
# Le flux « mot de passe » est activé sur le client public POUR LES TESTS. Le navigateur,
# lui, utilise « code + PKCE » : voir ./pkce.sh.
set -euo pipefail

UTILISATEUR="${1:-alice}"
MODE="${2:-}"
KEYCLOAK="${KEYCLOAK:-http://localhost:8081}"

REPONSE=$(curl -s -m 20 -X POST \
  "$KEYCLOAK/realms/supportdesk/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=supportdesk-front \
  -d "username=$UTILISATEUR" -d password=password)

JETON=$(printf '%s' "$REPONSE" | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null || true)

if [ -z "$JETON" ]; then
  echo "Échec : Keycloak n'a pas délivré de jeton pour « $UTILISATEUR »." >&2
  echo "$REPONSE" >&2
  echo >&2
  echo "Vérifie que Keycloak répond :  curl -s $KEYCLOAK/realms/supportdesk | head -c 80" >&2
  exit 1
fi

case "$MODE" in
  --claims)
    python3 - "$JETON" <<'PY'
import sys, json, base64
charge = sys.argv[1].split('.')[1]
charge += '=' * (-len(charge) % 4)
claims = json.loads(base64.urlsafe_b64decode(charge))
interessants = ['preferred_username', 'crm_client_ref', 'realm_access', 'aud', 'iss', 'exp']
for cle in interessants:
    if cle in claims:
        valeur = claims[cle]
        if cle == 'realm_access':
            valeur = [r for r in valeur.get('roles', []) if r in ('CLIENT', 'AGENT', 'ADMIN')]
        print(f"  {cle:20} : {valeur}")
PY
    ;;
  --export) echo "export TOKEN=$JETON" ;;
  *)        echo "$JETON" ;;
esac
