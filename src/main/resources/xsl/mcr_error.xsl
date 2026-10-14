<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:mcri18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="encoder mcri18n mcrxsl">

  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="xslInclude:ErrorPage" />

  <xsl:param name="REP.ProductionMode" select="''" />
  <xsl:param name="REP.ErrorPage.Mail.General" select="''" />
  <xsl:param name="REP.ErrorPage.Mail.Technical" select="''" />

  <xsl:variable name="Type" select="'document'" />
  <xsl:variable name="PageTitle" select="
    mcri18n:translate('titles.pageTitle.error', concat(' ', /mcr_error/@HttpError))
  " />

  <xsl:template match="/mcr_error">
    <h1>Es ist ein Fehler aufgetreten</h1>
    <div class="row">
      <div class="col-md-8" lang="de">
        <xsl:apply-templates select="." mode="error-content" />
      </div>
      <xsl:if test="$REP.ProductionMode = 'false' and exception/trace">
        <div class="hidden">
          <div class="panel panel-warning">
            <div class="panel-heading">
              <xsl:value-of select="concat(mcri18n:translate('error.stackTrace'),' :')" />
            </div>
            <div class="panel-body">
              <xsl:for-each select="exception/trace">
                <pre style="font-size:0.8em;">
                  <xsl:value-of select="." />
                </pre>
              </xsl:for-each>
            </div>
          </div>
        </div>
      </xsl:if>
    </div>
  </xsl:template>

  <xsl:template match="mcr_error[@HttpError='500']" mode="error-content">
    <h2>Interner Serverfehler</h2>
    <p>
      Es ist leider ein Serverfehler aufgetreten.
      Wir arbeiten an dessen Beseitigung!
      <xsl:if test="string-length($REP.ErrorPage.Mail.Technical) &gt; 0">
        Gern können Sie uns eine Mail an
        <xsl:call-template name="mail">
          <xsl:with-param name="address" select="$REP.ErrorPage.Mail.Technical" />
        </xsl:call-template> schicken und kurz schildern, wie es zu diesem Fehler kam.
        Vielen Dank!
      </xsl:if>
    </p>
  </xsl:template>

  <xsl:template match="mcr_error[@HttpError='404']" mode="error-content">
    <h2><xsl:value-of select="." /></h2>
    <p>
      Die von Ihnen angeforderte Seite konnte leider nicht gefunden werden.
      Eventuell haben Sie ein altes Lesezeichen oder einen veralteten Link benutzt.
      Bitte versuchen Sie, mithilfe der <a href="/index.html">Suche</a> die gewünschte Seite zu finden.
      <xsl:if test="string-length($REP.ErrorPage.Mail.General) &gt; 0">
        Alternativ können Sie eine Mail an
        <xsl:call-template name="mail">
          <xsl:with-param name="address" select="$REP.ErrorPage.Mail.General" />
        </xsl:call-template> schicken und schildern darin kurz, wie es zu diesem Fehler kam.
        Vielen Dank!
      </xsl:if>
    </p>
  </xsl:template>

  <!-- servlets mostly respond with 403 even for guests, so distinguish by login state instead of status code -->
  <xsl:template match="mcr_error[@HttpError='401' or @HttpError='403']" mode="error-content">
    <xsl:choose>
      <xsl:when test="mcrxsl:isCurrentUserGuestUser()">
        <xsl:variable name="loginURL"
          select="concat($ServletsBaseURL, 'MCRLoginServlet', $HttpSession, '?url=', encoder:encode(string($RequestURL)))" />
        <h2>Anmeldung erforderlich</h2>
        <p>
          Sie müssen angemeldet sein, um diese Seite zu sehen.
          Bitte <a href="{$loginURL}">melden Sie sich am System an</a>.
          <xsl:if test="string-length($REP.ErrorPage.Mail.General) &gt; 0">
            Sollten Sie trotz Anmeldung keinen Zugriff auf diese Seite haben, wenden Sie sich ggf. an
            Ihren Administrator oder schreiben Sie eine Mail an
            <xsl:call-template name="mail">
              <xsl:with-param name="address" select="$REP.ErrorPage.Mail.General" />
            </xsl:call-template>.
            Vielen Dank!
          </xsl:if>
        </p>
      </xsl:when>
      <xsl:otherwise>
        <h2>Zugriff verweigert</h2>
        <p>
          Sie sind als <strong><xsl:value-of select="$CurrentUser" /></strong> angemeldet,
          haben aber keine Berechtigung, diese Seite zu sehen.
          <xsl:if test="string-length($REP.ErrorPage.Mail.General) &gt; 0">
            Sollten Sie der Meinung sein, dass Sie die nötigen Rechte haben müssten, wenden Sie sich ggf. an
            Ihren Administrator oder schreiben Sie eine Mail an
            <xsl:call-template name="mail">
              <xsl:with-param name="address" select="$REP.ErrorPage.Mail.General" />
            </xsl:call-template>.
            Vielen Dank!
          </xsl:if>
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mcr_error" mode="error-content">
    <h2><xsl:value-of select="." /></h2>
    <p>
      Es ist leider ein Fehler aufgetreten.
      <xsl:if test="string-length($REP.ErrorPage.Mail.Technical) &gt; 0">
        Sollte dies wiederholt der Fall sein, schreiben Sie bitte eine Mail an
        <xsl:call-template name="mail">
          <xsl:with-param name="address" select="$REP.ErrorPage.Mail.Technical" />
        </xsl:call-template> und schildern kurz, wie es dazu kam.
        Vielen Dank!
      </xsl:if>
    </p>
  </xsl:template>

  <xsl:template name="mail">
    <xsl:param name="address" />

    <span class="madress">
      <xsl:value-of select="concat(substring-before($address, '@'), ' [at] ', substring-after($address, '@'))" />
    </span>
  </xsl:template>

</xsl:stylesheet>
