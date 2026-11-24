import { ensureTrailingSlash } from './helper';

const DEFAULT_SOLR_QUERY = 'objectType:mods AND state:published';

export async function getDocumentCount(baseUrl: string, query?: string) {
  const response = await fetch(
    `${ensureTrailingSlash(baseUrl)}api/v1/search?q=${query ? query : DEFAULT_SOLR_QUERY}&rows=0&wt=json`
  );
  if (!response.ok) {
    throw new Error(`HTTP error ${response.status}`);
  }
  const data = await response.json();
  return data?.response?.numFound;
}
