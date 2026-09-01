# Page statique servie par nginx. Aucune étape de construction : c'est du HTML, du CSS et du
# JavaScript que le navigateur exécute tel quel.
#
# C'est délibéré. Une seconde application Angular aurait laissé croire que le SSO dépend du
# framework. Cent lignes de JavaScript nu participent au même SSO qu'un projet Angular
# complet — parce que le SSO est une propriété du portail d'identité, pas du client.
FROM nginx:1.29-alpine

COPY public/ /usr/share/nginx/html/

# Une page statique n'a rien à mettre en cache pendant une démonstration : on veut voir la
# modification tout de suite, pas la version d'il y a dix minutes.
RUN printf 'add_header Cache-Control "no-store";\n' > /etc/nginx/conf.d/no-cache.conf

EXPOSE 80
