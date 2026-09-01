#!/usr/bin/env node
/**
 * Garde-fou du système de design.
 *
 * `design/project/tokens.css` est la source de vérité visuelle. Ce script échoue dès qu'une
 * couleur littérale réapparaît dans un style de composant, ou si le vocabulaire « note
 * interne » déborde des deux composants qui ont le droit de l'employer.
 *
 * Ce n'est pas un test unitaire : il lit le disque. Il tourne en CI, avec le lint.
 */
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const RACINE = join(import.meta.dirname, '..', 'src', 'app');
const AUTORISES_INTERNE = ['composeur.scss', 'fil-commentaires.scss'];

function fichiersScss(dossier) {
  return readdirSync(dossier).flatMap((entree) => {
    const chemin = join(dossier, entree);
    return statSync(chemin).isDirectory()
      ? fichiersScss(chemin)
      : chemin.endsWith('.scss')
        ? [chemin]
        : [];
  });
}

const fichiers = fichiersScss(RACINE);
const erreurs = [];

for (const chemin of fichiers) {
  const contenu = readFileSync(chemin, 'utf8');

  const couleurs = [...contenu.matchAll(/#[0-9a-fA-F]{3,8}\b/g)].map((m) => m[0]);
  if (couleurs.length) {
    erreurs.push(`${chemin} : couleur littérale ${couleurs.join(', ')} — utiliser un jeton`);
  }

  const nom = chemin.split('/').at(-1);
  if (/--sd-internal-/.test(contenu) && !AUTORISES_INTERNE.includes(nom)) {
    erreurs.push(
      `${chemin} : le vocabulaire « note interne » ne doit apparaître que dans ${AUTORISES_INTERNE.join(' et ')}`,
    );
  }
}

if (erreurs.length) {
  console.error('Vérification des jetons : ÉCHEC\n');
  for (const erreur of erreurs) {
    console.error('  - ' + erreur);
  }
  process.exit(1);
}

console.log(`Vérification des jetons : ${fichiers.length} fichiers SCSS, aucune couleur littérale,`);
console.log(`vocabulaire « note interne » limité à ${AUTORISES_INTERNE.join(' et ')}.`);
