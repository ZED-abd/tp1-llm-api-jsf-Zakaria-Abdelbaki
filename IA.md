# Utilisation de l'IA — TP 1

**Étudiant :** ABDELBAKI Zakaria — n° 1  
**Cours :** Agents conversationnels en Java avec LangChain4j (Richard Grin)  
**Projet :** TP 1 - Accès direct à l'API d'un LLM avec Jakarta EE et JSF  

J'ai utilisé Antigravity pour la version initiale du TP et Codex pour la finalisation du projet. Le code métier s'appuie sur les consignes et les fragments fournis par Richard Grin. L'IA a aidé à assembler l'architecture Maven, écrire les classes Java manquantes (`LlmInteraction`, `RequeteException`, `LlmClientPourGemini`), intégrer le mode debug PrimeFaces dans JSF.

---

## 1. Initialisation du projet et architecture globale

### Problème rencontré
Il fallait structurer un nouveau projet Maven `tp1-llm-api-jsf-zakaria-abdelbaki` reprenant les acquis du TP 0, en remplaçant le service de mock local par une intégration REST directe avec l'API de Google Gemini.

### Prompt utilisé
> Initialise le TP Maven « TP 1 - Accès direct à l'API d'un LLM, avec Jakarta EE et en particulier JSF »...

### Résolution et Apports de l'IA
L'IA a mis en place l'arborescence standard Maven, le fichier `pom.xml` adapté (Jakarta EE 11 Web Profile, PrimeFaces 16.0.0, Java release 21), les descripteurs (`web.xml`, `faces-config.xml`, `beans.xml`) et a découpé les packages de façon modulaire :
- `com.zakariaabdelbaki.tp1.jsf` pour le contrôleur Web JSF (`Bb.java`).
- `com.zakariaabdelbaki.tp1.llm` pour la communication bas niveau REST/JSON avec l'API Gemini.

### Ce que j'ai appris
La séparation nette entre le backing bean (responsable uniquement de l'interaction avec la vue XHTML) et l'adaptateur LLM (`JsonAdapterPourGemini`) respecte le principe de responsabilité unique (SRP). L'injection de dépendances CDI (`@Inject`) avec la portée `@Dependent` permet de synchroniser naturellement le cycle de vie de l'adaptateur et de son historique JSON avec celui de la vue utilisateur.

---

## 2. Implémentation du Record `LlmInteraction` et de l'exception `RequeteException`

### Problème rencontré
Le sujet demandait d'écrire un record Java `LlmInteraction` à 3 membres utilisé pour renvoyer simultanément la question JSON, la réponse JSON et le texte extrait de la réponse.

### Résolution et Apports de l'IA
L'IA a analysé la méthode `envoyerRequete` de `JsonAdapterPourGemini` et l'appel dans le backing bean `Bb` :
```java
public record LlmInteraction(String questionJson, String reponseJson, String reponseExtraite) implements Serializable {}
```
Elle a également structuré la classe `RequeteException` en encapsulant le code HTTP de retour et le corps JSON de la requête rejetée.

### Ce que j'ai appris
Les `records` (introduits en Java 16) permettent de définir en une seule ligne des conteneurs de données immuables sans écrire de boilerplate (getters, constructeur canonique, `equals`, `hashCode`, `toString`).

---

## 3. Communication REST avec l'API Gemini via JAX-RS (`LlmClientPourGemini`)

### Problème rencontré
Il fallait se connecter à l'endpoint de Google Gemini sans aucune librairie externe (sans SDK Google), en utilisant uniquement l'API standard JAX-RS de Jakarta EE (`jakarta.ws.rs.client.*`).

### Résolution et Apports de l'IA
L'IA a complété le squelette de `LlmClientPourGemini` :
1. Récupération sécurisée de la clé d'API via `System.getenv("GEMINI_KEY")`.
2. Configuration du `WebTarget` sur le point d'accès v1beta de Gemini avec le modèle `gemini-3.5-flash-lite` et l'en-tête `x-goog-api-key`.
3. Fermeture propre des ressources HTTP lors de la destruction du bean via `@PreDestroy`.

### Ce que j'ai appris
JAX-RS Client offre une abstraction fluide pour construire des requêtes HTTP POST avec `target.request(MediaType.APPLICATION_JSON_TYPE).post(requestEntity)`. Cela permet de rester indépendant de tout SDK propriétaire.

---

## 4. Gestion de l'historique conversationnel avec Jakarta JSON Processing

### Problème rencontré
Comprendre pourquoi et comment l'historique conversationnel est conservé et envoyé à chaque requête.

### Résolution et Apports de l'IA
L'IA a expliqué le rôle du `JsonPointer` (`/contents/-`) dans `JsonAdapterPourGemini`. L'API REST de Gemini étant *stateless*, le serveur distant n'a aucune mémoire des échanges. Le client doit obligatoirement renvoyer le tableau `contents` enrichi des tours précédents (`user` -> `model` -> `user`).

---

## 5. Mode Debug et composant PrimeFaces ToggleSwitch

### Problème rencontré
Ajouter deux zones de texte conditionnelles pour afficher le JSON de la requête et de la réponse, pilotées par un switch interactif.

### Résolution et Apports de l'IA
L'IA a intégré dans `index.xhtml` le composant `<p:toggleSwitch>` avec `<p:ajax update="paneldebugwrapper"/>` enveloppant le `<h:panelGrid id="paneldebug" rendered="#{bb.debug}">`, assurant une apparition/disparition immédiate et fluide du panneau debug.
