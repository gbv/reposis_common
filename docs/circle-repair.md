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

## CLI-Kommandos für einfache Kreise

Einfache Kreise lassen sich über die MyCoRe-CLI auflösen. Ein einfacher Kreis besteht aus
genau zwei Dokumenten mit genau einem direkten Verweis in jede Richtung, für den die Übersicht
einen Änderungsvorschlag macht (also nicht „Fachliche Prüfung nötig“). Entfernt wird genau der
in der Übersicht vorgeschlagene `relatedItem`. Alle anderen Kreise bleiben unverändert und
müssen weiterhin im Editor bearbeitet werden.

```text
repair simple circle for object {0}
repair all simple circles
```

`repair simple circle for object {0}` erwartet eine der beiden Objekt-IDs. Das Kommando liest das
Objekt und alle von ihm aus erreichbaren Objekte direkt aus dem gespeicherten XML, nutzt also nicht
den Verweisindex. Ist das Objekt nicht Teil eines einfachen Kreises, wird nur eine Meldung
protokolliert. Vor dem Speichern wird geprüft, ob der `relatedItem` an der geplanten Position noch
auf das erwartete Ziel mit der erwarteten Relation zeigt.

Beim Speichern verteilt MyCoRe die geteilten Metadaten des Objekts an seine Kinder und an alle
Objekte, die darauf verweisen, und von dort weiter. MyCoRe schreibt das Objekt, bevor es verteilt.
Scheitert die Verteilung an einem anderen Objekt mit Kreis, wird nur die Datenbanktransaktion
zurückgerollt. Die bereits geschriebenen XML-Dokumente bleiben geändert, Linkindex und Dokumente
passen dann nicht mehr zusammen. Deshalb simuliert das Kommando vor dem Speichern die gesamte
Verteilung im Speicher (`org.mycore.mods.MCRMODSShareCascadeSimulator`). Würde sie scheitern,
wird nichts geschrieben und eine Warnung mit dem blockierenden Objekt protokolliert. Dieser Kreis
muss zuerst aufgelöst werden.

Ein Sonderfall sind veraltete eingebettete Kopien: Kinder und verweisende Objekte speichern Kopien
der geteilten Metadaten. Sind diese entstanden, während der Kreis bestand, enthalten sie ihn noch.
MyCoRe prüft beim Weiterverteilen auf Kreise, bevor es die alte Kopie ersetzt, und bricht deshalb
ab. Solche Objekte erkennt die Simulation: Sie leert deren geteilte Metadaten und rechnet mit dem
geleerten Stand weiter. Gelingt die Simulation so, leert das Kommando die geteilten Metadaten dieser
Objekte wirklich, speichert sie ohne Weiterverteilung und repariert danach den Kreis. Die Reparatur
füllt die Kopien wieder auf. Parent und Verweise bleiben dabei unverändert. Ein echter Kreis
blockiert weiterhin, weil die Kopie in der Simulation aus den aktuellen Objekten neu aufgebaut wird.

Die Zahl der simulierten Speichervorgänge begrenzt
`MCR.CircleRepair.MaxCascadeUpdates`; größere Kaskaden werden nicht repariert.

`repair all simple circles` erzeugt die Liste der Kreisgruppen wie die Übersicht (Verweisindex
plus Prüfung der XML-Dokumente) und reiht für jeden einfachen Kreis das Einzelkommando ein.
Anschließend sollte der Verweisindex aktuell sein, damit die Übersicht das Ergebnis zeigt.
Die Kommandos sollten ohne gleichzeitige Bearbeitung der betroffenen Dokumente laufen.

## CLI-Kommandos für veraltete Kopien nach aufgelösten Kreisen

Wird ein Kreis auf anderem Weg aufgelöst, etwa im Editor, bleiben die eingebetteten Kopien aus der Zeit
des Kreises in anderen Objekten stehen. Der Verweisindex enthält den Kreis dann nicht mehr, deshalb
zeigt die Übersicht nichts an. Jedes Speichern eines solchen Objekts scheitert trotzdem mit
`Hierarchy of mods:relatedItem contains ciruit by object <ID>`.

```text
refresh shared metadata of object {0}
refresh all outdated shared metadata
```

`refresh shared metadata of object {0}` baut die geteilten Metadaten des Objekts neu auf und verteilt
sie weiter, auch wenn sich das Objekt selbst nicht ändert (wie `repair shared metadata for the ID {0}`).
Vorher wird die Verteilung wie bei der Kreisreparatur im Speicher simuliert. Veraltete Kopien in der
Kaskade werden geleert und ohne Weiterverteilung gespeichert. Würde die Simulation scheitern, wird nichts
geschrieben.

Aufgerufen wird das Kommando auf dem Objekt, das in der Fehlermeldung hinter „ciruit by object“ steht.
Dieses Objekt holt sich beim Neuaufbau die aktuelle Fassung seiner Ziele, die den Kreis nicht mehr
enthält, und verteilt sie an alle Objekte mit veralteter Kopie. Auf einem dieser Objekte selbst hilft
das Kommando nicht, weil es die veraltete Kopie wieder erben würde. Besteht der Kreis noch, scheitert
die Simulation; dann muss zuerst der Kreis aufgelöst werden.

`refresh all outdated shared metadata` prüft die geteilten Metadaten aller MODS-Objekte direkt im
gespeicherten XML, so wie MyCoRe beim Speichern prüft, und reiht für jedes Objekt, das dort einen
Kreis schließt, einmal das Einzelkommando ein. Die Prüfung liest jedes Objekt und dauert bei großen
Beständen einige Minuten.

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

