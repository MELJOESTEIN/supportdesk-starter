# syntax=docker/dockerfile:1

# --- Construction ----------------------------------------------------------
# Node 24.20 : le CLI Angular 22 exige ^22.22.3 || ^24.15.0 || >=26.0.0 et refuse
# de démarrer en dessous. « 24 LTS » ne suffit pas comme spécification.
FROM node:24.20-alpine AS build
WORKDIR /src

COPY package.json package-lock.json ./
# npm ci, pas npm install : le lock fait foi, et le build est reproductible.
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .

# tokens.css est un lien symbolique vers ../../design/project/tokens.css — hors du contexte
# de build. Le lien est bien copié, mais sa cible n'existe pas dans l'image, et le build
# échoue sur « Could not resolve ./styles/tokens.css ».
#
# Le contexte additionnel « design » apporte le fichier réel. Hors Compose :
#     docker build --build-context design=../design -t sd-frontend ./frontend
COPY --from=design project/tokens.css src/styles/tokens.css

RUN npm run build

# --- Exécution -------------------------------------------------------------
FROM nginx:1.30-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
