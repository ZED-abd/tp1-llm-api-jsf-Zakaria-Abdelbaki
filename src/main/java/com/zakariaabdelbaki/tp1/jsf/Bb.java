package com.zakariaabdelbaki.tp1.jsf;

import com.zakariaabdelbaki.tp1.llm.JsonAdapterPourGemini;
import com.zakariaabdelbaki.tp1.llm.LlmInteraction;
import com.zakariaabdelbaki.tp1.llm.RequeteException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation qui dure pendant plusieurs requêtes HTTP.
 * La portée view nécessite l'implémentation de Serializable (le backing bean peut être mis en mémoire secondaire).
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    private static final String ROLE_ASSISTANT = """
            You are a helpful assistant. You help the user to find the information they need.
            If the user type a question, you answer it.
            """;

    private String roleSysteme = ROLE_ASSISTANT;

    /**
     * Quand le rôle est choisi par l'utilisateur dans la liste déroulante,
     * il n'est plus possible de le modifier (voir code de la page JSF), sauf si on veut un nouveau chat.
     */
    private boolean roleSystemeChangeable = true;

    /**
     * Liste de tous les rôles de l'API prédéfinis.
     */
    private List<SelectItem> listeRolesSysteme;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;
    /**
     * Dernière réponse du serveur.
     */
    private String reponse;
    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    private boolean debug;
    private String texteRequeteJson;
    private String texteReponseJson;

    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    /**
     * Contexte JSF. Utilisé pour qu'un message d'erreur s'affiche dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    /**
     * Obligatoire pour un bean CDI (classe gérée par CDI), s'il y a un autre constructeur.
     */
    public Bb() {
    }

    public String getRoleSysteme() {
        return this.roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return this.roleSystemeChangeable;
    }

    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return this.reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if (!debug) {
            texteRequeteJson = null;
            texteReponseJson = null;
        }
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    /**
     * Envoie la question au serveur.
     *
     * @return null pour rester sur la même page.
     */
    public String envoyer() {
        if (this.question == null || this.question.isBlank()) {
            // Erreur ! Le formulaire va être réaffiché en réponse à la requête POST, avec un message d'erreur.
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        if (this.conversation.isEmpty()) {
            jsonAdapter.setSystemRole(this.roleSysteme);
        }
        try {
            LlmInteraction interaction = jsonAdapter.envoyerRequete(this.question);
            this.reponse = interaction.reponseExtraite();
            if (debug) {
                texteRequeteJson = interaction.questionJson();
                texteReponseJson = interaction.reponseJson();
            }
            this.roleSystemeChangeable = false;
            afficherConversation();
        } catch (RequeteException e) {
            if (debug) {
                texteRequeteJson = e.getRequeteJson();
                texteReponseJson = null;
            }
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur Gemini", e.getMessage()));
        }
        return null;
    }

    /**
     * Pour un nouveau chat.
     * La redirection ouvre une nouvelle vue et réinitialise la conversation.
     */
    public String nouveauChat() {
        return "index?faces-redirect=true";
    }

    /**
     * Pour afficher la conversation dans le textArea de la page JSF.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question).append("\n== Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            // Génère les rôles de l'API prédéfinis
            this.listeRolesSysteme = new ArrayList<>();
            this.listeRolesSysteme.add(new SelectItem(ROLE_ASSISTANT, "Assistant"));

            String role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));

            role = """
                    Tu es un philosophe socratique bienveillant, curieux et perspicace.
                    Pour chaque question ou réflexion posée par l'utilisateur, tu apportes une analyse nuancée
                    et profonde, puis tu termines toujours ta réponse par une question ouverte stimulante
                    pour inciter l'utilisateur à pousser sa propre réflexion.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Philosophe Socratique (Bonus)"));
        }

        return this.listeRolesSysteme;
    }

}
