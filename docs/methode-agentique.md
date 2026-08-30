# La méthode agentique

> **Version alignée sur le projet SupportDesk.** Document d'origine conservé dans son intégralité ;
> seuls les noms du fil rouge et les numéros de jour ont été corrigés pour rester cohérents avec
> `CLAUDE.md`. La mise en œuvre concrète, jour par jour, est conduite en séance.


Document de référence du module M0. À garder ouvert pendant les quatre jours.

---

## Objectif de la méthode

> **Rendre reproductible et vérifiable un développement assisté par IA, en sortant
> l'état du fil de conversation pour le placer dans des fichiers versionnés — afin
> que la vitesse gagnée sur la production ne se paie ni en dette technique, ni en
> perte de maîtrise.**

Trois choses en découlent, et ce sont les trois raisons d'adopter la méthode :

**Reproductibilité.** Un autre développeur — ou vous dans trois mois — doit pouvoir
rejouer le raisonnement, pas seulement lire le résultat. Une conversation avec un
agent ne se relit pas : elle est longue, non structurée, et elle disparaît.

**Vérifiabilité.** Chaque étape produit un artefact qu'on peut relire *avant* que le
code n'existe. C'est ce qui permet d'arrêter une mauvaise direction en trente
secondes plutôt qu'en trois heures.

**Responsabilité.** La décision reste identifiable et humaine. « L'agent l'a écrit »
n'est pas une explication recevable en revue de code, et ne le sera jamais.

### La règle qui résume tout

**L'état vit dans des fichiers, pas dans la conversation.**

C'est le seul principe à retenir si vous n'en retenez qu'un. Chaque étape de la
méthode consiste à externaliser une pièce d'état qui, autrement, resterait dans une
fenêtre de chat — et serait perdue à la fermeture.

| Étape | Artefact produit | Où il vit | Ce qu'il évite |
|---|---|---|---|
| 1 · Produit d'abord | Modèle de domaine, règles d'accès | `docs/produit.md` | Un schéma de données choisi par le framework |
| 2 · Compétences | Savoir-faire outil par outil | `.claude/skills/` | Du code d'une version antérieure du framework |
| 3 · Contexte projet | Rôle, règles, conventions | `CLAUDE.md`, `.claude/rules/` | Réexpliquer les mêmes règles chaque session |
| 4 · Plan avant code | Prompt d'implémentation | `prompts/NNN-*.md` | Trois heures dans la mauvaise direction |
| 5 · Vérification | Tests, scan de sécurité | `src/test/`, CI | Un « c'est fait » qui ne compile pas |
| 6 · Git et revue | Branche, PR, revue à froid | Historique Git | Un défaut invisible à celui qui l'a écrit |
| 7 · Mesure | Métriques d'usage | Hors périmètre ici | Optimiser ce que personne n'utilise |
| 8 · Workflow outillé | Le processus lui-même | `.claude/skills/` | Un processus qui ne tient que dans votre tête |

---

## 1 · L'architecture centrée produit

**Le principe.** Répondre aux questions produit avant de choisir la pile technique :
ce que l'application doit faire, qui crée et qui consomme, où vivent les données,
ce qui exige une authentification, ce qui doit rester côté serveur.

**Dans cette formation.** La pile est imposée — c'est le sujet du cours. Le principe
survit sous une autre forme, plus exigeante : **le modèle de domaine avant toute
ligne de code**, et surtout avant toute entité JPA. L'étudiant modélise, l'agent
critique le modèle. Jamais l'inverse.

Les questions à trancher sur le projet SupportDesk :

- Qui déclare un ticket, qui l'instruit, qui le clôture ?
- Quelles données un client peut-il voir, et lesquelles lui sont interdites ?
- Quel est le cycle de vie d'un dossier, et quelles transitions sont illégales ?
- Quelles opérations ne doivent jamais dépendre d'une donnée envoyée par le client ?
- Quels écrans existent, et lequel de ces écrans a le droit d'afficher quoi ?

**Pourquoi cette étape est celle qui rapporte le plus.** La quatrième question est,
mot pour mot, la définition de la règle d'autorisation dont la faille BOLA du J2 est
la violation. Un modèle produit clair au J1 rend la faille du J2 évidente. Un modèle
flou la rend invisible.

**Artefact :** `docs/produit.md` — une page, pas dix.

---

## 2 · Installer les compétences de l'agent

**Le principe.** Fournir à l'agent un savoir-faire par outil, installé une fois, pour
qu'il cesse de deviner à partir de données d'entraînement périmées. Les prompts
quotidiens redeviennent courts, puisque le savoir est déjà là.

**Dans cette formation, c'est plus qu'utile : c'est nécessaire.** Spring Boot 4 et
Spring Framework 7 sont récents. L'agent produira spontanément du code Spring Boot 3,
avec des API et des configurations qui n'existent plus — et il le fera avec assurance.
C'est le mode d'échec numéro un de ce projet.

Les compétences à écrire pour le projet SupportDesk :

| Compétence | Ce qu'elle contient |
|---|---|
| `spring-boot-4` | Ce qui a changé depuis Spring Boot 3 : configuration de sécurité, API dépréciées, nouveautés du framework 7 |
| `angular-22` | Détection `OnPush` par défaut, Signal Forms stables, API `resource()`, décorateur `@Service`, `HttpClient` sur Fetch, TypeScript 6 obligatoire |
| `keycloak-oidc` | Validation de l'audience, conversion des rôles en autorités, le piège de l'`issuer`, l'intégration `keycloak-angular` |
| `testcontainers` | Le patron de test d'intégration du projet, la réutilisation du conteneur |
| `revue-owasp` | *(déjà fournie)* L'audit de sécurité en cinq risques |

**Le versant Angular est au moins aussi critique que le versant Spring.** Angular 22
(juin 2026) a changé la stratégie de détection de changement par défaut, stabilisé les
Signal Forms avec une API différente de la précédente, rendu TypeScript 6 obligatoire
et basculé `HttpClient` sur Fetch. Un agent produira spontanément du code Angular 19
ou 20 : modules au lieu de composants autonomes, `ChangeDetectionStrategy` implicite
supposée `Eager`, `ReactiveFormsModule` là où le projet utilise les Signal Forms. Le
code compilera parfois, et se comportera autrement.

Sur les deux extrémités de la pile, le même mécanisme d'échec : **le framework a bougé
plus vite que les données d'entraînement du modèle.** C'est précisément ce que la
compétence corrige.

**La règle qui sépare une bonne compétence d'une mauvaise :** une compétence contient
ce que le modèle **ignore ou se trompe**, pas une copie de la documentation. Recopier
la doc gaspille du contexte et n'améliore rien.

**Point de vigilance — sécurité de la chaîne d'approvisionnement.** Installer un pack
de compétences tiers (`npx skills@latest add <auteur>/<pack>`) revient à faire entrer
dans votre projet des instructions écrites par quelqu'un d'autre, que l'agent suivra.
Cela se lit avant installation, exactement comme une dépendance Maven — et cela se
verrouille sur une version.

**Artefact :** `.claude/skills/<nom>/SKILL.md`

---

## 3 · Le fichier de contexte du projet

**Correction importante par rapport à la méthode d'origine.** Celle-ci parle d'un
fichier `agents.md`. **Claude Code lit `CLAUDE.md`, pas `AGENTS.md`** — c'est
explicite dans la documentation officielle. Si votre dépôt possède déjà un
`AGENTS.md` pour d'autres agents, ne le dupliquez pas : créez un `CLAUDE.md` qui
l'importe.

```markdown
@AGENTS.md

## Spécifique à Claude Code
Utiliser le mode plan pour toute modification sous src/main/java/.../securite/.
```

Un lien symbolique fonctionne aussi (`ln -s AGENTS.md CLAUDE.md`), sauf sous Windows
où l'import est préférable.

**Ce que contient le fichier.** Un rôle attribué, une description courte du produit,
les contraintes techniques non négociables, les conventions de code, et les règles de
travail — dont la plus importante : ne jamais coder sans plan approuvé.

**Trois niveaux, et non deux.** La méthode d'origine distingue le savoir technique
(compétences) des règles de projet (`CLAUDE.md`). La documentation de Claude Code en
ajoute un troisième, très utile sur un projet à plusieurs modules :

| Où | Quoi | Quand c'est chargé |
|---|---|---|
| `.claude/skills/` | Savoir-faire technique, procédures | À la demande, quand c'est pertinent |
| `CLAUDE.md` | Règles valables partout dans le projet | À chaque session |
| `.claude/rules/*.md` avec `paths:` | Règles ne valant que pour un répertoire | Quand l'agent ouvre un fichier correspondant |

**Les contraintes de forme comptent autant que le contenu :**

- Viser **moins de 200 lignes**. Au-delà, l'adhérence baisse et le contexte se remplit.
- Être **vérifiable** : « indentation de 4 espaces » plutôt que « code bien formaté ».
- Ne jamais laisser deux règles se contredire — l'agent en choisira une au hasard.
- Vérifier que le fichier est bien chargé avec `/context`, et l'éditer avec `/memory`.
- `/init` génère un premier jet à partir du code existant ; on l'affine ensuite.

**Ce qui ne relève pas du `CLAUDE.md` :** une consigne qui doit s'appliquer à un
moment précis et sans exception — avant chaque commit, après chaque édition — n'est
pas une instruction, c'est un *hook*. Le `CLAUDE.md` oriente le comportement ; il ne
le contraint pas.

**Artefact :** `CLAUDE.md` (fourni, à enrichir), `.claude/rules/`

---

## 4 · Le cycle plan puis approbation

**Le principe.** Pour chaque fonctionnalité, l'agent analyse le code existant, lit les
compétences installées, et rédige une **proposition d'implémentation** dans un fichier
markdown. Vous l'approuvez explicitement. Ensuite seulement, il code.

**Le mode plan de Claude Code fait la moitié du travail.** Il empêche l'exécution
prématurée. Mais son plan vit dans la conversation : il disparaît. Le fichier dans
`prompts/` fait l'autre moitié — il survit, il se relit, il se compare au code livré,
et il se versionne avec lui.

Le plan d'une fonctionnalité SupportDesk doit contenir :

1. **Contexte** — la demande, en une phrase.
2. **Fichiers inspectés** — ce que l'agent a réellement lu avant de proposer.
3. **Hypothèses** — ce qu'il a supposé faute d'information. *La section la plus utile
   du document : c'est là que se trouvent les erreurs.*
4. **Modèle et migration** — le schéma visé, la migration Flyway prévue.
5. **Fichiers créés ou modifiés** — la liste, avant.
6. **Impact sécurité** — qui a le droit d'appeler cela, et où la vérification se fait.
   *Section propre à ce projet, ajoutée parce que c'est exactement ce qu'un agent
   oublie.*
7. **Critères d'acceptation** — observables.
8. **Comment on teste** — quel test, à quel niveau, et ce qu'il doit échouer à faire.

**La discipline réelle :** relire la section « hypothèses » avant tout le reste, et
refuser un plan qui produirait plus de deux cents lignes de diff. Un plan trop gros
n'est pas relisible, donc il ne sera pas relu, donc l'étape ne sert plus à rien.

**Artefact :** `prompts/NNN-nom-fonctionnalite.md` — voir le modèle fourni dans ce
dossier.

---

## 5 · Tests et vérification automatisée

**Le principe.** Pendant le développement, l'agent exécute lui-même les vérifications
— compilation, analyse statique, tests — et ne déclare la tâche terminée qu'au vert.

**Transposition honnête.** La méthode d'origine s'appuie sur Playwright et des
captures d'écran multi-résolutions, parce qu'elle vise une application web dont le
rendu est le livrable. Pour une API Spring, la vérification équivalente est ailleurs :

La pile étant full stack, la vérification l'est aussi — elle a deux versants qui ne
se remplacent pas :

| Versant | Ce que l'agent exécute lui-même |
|---|---|
| **Back — Spring** | `./mvnw verify` : compilation, tests unitaires, tests d'intégration Testcontainers sur le vrai PostgreSQL |
| **Front — Angular** | `ng build` puis `ng test` ; la compilation stricte de TypeScript 6 attrape déjà une bonne part du code généré pour une version antérieure |
| **Bout en bout** | Playwright : le parcours de connexion SSO puis une action métier, joué pour de vrai dans un navigateur |
| **Visuel** | Captures d'écran Playwright à deux ou trois largeurs, comparées à l'écran de référence |
| **Sécurité** | Scan OWASP ZAP — l'équivalent, côté API, de la capture d'écran : une vérification automatique de l'état attendu |

**Le test de bout en bout est le seul qui prouve que la chaîne complète tient.** Un
jeton valide côté Spring et un `HttpClient` correct côté Angular peuvent parfaitement
coexister sans que la connexion fonctionne — c'est exactement le piège de l'`issuer`
Keycloak. Ni les tests back ni les tests front ne le détectent ; Playwright, si.

**La règle à inscrire dans le `CLAUDE.md` :** l'agent ne dit jamais « c'est fait ».
Il dit « la vérification passe », et il montre la commande. La nuance n'est pas
rhétorique — c'est la différence entre une affirmation et une preuve.

**Le point de bascule pédagogique.** L'agent ne voit pas ce qu'il n'exécute pas. Il
suppose son code correct. Le N+1 provoqué volontairement au J1 le démontre : tant
qu'on ne lui montre pas les logs SQL, le problème n'existe pas pour lui.

---

## 6 · Flux Git et revue à froid

**Le principe.** Une fonctionnalité, une branche, un commit propre, une *pull
request*. Avant la fusion, une revue automatisée cherche les vulnérabilités, les
secrets en dur, et les défauts de logique.

**Ce qui compte vraiment, et que la formulation d'origine sous-estime :** la revue ne
doit pas être faite par le contexte qui a écrit le code. Un agent qui relit sa propre
production ne voit pas ce qu'il n'a pas su voir en écrivant — il n'a aucune raison de
changer d'avis. Le relecteur doit **repartir du diff, à froid**.

C'est exactement ce qui s'est passé sur le socle de cette formation : un second agent,
sans le contexte du premier, y a trouvé quatre défauts bloquants qu'aucune relecture
du code seul n'aurait révélés. Cet audit croisé est rejoué en séance au J2.

**Sur ce projet :** le sous-agent `code-reviewer` (`.claude/agents/code-reviewer.md`)
tient ce rôle localement, et la CI porte la vérification automatisée. Un service de
revue de PR type CodeRabbit s'ajoute utilement sur un vrai projet ; il n'est pas
nécessaire pendant les quatre jours.

**Ce que la revue cherche en priorité sur du code généré :**

- Une API, une méthode ou une annotation qui n'existe pas dans la version utilisée.
- Du code plausible mais mort : un service jamais injecté, une branche inatteignable.
- Une abstraction introduite sans besoin exprimé.
- Un test sans assertion réelle.
- **Un accès à une ressource sans vérification du propriétaire.**
- Un secret en dur, une politique CORS permissive, un endpoint public sans limitation
  de débit.

Les trois derniers points sont, mot pour mot, le programme sécurité du J2. Ce n'est pas une
coïncidence : les défauts de sécurité que produit un agent sont les mêmes que ceux
que l'OWASP recense.

**Artefact :** l'historique Git lui-même — une branche par module, un diff relu par
commit.

---

## 7 · Mesurer et apprendre

**Le principe.** Le travail ne s'arrête pas au déploiement. On mesure ce que font
réellement les utilisateurs, on détecte les frictions, on corrige.

**Position honnête pour cette formation : hors périmètre.** Il n'y a pas
d'utilisateurs, pas de trafic, pas de parcours à mesurer. Prétendre traiter cette
étape en quatre jours serait la traiter en diaporama.

**Ce qu'on en garde malgré tout**, parce que le principe est juste et que l'équivalent
côté API est atteignable : `/actuator/health` exposé et le reste fermé, des logs
structurés exploitables, et le scan de sécurité rejoué à chaque intégration. C'est la
version minimale de « on continue à regarder après avoir livré ».

**Un réflexe d'ingénierie agentique à appliquer ici même :** les capacités précises
des outils d'analytique produit — en particulier ceux qui proposent d'ouvrir des
correctifs automatiquement — évoluent vite. Avant d'enseigner ou d'intégrer une telle
fonctionnalité, il faut la vérifier dans la documentation de l'éditeur plutôt que dans
un tutoriel. C'est le même réflexe que celui qu'on applique au code de l'agent.

---

## 8 · Le workflow lui-même devient une compétence

**Le principe.** Passer du prototype à la production en outillant le processus par des
commandes réutilisables plutôt que par des habitudes personnelles.

**Précision par rapport à la méthode d'origine.** Le pack public qui porte ce workflow
(`jsmastery-pro/skills`, licence MIT) en compte **neuf**, et non quatre :

| Commande | Rôle |
|---|---|
| `scope` | Transformer une idée produit en périmètre, et le tenir à jour |
| `audit` | Générer les fichiers de contexte à partir du code existant |
| `architect` | Prendre et consigner les décisions techniques structurantes |
| `develop` | Implémenter à partir d'une spécification |
| `check` | Vérifier ou faire relire une modification |
| `test` | Générer les suites de tests |
| `document` | Rédiger le texte de PR, le changelog, les notes de version |
| `sync` | Remettre à jour les documents de contexte après fusion |
| `debug` | Trouver une cause racine et produire le test de non-régression |

Installation : `npx skills@latest add jsmastery-pro/skills -a claude-code`.

**Recommandation pour les quatre jours : ne pas l'installer.** Neuf compétences
méta-workflow devant un étudiant qui découvre Spring Boot déplacent le sujet : on
passerait la journée à apprendre le processus au lieu de construire l'application.
On garde le **principe** — le workflow est une compétence versionnée, pas une
habitude — et on l'implémente avec trois compétences maison, écrites par l'étudiant,
qu'il comprend entièrement.

Le pack reste une excellente base **après** la formation, une fois que la méthode est
comprise de l'intérieur. À lire avant d'installer, comme toute dépendance.

---

## Ce que la méthode ne fait pas

Trois limites, à dire clairement plutôt qu'à laisser découvrir.

**Elle ne remplace pas la compétence technique.** Un plan approuvé par quelqu'un qui
ne sait pas le lire ne vaut rien — il ajoute même une fausse assurance. Toute la
méthode repose sur un humain capable de dire non, et c'est pour cela que l'épreuve
sans agent du J4 existe.

**Elle a un coût fixe.** Le cérémonial complet sur une correction de trois lignes
coûte plus cher que le bug. Le jugement fait partie de la méthode : le plan écrit est
pour les fonctionnalités, pas pour les fautes de frappe.

**Son échec le plus courant est le théâtre de processus.** Approuver des plans sans
les lire, ouvrir des PR qu'on fusionne sans regarder, écrire des tests qui ne testent
rien. Le processus est alors intégralement respecté, et intégralement inutile — avec,
en prime, la conviction d'avoir bien travaillé. C'est le risque contre lequel la
formation vaccine en faisant relire, à chaque fin de journée, ce que l'agent a
réellement produit.
