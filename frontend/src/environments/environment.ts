import { resolveApiBase } from './runtime-config';

export const environment = {
  production: true,
  apiBase: resolveApiBase()
};
