# RelatedItem-Kreise: Übersicht zur manuellen Bearbeitung

## Lesende Übersicht für Bearbeiter

```text
<Anwendungs-URL>/servlets/MCRCircleRepairServlet
```

Die Seite ist für die Rollen `editor` und `admin` zugänglich. Andere Rollen lassen
sich über `MCR.CircleOverview.Roles` als kommagetrennte Liste konfigurieren.

Beim Öffnen wird der Verweisindex in der Datenbank geprüft. Die Übersicht zeigt Kreisgruppen, betroffene
Verweise und begründete Änderungsvorschläge. Suche und Filter helfen bei der Auswahl.
Die Objekt-IDs öffnen die normale Dokumentansicht in einem neuen Tab. Dort wählen
Bearbeiter den vorgesehenen Editor über „Bearbeiten“; dessen Berechtigungsprüfung
bleibt maßgeblich. Nach dem Speichern aktualisiert „Bestand erneut prüfen“ die Übersicht.

**Die Seite ändert keine Bestandsdaten.** Fix-Buttons und der schreibende POST-Endpunkt
sind entfernt; POST-Anfragen werden mit HTTP 405 abgewiesen. Auch Reparatur-Payloads,
Prüfsummen und CSRF-Tokens werden nicht mehr an die Oberfläche geliefert. Der Servlet
verwendet eine reine Lese-Schnittstelle und ruft keine CLI- oder Reparaturmethoden auf.
Es werden keine Report-, Plan- oder Backup-Dateien erzeugt.
Fehlende Berechtigungen werden als `MCRAccessException` an die normale MyCoRe-
Fehlerbehandlung weitergegeben. Sonstige unerwartete Fehler werden ebenfalls
weitergereicht; erwartete HTTP-Fehler verwenden `sendError`. Der Servlet erzeugt
keine eigenen JSON-Fehlerantworten.

Die Kreissuche lädt `parent`- und `reference`-Kanten aus `MCRLINKHREF` mit einer
JPA-Projektionsabfrage. Sie lädt weder alle XML-Dokumente noch pro Dokument einzelne
Datenbankeinträge. Erst für die Mitglieder zyklischer Komponenten werden XML-Dokumente
gelesen, um direkte Beziehungen, Positionen und zusätzliche Metadaten zu prüfen.
Dadurch werden veraltete positive Indexeinträge nicht ungeprüft als Vorschlag angezeigt.
Die Zahl „Dokumente im Verweisnetz“ zählt die eindeutigen Objekt-IDs dieser Kanten,
nicht alle Dokumente des Repositories. Die Übersicht zeigt einzelne tatsächliche Kreisgruppen.

Voraussetzung ist ein aktueller Linkindex. Nicht indizierte Beziehungen und ausschließlich
in übernommenem XML enthaltene Kreise sind damit nicht vollständig erkennbar. Insbesondere
indiziert MyCoRe 2023.06 MODS-Hostbeziehungen über den strukturellen Elternverweis.
Die Übersicht baut den Index nicht selbst neu auf und schreibt keine Daten.

Darstellung: XML-Modell `circleRepair` → `MCRLayoutService` → `xsl/circleRepair.xsl`
mit `MyCoReLayout.xsl` und dem Layout der Anwendung. JavaScript filtert die vorhandenen
Elemente und lädt bei Bedarf eine neue serverseitig gerenderte Ansicht per GET.

Ein Vorschlag garantiert nicht, dass das Dokument im Editor gespeichert werden kann:
Bestehende Kreise können die Metadatenübernahme weiterhin blockieren. In diesem Fall
müssen die betroffenen IDs zur technischen Klärung weitergegeben werden.

Alle Circle-CLI-Kommandos einschließlich Diagnose, Dry-Run und Reparatur sind entfernt.
Änderungen erfolgen ausschließlich manuell im vorhandenen Editor.

## Verhalten unter MyCoRe 2025.12

Quellcodeprüfung der Releases **2025.12.0, 2025.12.1 und 2025.12.2**:
Der `MCRMODSLoopValidator` ist in allen drei Versionen identisch. Die begrenzte
Übernahme verknüpfter Metadaten hebt die Kreisprüfung beim Speichern nicht auf.

- `MCRMetadataManager.update` normalisiert und validiert vor dem Update-Event.
- Die MODS-Standardkonfiguration aktiviert `MCR.Metadata.Validator.mods.loop.class`.
- Der Validator folgt den von `LINKED_RELATED_ITEMS` erfassten direkten Verweisen
  rekursiv durch die gespeicherten Objekte. Er prüft gegen die ID des zu speichernden
  Objekts; es gibt keine Begrenzung auf eine Ebene oder zwei Dokumente.
- Damit werden sowohl `A → B → A` als auch `A → B → C → A` abgewiesen.
- Bei `D → A → B → A` fehlt im Validator eine Menge bereits besuchter IDs.
  Aus dem Quellcode ergibt sich deshalb eine mögliche endlose Rekursion bis zum
  `StackOverflowError`, wenn dieser Pfad erreicht wird. Das ist hier eine
  Quellcodeanalyse, kein reproduzierter Lauf auf einer 2025.12-Installation.
- Der Expander übernimmt gewöhnliche verknüpfte Metadaten aus normalisierten
  Zielobjekten und leert deren verknüpfte `relatedItem`-Inhalte. Strukturelle Eltern
  werden hingegen ihrerseits expandiert; für sie gilt keine allgemeine Ein-Ebenen-Regel.

Ein Upgrade allein macht vorhandene Kreise daher nicht speicherbar. Die tatsächliche
Anwendung kann Validatoren und Beziehungstypen abweichend konfigurieren. Diese
Prüfung ändert weder die Projektversion noch die Erkennungsregeln der Übersicht.

Quellen: [Migration 2025.12](https://www.mycore.de/documentation/migrate/migrate_mcr2025_12/),
[LoopValidator](https://github.com/MyCoRe-Org/mycore/blob/v2025.12.2/mycore-mods/src/main/java/org/mycore/mods/MCRMODSLoopValidator.java),
[Standardkonfiguration](https://github.com/MyCoRe-Org/mycore/blob/v2025.12.2/mycore-mods/src/main/resources/components/mods/config/mycore.properties),
[MetadataManager](https://github.com/MyCoRe-Org/mycore/blob/v2025.12.2/mycore-base/src/main/java/org/mycore/datamodel/metadata/MCRMetadataManager.java),
[Expander](https://github.com/MyCoRe-Org/mycore/blob/v2025.12.2/mycore-mods/src/main/java/org/mycore/mods/MCRMODSExpander.java).

