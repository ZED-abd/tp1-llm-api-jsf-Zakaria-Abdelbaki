# TP 1 — Accès direct à l'API d'un LLM avec Jakarta EE et JSF

**Étudiant** — Zakaria Abdelbaki  
**Filière** — 5IASD-R, EMSI Rabat  
**Cours** — Agents conversationnels en Java avec LangChain4j (Richard Grin)  
**Dépôt GitHub** — [ZED-abd/tp1-llm-api-jsf-Zakaria-Abdelbaki](https://github.com/ZED-abd/tp1-llm-api-jsf-Zakaria-Abdelbaki)

## 1. Description du projet

Le TP reprend l'interface du TP 0. Le serveur transmet les questions à l'API REST de Google Gemini. Jakarta REST envoie les requêtes HTTP `POST` et Jakarta JSON Processing construit leur contenu.

À chaque question, l'application envoie l'instruction système et l'historique de la conversation. Le mode debug de PrimeFaces permet d'afficher et de copier le JSON de la requête et celui de la réponse.

## 2. Prérequis

- JDK 21 ou 25. Maven compile le projet pour Java 21.
- Payara 7 et Jakarta EE 11 Web Profile.
- Apache Maven 3.9 et PrimeFaces 16.0.0.
- Clé API Gemini à configurer dans l'environnement utilisateur Windows. [Google AI Studio](https://aistudio.google.com/app/apikey)

## 3. Configuration

Démarrez Payara depuis un terminal dont l'environnement contient `GEMINI_KEY`. Vérifiez la présence de la variable avec PowerShell.

```powershell
if ($env:GEMINI_KEY) { "OK" }
```

Le modèle utilisé est `gemini-3.5-flash-lite`. La clé est envoyée dans l'en-tête HTTP `x-goog-api-key`. [Modèles Gemini](https://ai.google.dev/gemini-api/docs/models)

## 4. Compilation et déploiement

Depuis le dossier `tp1-llm-api-jsf-zakaria-abdelbaki`, avec `PAYARA_HOME` pointant vers l'installation de Payara, exécutez les commandes suivantes.

```powershell
mvn clean package
& "$env:PAYARA_HOME\bin\asadmin.bat" start-domain domain1
& "$env:PAYARA_HOME\bin\asadmin.bat" deploy --force=true --contextroot tp1-llm-api-jsf-zakaria-abdelbaki target/tp1-llm-api-jsf-zakaria-abdelbaki-1.0-SNAPSHOT.war
```

[Ouvrir l'application sur le port 8080](http://localhost:8080/tp1-llm-api-jsf-zakaria-abdelbaki/)

Choisissez un rôle, activez le mode debug et envoyez une question. Le bouton **Nouveau chat** réinitialise la conversation.

## 5. Structure du projet

| Élément | Rôle |
| --- | --- |
| `com.zakariaabdelbaki.tp1.jsf.Bb` | Backing bean JSF qui gère la vue et affiche les erreurs. |
| `com.zakariaabdelbaki.tp1.llm.JsonAdapterPourGemini` | Construit le JSON, conserve l'historique et extrait le texte de la réponse. |
| `LlmClientPourGemini` | Effectue les appels REST à Gemini. |
| `LlmInteraction` et `RequeteException` | Transportent le résultat de l'appel ou les détails de l'erreur. |
| `src/main/webapp/index.xhtml` | Interface JSF avec le mode debug PrimeFaces. |

[Énoncé du TP](docs/TP1.md) · [Utilisation de l'IA](IA.md)
