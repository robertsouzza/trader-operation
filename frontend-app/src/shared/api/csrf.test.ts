import { beforeEach, describe, expect, it, vi } from 'vitest';
import { garantirCsrf, lerTokenCsrf } from './csrf';

describe('csrf', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    vi.restoreAllMocks();
  });

  it('lerTokenCsrf devolve null quando o cookie não existe', () => {
    expect(lerTokenCsrf()).toBeNull();
  });

  it('lerTokenCsrf devolve o valor do cookie XSRF-TOKEN', () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/';
    expect(lerTokenCsrf()).toBe('abc123');
  });

  it('garantirCsrf não faz fetch quando o cookie já está presente', async () => {
    document.cookie = 'XSRF-TOKEN=existente; path=/';
    const fetchMock = vi.spyOn(window, 'fetch');
    const token = await garantirCsrf('http://localhost:8090');
    expect(token).toBe('existente');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('garantirCsrf dispara GET /api/auth/csrf quando falta o cookie', async () => {
    const fetchMock = vi.spyOn(window, 'fetch').mockImplementation(async () => {
      document.cookie = 'XSRF-TOKEN=novo; path=/';
      return new Response(null, { status: 204 });
    });
    const token = await garantirCsrf('http://localhost:8090');
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8090/api/auth/csrf',
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    );
    expect(token).toBe('novo');
  });
});
