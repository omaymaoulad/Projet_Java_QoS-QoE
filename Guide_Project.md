# Projet Java QoS-QoE

## Description du projet

Ce projet est une application Java visant à surveiller et analyser la **Qualité de Service (QoS)** et la **Qualité d’Expérience (QoE)** dans un réseau de télécommunication. L’application offre une interface utilisateur intuitive, une connexion à une base de données Oracle pour stocker les données des utilisateurs et des mesures réseau, ainsi que des fonctionnalités statistiques pour visualiser les performances du réseau.

L’objectif principal est de fournir un outil pratique pour **suivre les indicateurs QoS et QoE**, permettre une **gestion des utilisateurs** et offrir des **rapports détaillés et visualisations graphiques**.

## Fonctionnalités

1. **Gestion des utilisateurs**

   * Authentification via login/mot de passe.
   * Différents profils utilisateur (admin, technicien, analyste).
   * Réinitialisation de mot de passe.

2. **Connexion à la base de données Oracle**

   * Stockage des utilisateurs, mesures et rapports.
   * Possibilité de synchroniser les données pour plusieurs utilisateurs.

3. **Mesure et suivi QoS**

   * Suivi des indicateurs réseau comme latence, débit, perte de paquets.
   * Génération de rapports quotidiens, hebdomadaires et mensuels.

4. **Analyse QoE**

   * Évaluation de la satisfaction des utilisateurs à partir de métriques réseau.
   * Recommandations et alertes en cas de dégradation de la qualité.

5. **Statistiques et visualisations**

   * Graphiques et tableaux interactifs des performances.
   * Statistiques globales et détaillées par type de service ou par utilisateur.

6. **Interface utilisateur**

   * Développée avec **JavaFX** pour une interface moderne et interactive.
   * Navigation intuitive avec menus, boutons et formulaires clairs.
   * Affichage des graphiques et tableaux directement dans l’interface.

7. **Fonctionnalités avancées**

   * Notifications pour alertes réseau ou seuils critiques.
   * Export des rapports au format PDF.
   * Support multi-utilisateur avec gestion de permissions.

## Technologies utilisées

* **Java 17+**
* **JavaFX** pour l’interface graphique
* **Oracle Database** pour la gestion des données
* **Maven** pour la gestion du projet et des dépendances
* **JFreeChart ou autre bibliothèque** pour les graphiques (si applicable)
* **Git/GitHub** pour le versionnement du code

## Installation

1. Cloner le projet depuis GitHub :

   ```bash
   git clone https://github.com/votre-utilisateur/Projet_Java_QoS-QoE.git
   ```

2. Importer le projet dans **IntelliJ IDEA** ou un autre IDE compatible Maven.

3. Configurer le fichier **`pom.xml`** avec les dépendances JavaFX et Oracle JDBC.

4. Configurer la connexion à la base de données dans le fichier de configuration (`db.properties` ou équivalent).

5. Compiler et lancer le projet depuis l’IDE.

## Utilisation

1. Lancer l’application avec la configuration Maven ou directement depuis l’IDE.
2. Se connecter avec un compte utilisateur existant.
3. Accéder aux différentes sections :

   * **Mesures QoS/QoE**
   * **Statistiques**
   * **Gestion utilisateurs**
   * **Rapports et export**
4. Administrer les utilisateurs et consulter les graphiques d’analyse réseau.

## Organisation du projet

```
Projet_Java_QoS-QoE/
│
├─ src/main/java/        # Code source Java
│   ├─ controllers/     # Classes des contrôleurs JavaFX
│   ├─ models/          # Classes pour les entités (User, Mesure, Rapport)
│   └─ utils/           # Classes utilitaires (DB connection, validation)
│
├─ src/main/resources/   # Fichiers FXML, CSS et images
│   ├─ fxml/
│   ├─ css/
│   └─ images/
│
├─ pom.xml               # Configuration Maven
└─ README.md             # Description du projet
```

## Contributeurs

* **Salma Jaghoua** – Développement interface et logique QoS/QoE
* **Nom du collègue** – Base de données et analyse des statistiques

## Licence

Ce projet est sous licence MIT.
