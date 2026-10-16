import { ensureTrailingSlash } from './helper';

const I18N_BASE_PATH = 'rsc/locale/translate/';

export async function getTranslation(
  baseUrl: string,
  name: string,
  locale?: string
) {
  const response = await fetch(
    `${ensureTrailingSlash(baseUrl)}${I18N_BASE_PATH}${locale ? `${locale}/` : ''}${name}`
  );
  if (!response.ok) {
    throw new Error(`HTTP error ${response.status}`);
  }
  return await response.text();
}
