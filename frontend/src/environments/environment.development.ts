import { resolveApiBase } from './runtime-config';

export const environment = {
  production: false,
  apiBase: resolveApiBase()
};
