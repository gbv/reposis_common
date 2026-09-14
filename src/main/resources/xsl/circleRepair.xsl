<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:include href="MyCoReLayout.xsl"/>
  <xsl:variable name="PageID" select="'circle-repair'"/>
  <xsl:variable name="PageTitle" select="'Kreisverweise prüfen'"/>

  <xsl:template match="/circleRepair">
    <link rel="stylesheet" href="{$WebApplicationBaseURL}css/circle-repair.css"/>
    <script src="{$WebApplicationBaseURL}js/circle-repair.js" defer="defer"><xsl:text> </xsl:text></script>
    <section id="circle-repair">
      <header class="repair-header">
        <div><p class="eyebrow">Bearbeitung · Metadaten</p><h1>Kreisverweise prüfen</h1>
          <p>Kreisverweise und Änderungsvorschläge ansehen. Die Bearbeitung erfolgt im Editor des jeweiligen Dokuments.</p></div>
        <button id="refresh" class="btn btn-secondary" type="button">Bestand erneut prüfen</button>
      </header>
      <div id="notice" role="alert" hidden="hidden"/>
      <p id="status" role="status" aria-live="polite">Prüfung abgeschlossen am <xsl:value-of select="@generated"/>. <xsl:value-of select="count(group)"/> Kreisgruppen gefunden.</p>
      <p class="hint">Die Kreissuche verwendet den Verweisindex der Datenbank. Details werden für gefundene Kreisgruppen aus den Dokumenten gelesen. Nicht indizierte Verweise werden nicht erfasst.</p>
      <noscript>Der Bericht ist vollständig sichtbar. Zum Filtern und Nachladen bitte JavaScript aktivieren.</noscript>
      <section class="stats" aria-label="Ergebnis der Prüfung">
        <div><strong id="objects"><xsl:value-of select="@objects"/></strong><span>Dokumente im Verweisnetz</span></div>
        <div><strong id="ready"><xsl:value-of select="count(group[@suggested='true'])"/></strong><span>Änderungsvorschläge</span></div>
        <div><strong id="manual"><xsl:value-of select="count(group[@suggested='false'])"/></strong><span>Fachlich zu prüfen</span></div>
      </section>
      <div class="toolbar">
        <label>Dokument suchen<input id="search" class="form-control" type="search" placeholder="Objekt-ID eingeben" autocomplete="off"/></label>
        <label>Anzeige<select id="filter" class="form-control"><option value="all">Alle Fälle</option><option value="ready">Mit Änderungsvorschlag</option><option value="manual">Fachliche Prüfung nötig</option></select></label>
      </div>
      <p class="hint">Diese Seite prüft die indizierten Verweise und ändert keine Dokumente. Öffnen Sie ein Dokument über seine ID
        und wählen Sie dort „Bearbeiten“. Nach dem Speichern im Editor klicken Sie hier auf „Bestand erneut prüfen“.
        Die vorhandenen Bearbeitungsrechte gelten weiterhin. Falls der Editor wegen eines Kreises nicht speichern kann,
        geben Sie die betroffenen IDs zur technischen Klärung weiter.</p>
      <section id="cases" aria-label="Kreisgruppen" aria-busy="false">
        <xsl:apply-templates select="group"/>
        <p id="empty" class="empty">
          <xsl:if test="group"><xsl:attribute name="hidden">hidden</xsl:attribute></xsl:if>
          <xsl:choose><xsl:when test="group">Keine Fälle passen zu dieser Auswahl.</xsl:when><xsl:otherwise>Es wurden keine Kreisverweise gefunden.</xsl:otherwise></xsl:choose>
        </p>
      </section>
      <button id="more" class="btn btn-secondary" type="button" hidden="hidden">Weitere Fälle anzeigen</button>
    </section>
  </xsl:template>

  <xsl:template match="group">
    <details data-suggested="{@suggested}">
      <xsl:attribute name="data-members"><xsl:for-each select="member"><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:for-each></xsl:attribute>
      <summary>
        <span class="group-title">Gruppe <xsl:value-of select="position()"/></span>
        <xsl:choose>
          <xsl:when test="@suggested='true'"><span class="repair-badge"><xsl:value-of select="count(link[@remove='true'])"/><xsl:choose><xsl:when test="count(link[@remove='true'])=1"> Verweis</xsl:when><xsl:otherwise> Verweise</xsl:otherwise></xsl:choose> zur Prüfung</span></xsl:when>
          <xsl:otherwise><span class="repair-badge manual">Fachliche Prüfung nötig</span></xsl:otherwise>
        </xsl:choose>
        <span class="description"><xsl:value-of select="count(member)"/> Dokumente · <xsl:for-each select="member[position() &lt;= 3]"><xsl:if test="position()!=1">, </xsl:if><xsl:value-of select="."/></xsl:for-each><xsl:if test="count(member)&gt;3">, …</xsl:if></span>
      </summary>
      <div class="repair-body">
        <p><xsl:value-of select="explanation"/></p>
        <div class="members"><xsl:for-each select="member"><xsl:call-template name="object-link"><xsl:with-param name="id" select="."/></xsl:call-template></xsl:for-each></div>
        <div class="table-responsive"><table class="table">
          <thead><tr><th scope="col">Vorschlag</th><th scope="col">Von</th><th scope="col">Beziehung / Position</th><th scope="col">Nach</th><th scope="col">Begründung</th></tr></thead>
          <tbody><xsl:apply-templates select="link"/></tbody>
        </table></div>
        <p class="hint">Die verknüpften Dokumente öffnen und die vorgeschlagenen Änderungen im jeweiligen Editor prüfen.
          Es werden weder Verweise noch Dokumente automatisch geändert.</p>
      </div>
    </details>
  </xsl:template>

  <xsl:template match="link">
    <tr>
      <xsl:if test="@remove='true'"><xsl:attribute name="class">remove</xsl:attribute></xsl:if>
      <td><xsl:choose><xsl:when test="@remove='true'"><strong>Entfernen prüfen</strong></xsl:when><xsl:otherwise>Beibehalten</xsl:otherwise></xsl:choose></td>
      <td><xsl:call-template name="object-link"><xsl:with-param name="id" select="@from"/></xsl:call-template></td>
      <td><xsl:value-of select="@relation"/> · <xsl:choose><xsl:when test="@position&gt;0">Position <xsl:value-of select="@position"/></xsl:when><xsl:otherwise>Struktur</xsl:otherwise></xsl:choose></td>
      <td><xsl:call-template name="object-link"><xsl:with-param name="id" select="@to"/></xsl:call-template></td>
      <td class="reason"><xsl:value-of select="."/></td>
    </tr>
  </xsl:template>

  <xsl:template name="object-link">
    <xsl:param name="id"/>
    <a href="{$WebApplicationBaseURL}receive/{$id}" target="_blank" rel="noopener noreferrer"><xsl:value-of select="$id"/></a>
  </xsl:template>
</xsl:stylesheet>
