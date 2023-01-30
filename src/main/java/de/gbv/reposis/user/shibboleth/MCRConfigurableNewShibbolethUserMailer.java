package de.gbv.reposis.user.shibboleth;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.mycore.common.MCRMailer;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRUser;

public class MCRConfigurableNewShibbolethUserMailer implements MCRShibbolethNewUserHandler {

    private String mailTo;

    private String mailFrom;

    private String mailSubjectKey;

    private String mailBodyKey;

    private String mailLocale;

    private Boolean bcc;

    public MCRConfigurableNewShibbolethUserMailer() {
        setMailLocale(null);
        setBcc(true);
    }

    @Override
    public void handleNewUser(MCRUser user) {
        String userID = user.getUserName();
        String mailAddress = user.getEMailAddress();
        String realmLabel = getRealmLabel(user.getRealm());
        String realName = user.getRealName();

        String subject = getMailSubject(userID, realmLabel, mailAddress, realName);
        String mailBody = getMailBody(userID, realmLabel, mailAddress, realName);
        List<String> to = MCRConfiguration2.splitValue(getMailTo()).toList();
        String from = getMailFrom();

        MCRMailer.send(from, to, subject, mailBody, getBcc());
    }

    private String getMailSubject(String userID, String realmLabel, String mailAddress, String realName) {
        Optional<Locale> locale = getMailLocale();

        return locale
            .map(l -> translate(getMailSubjectKey(), l, userID, realmLabel, mailAddress, realName))
            .orElseGet(() -> MCRTranslation.translate(getMailSubjectKey(), userID, realmLabel, mailAddress, realName));
    }

    private String getMailBody(String userID, String realmLabel, String mailAddress, String realName) {
        Optional<Locale> locale = getMailLocale();

        return locale
            .map(l -> translate(getMailBodyKey(), l, userID, realmLabel, mailAddress, realName))
            .orElseGet(() -> MCRTranslation.translate(getMailBodyKey(), userID, realmLabel, mailAddress, realName));
    }


    public static String translate(String key, Locale locale, Object... args) {
        String translation = MCRTranslation.translate(key, locale);
        return MessageFormat.format(translation, args);
    }
    private Optional<Locale> getMailLocale() {
        return Optional.ofNullable(this.mailLocale).map(MCRTranslation::getLocale);
    }

    protected String getRealmLabel(MCRRealm realm) {
        return realm.getLabel() != null ? realm.getLabel() + "[" + realm.getID() + "]" : realm.getID();
    }

    public String getMailTo() {
        return mailTo;
    }

    @MCRProperty(name = "MailTo", required = true)
    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    @MCRProperty(name = "MailFrom", required = true)
    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailSubjectKey() {
        return mailSubjectKey;
    }

    @MCRProperty(name = "MailSubjectKey", required = true)
    public void setMailSubjectKey(String mailSubjectKey) {
        this.mailSubjectKey = mailSubjectKey;
    }

    public String getMailBodyKey() {
        return mailBodyKey;
    }

    @MCRProperty(name = "MailBodyKey", required = true)
    public void setMailBodyKey(String mailBodyKey) {
        this.mailBodyKey = mailBodyKey;
    }


    @MCRProperty(name = "MailLocaleKey", required = false)
    public void setMailLocale(String mailLocale) {
        this.mailLocale = mailLocale;
    }

    public Boolean getBcc() {
        return bcc;
    }

    @MCRProperty(name = "Bcc", required = false)
    public void setBcc(String bcc) {
        this.bcc = Boolean.parseBoolean(bcc);
    }

    public void setBcc(Boolean bcc) {
        this.bcc = bcc;
    }
}
