function trimTrailingSlash(value: string): string {
  return value.replace(/\/+$/, '');
}

export function resolveApiBase(): string {
  const runtimeConfig = (globalThis as { __NEXBUY_CONFIG__?: { apiBase?: string } }).__NEXBUY_CONFIG__;
  const configuredApiBase = runtimeConfig?.apiBase?.trim();
  if (configuredApiBase) {
    return trimTrailingSlash(configuredApiBase);
  }

  const location = globalThis.location;
  if (location) {
    const isLocalFrontend = ['localhost', '127.0.0.1'].includes(location.hostname) && location.port === '4200';
    if (isLocalFrontend) {
      return 'http://localhost:8080';
    }
    return trimTrailingSlash(location.origin);
  }

  return 'http://localhost:8080';
}
