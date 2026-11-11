'use strict';
(() => {
  if (window.reposisCircleRepairInitialized) return;
  window.reposisCircleRepairInitialized = true;
  let busy = false;
  let limit = 20;
  const root = () => document.getElementById('circle-repair');
  const $ = id => root().querySelector('#' + id);
  const endpoint = new URL(window.location.href);
  endpoint.search = ''; endpoint.hash = '';

  function setBusy(value, message) {
    busy = value;
    root().querySelectorAll('button').forEach(button => {
      button.disabled = value;
    });
    $('search').disabled = value;
    $('filter').disabled = value;
    $('cases').setAttribute('aria-busy', String(value));
    if (message) $('status').textContent = message;
  }
  function notice(message) {
    $('notice').textContent = message;
    $('notice').className = 'alert alert-danger';
    $('notice').hidden = !message;
  }
  async function request(action) {
    const url = new URL(endpoint); url.searchParams.set('action', action);
    const options = { credentials: 'same-origin', cache: 'no-store' };
    const response = await fetch(url, options);
    const text = await response.text();
    if (!response.ok) {
      throw new Error(response.status === 401 || response.status === 403
        ? 'Bitte die Seite neu öffnen und sich mit einem berechtigten Konto anmelden.'
        : response.status === 409 ? 'Eine Kreisprüfung läuft bereits. Bitte anschließend erneut versuchen.'
          : 'Die Anfrage ist fehlgeschlagen. Bitte die Seite neu öffnen oder das Serverlog prüfen.');
    }
    return text;
  }
  async function load() {
    const search = $('search').value;
    const filter = $('filter').value;
    notice('');
    setBusy(true, 'Der Bestand wird erneut geprüft. Das kann bei großen Beständen einige Zeit dauern …');
    try {
      const html = await request('preview');
      const page = new DOMParser().parseFromString(html, 'text/html');
      const updated = page.getElementById('circle-repair');
      if (!updated) throw new Error('Die Antwort enthält keinen Prüfbericht. Bitte die Anmeldung prüfen.');
      root().replaceWith(document.importNode(updated, true));
      limit = 20;
      $('search').value = search; $('filter').value = filter;
      notice('');
      filterCases();
    } catch (error) {
      notice('Prüfung fehlgeschlagen: ' + error.message);
      $('status').textContent = 'Die angezeigten Vorschläge sind nicht aktuell. Bitte erneut prüfen.';
    } finally { setBusy(false); }
  }
  function filterCases() {
    const query = $('search').value.trim().toLowerCase();
    const filter = $('filter').value;
    let matches = 0;
    root().querySelectorAll('#cases > details').forEach(group => {
      const match = (!query || group.dataset.members.toLowerCase().includes(query)) &&
        (filter === 'all' || (group.dataset.suggested === 'true') === (filter === 'ready'));
      if (match) matches++;
      group.hidden = !match || matches > limit;
    });
    $('empty').hidden = matches !== 0;
    $('more').hidden = matches <= limit;
  }
  document.addEventListener('click', event => {
    const button = event.target.closest('#circle-repair button');
    if (!button || busy) return;
    if (button.id === 'refresh') load();
    else if (button.id === 'more') { limit += 20; filterCases(); }
  });
  document.addEventListener('input', event => {
    if (event.target === $('search')) { limit = 20; filterCases(); }
  });
  document.addEventListener('change', event => {
    if (event.target === $('filter')) { limit = 20; filterCases(); }
  });
  filterCases();
})();
