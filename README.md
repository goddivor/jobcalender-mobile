<div align="center">

<img src="./media/icon.png" alt="Icône de JobCalender" width="120" height="120" />

# JobCalender

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue?logo=kotlin&logoColor=fff&label=Kotlin&labelColor=333&color=7F52FF&style=flat)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android&logoColor=fff&label=Android&labelColor=333&color=3DDC84&style=flat)](https://developer.android.com)
[![Release](https://img.shields.io/github/v/release/goddivor/jobcalender-mobile?logo=github&logoColor=fff&label=Release&labelColor=333&color=1476FC&style=flat)](https://github.com/goddivor/jobcalender-mobile/releases)
[![Téléchargements](https://img.shields.io/github/downloads/goddivor/jobcalender-mobile/total?logo=github&logoColor=fff&label=T%C3%A9l%C3%A9chargements&labelColor=333&color=0A66EE&style=flat)](https://github.com/goddivor/jobcalender-mobile/releases)

[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2026.08-blue?logo=jetpackcompose&logoColor=fff&label=Compose&labelColor=333&color=4285F4&style=flat)](https://developer.android.com/compose)
[![Material 3](https://img.shields.io/badge/Material_3-1.4.0-blue?logo=materialdesign&logoColor=fff&label=Material%203&labelColor=333&color=757575&style=flat)](https://m3.material.io)
[![Room](https://img.shields.io/badge/Room-3.0.1-green?logo=sqlite&logoColor=fff&label=Room&labelColor=333&color=003B57&style=flat)](https://developer.android.com/jetpack/androidx/releases/room3)
[![Hilt](https://img.shields.io/badge/Hilt-2.60.1-orange?logo=android&logoColor=fff&label=Hilt&labelColor=333&color=FF6F00&style=flat)](https://dagger.dev/hilt)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green?logo=mongodb&logoColor=fff&label=MongoDB&labelColor=333&color=47A248&style=flat)](https://www.mongodb.com/atlas)

[![Stars](https://img.shields.io/github/stars/goddivor/jobcalender-mobile?logo=github&logoColor=fff&label=Stars&labelColor=333&color=E3B341&style=flat)](https://github.com/goddivor/jobcalender-mobile/stargazers)
[![Forks](https://img.shields.io/github/forks/goddivor/jobcalender-mobile?logo=github&logoColor=fff&label=Forks&labelColor=333&color=8957E5&style=flat)](https://github.com/goddivor/jobcalender-mobile/network/members)
[![Watchers](https://img.shields.io/github/watchers/goddivor/jobcalender-mobile?logo=github&logoColor=fff&label=Watchers&labelColor=333&color=1F6FEB&style=flat)](https://github.com/goddivor/jobcalender-mobile/watchers)
[![Contributeurs](https://img.shields.io/github/contributors/goddivor/jobcalender-mobile?logo=github&logoColor=fff&label=Contributeurs&labelColor=333&color=DB61A2&style=flat)](https://github.com/goddivor/jobcalender-mobile/graphs/contributors)
[![Issues](https://img.shields.io/github/issues/goddivor/jobcalender-mobile?logo=github&logoColor=fff&label=Issues&labelColor=333&color=3FB950&style=flat)](https://github.com/goddivor/jobcalender-mobile/issues)

Une recherche d'emploi entière dans un seul calendrier : les **candidatures**, le **niveau atteint**
par chacune, et tous les **rendez-vous** qu'elles produisent.

Née d'un problème concret : vingt-cinq candidatures sur six canaux, des réponses qui arrivent par
e-mail et par WhatsApp à toute heure, un entretien et un test passés sans jamais figurer dans le
suivi écrit, et deux rendez-vous qui se sont chevauchés.

<img src="./media/calendrier.png" alt="Écran Calendrier, avec un jour en conflit d'horaire" width="260" />

<img src="./media/candidatures.png" alt="Liste des candidatures, groupée par employeur" width="260" />

<img src="./media/detail.png" alt="Détail d'une candidature et sa frise chronologique" width="260" />

</div>

## 🎖️ Fonctionnalités

- **Calendrier mensuel** : les entretiens, tests, réunions et échéances, avec un point coloré par
  famille d'événement sous chaque jour chargé.
- **Détection des conflits d'horaire** : deux rendez-vous qui se recoupent marquent leur journée
  d'un anneau rouge, visible depuis la grille du mois et pas seulement en ouvrant le jour.
- **Prochain rendez-vous en tête d'écran**, avec le temps restant et le lien de visioconférence
  quand il est arrivé.
- **Suivi du niveau atteint** : dix statuts, du brouillon à la décision, sans jamais imposer l'ordre
  de progression, parce qu'une candidature saute parfois une étape.
- **Frise chronologique** par candidature : qui a répondu, quand, et par quel canal.
- **Fonctionne hors connexion** : la base locale fait autorité, la synchronisation n'est qu'une
  sauvegarde et un passage d'un appareil à l'autre.
- **Trois thèmes** : clair, sombre et noir intégral pour les écrans AMOLED.
- **Mise à jour intégrée** depuis les publications GitHub, sans passer par un magasin d'applications.

L'application ne stocke **ni CV, ni lettre, ni e-mail, ni pièce jointe**. Elle garde seulement le nom
du dossier où ces documents vivent sur l'ordinateur : ce sont des traces, pas des archives.

## 📋 Prérequis

- Android 8.0 (API 26) ou plus récent.
- Pour la synchronisation, facultative : une instance de
  [jobcalender-server](https://github.com/goddivor/jobcalender-server) et une base MongoDB Atlas.
  Sans elle, l'application reste pleinement utilisable.

## 📦 Installation

Téléchargez l'APK depuis la
[dernière publication](https://github.com/goddivor/jobcalender-mobile/releases/latest), puis
autorisez l'installation depuis une source inconnue quand Android le demande.

Depuis les sources :

```bash
git clone https://github.com/goddivor/jobcalender-mobile.git
cd jobcalender-mobile
./gradlew assembleDebug
```

La synchronisation exige deux valeurs dans `local.properties`, qui n'est jamais versionné :

```properties
SYNC_CONFIG_URL=https://votre-instance.vercel.app
SYNC_CONFIG_KEY=la-cle-qui-debloque-api-config
```

Sans elles, l'application se construit et fonctionne, simplement sans synchronisation.

## ⚙️ Utilisation

### 📅 Voir ce qui arrive

L'écran Calendrier s'ouvre sur le prochain rendez-vous et le temps qui en sépare. La grille du mois
porte un point par famille d'événement, et un anneau rouge sur les journées où deux choses se
chevauchent.

### 💼 Suivre une candidature

L'écran Candidatures groupe par employeur et se filtre par statut. Un statut sans aucune candidature
reste sélectionnable : un compteur à zéro est une information, pas une raison de le cacher.

Le détail ouvre sur la frise du parcours, puis le contact avec ses actions (écrire, appeler,
ouvrir WhatsApp), le canal et le nom du dossier.

### ✍️ Enregistrer une réponse

Changer un statut laisse une trace dans le parcours. Pour un accusé de réception ou un refus, la date
est celle du jour. Pour un test ou un entretien, la date vit dans la convocation : l'application
ouvre le formulaire d'événement plutôt que d'inventer une date.

### ☁️ Synchroniser

L'application récupère son adresse et son jeton toute seule au premier lancement : aucun jeton ne se
saisit à la main. La synchronisation remplace la copie la plus ancienne par la plus récente, dans un
seul sens, sans fusion. Un échec n'est jamais bloquant.

## 🤝 Contribuer

Les remarques et les propositions sont bienvenues par
[issue](https://github.com/goddivor/jobcalender-mobile/issues) ou par pull request. Le développement
se fait sur `dev` ; `main` ne reçoit que des pull requests, dont le merge déclenche la publication
d'une nouvelle version.

## 📜 Licence

Projet personnel. Tous droits réservés.
