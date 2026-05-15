const KEY = 'sar_credentials';

export interface Credentials { username: string; password: string }

export function saveCredentials(c: Credentials) {
  localStorage.setItem(KEY, JSON.stringify(c));
}

export function loadCredentials(): Credentials | null {
  const raw = localStorage.getItem(KEY);
  if (!raw) return null;
  try { return JSON.parse(raw) as Credentials; } catch { return null; }
}

export function clearCredentials() {
  localStorage.removeItem(KEY);
}

export function toBasicAuth(c: Credentials) {
  return 'Basic ' + btoa(`${c.username}:${c.password}`);
}
