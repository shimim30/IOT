Projet IoT – Application Android & MicroPython

Présentation
Ce projet IoT consiste en une application Android qui se connecte à une carte STM32 via Bluetooth Low Energy (BLE). La carte STM32 est programmée en MicroPython et lit plusieurs capteurs (température, humidité, lumière) tout en contrôlant un servomoteur. L'application Android permet d'afficher les données en temps réel et de visualiser le comportement du panneau solaire.

Réalisé par
S’himi Mohammed
Oulali Bachir
Boudal Ishaq


Technologies utilisées
Côté Android :
- Java
- Jetpack Compose
- Dagger Hilt
- Navigation Compose
- Bluetooth Low Energy (BLE)
- Accompanist pour les permissions
Côté STM32 (MicroPython) :
- MicroPython
- Capteurs : DHT22, Grove Light Sensor v1.2
- Actionneur : servomoteur contrôlé via PWM
- Communication BLE UART


Fonctionnement
STM32 (MicroPython)
- Toutes les 5 secondes :
  - Lecture température et humidité (DHT22)
  - Lecture intensité lumineuse gauche et droite
  - Calcul de l'angle optimal pour le panneau solaire
  - Envoi BLE format compact :

    
Application Android
- Scan BLE uniquement après validation des permissions
- Connexion à la carte STM32
- Lecture et affichage des données :
  - Température (T)
  - Humidité (H)
  - Lumière gauche (L)
  - Lumière droite (R)
  - Angle du servomoteur (A)

  
Installation
Android
1. Cloner le dépôt
2. Ouvrir le projet avec Android Studio
3. Synchroniser Gradle
4. Brancher un téléphone Android avec le Bluetooth activé
5. Lancer l'application

   
STM32
1. Flasher MicroPython sur la carte STM32
2. Uploader main.py avec Thonny ou un autre outil
3. Connecter les capteurs et le servomoteur selon le code
   
Fonctionnalités prévues ou améliorations
- Ajout d’un historique des mesures
- Graphiques pour la température et la lumière
- Mode manuel pour contrôler le servomoteur
- Alertes en cas de conditions critiques
