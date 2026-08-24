const MASKED_EMAIL_PATTERN = ' [at] ';

export function unmaskEmailLink(el: HTMLElement) {
  if (!el.textContent?.includes(MASKED_EMAIL_PATTERN)) {
    return;
  }
  const address = el.textContent.replace(MASKED_EMAIL_PATTERN, '@');
  const link = document.createElement('a');
  link.href = `mailto:${address}`;
  link.textContent = address;
  el.replaceWith(link);
}

export function omitEmptyFieldsOnSubmit(event: SubmitEvent) {
  const form = event.currentTarget as HTMLFormElement;
  const inputs = form.querySelectorAll<HTMLInputElement>('input');
  inputs.forEach(input => {
    if (!input.value) {
      input.dataset.nameBackup = input.name;
      input.removeAttribute('name');
    }
  });
  // Restore field names after the form is submitted
  // setTimeout ensures this runs after the submit event completes
  setTimeout(() => {
    inputs.forEach(input => {
      if (input.dataset.nameBackup) {
        input.name = input.dataset.nameBackup;
        delete input.dataset.nameBackup;
      }
    });
  }, 0);
}

export function removeOptions(
  select: HTMLSelectElement,
  values: string | string[]
) {
  const set = new Set(Array.isArray(values) ? values : [values]);

  Array.from(select.options).forEach(option => {
    if (set.has(option.value)) {
      option.remove();
    }
  });
}
