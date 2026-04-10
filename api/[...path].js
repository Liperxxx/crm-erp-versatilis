const { Readable } = require('node:stream');

const HOP_BY_HOP_HEADERS = new Set([
  'connection',
  'content-length',
  'host',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailer',
  'transfer-encoding',
  'upgrade',
]);
const REQUEST_URL_BASE = 'http://localhost';
let cachedBackendBaseUrl;

function resolveBackendBaseUrl() {
  const raw = process.env.BACKEND_API_BASE_URL || '';

  if (!raw) {
    throw new Error('BACKEND_API_BASE_URL não foi configurada no projeto Vercel.');
  }

  let parsed;
  try {
    parsed = new URL(raw);
  } catch {
    throw new Error('BACKEND_API_BASE_URL deve ser uma URL absoluta válida.');
  }

  if (!['http:', 'https:'].includes(parsed.protocol)) {
    throw new Error('BACKEND_API_BASE_URL deve usar http ou https.');
  }

  parsed.pathname = parsed.pathname.replace(/\/+$/, '');
  if (parsed.pathname === '/api') {
    parsed.pathname = '';
  }
  parsed.search = '';
  parsed.hash = '';

  return parsed.toString().replace(/\/$/, '');
}

function buildTargetUrl(req, backendBaseUrl) {
  const requestUrl = new URL(req.url, REQUEST_URL_BASE);
  return backendBaseUrl + requestUrl.pathname + requestUrl.search;
}

function buildForwardHeaders(req) {
  const headers = new Headers();

  for (const [key, value] of Object.entries(req.headers || {})) {
    const lowerKey = key.toLowerCase();
    if (HOP_BY_HOP_HEADERS.has(lowerKey) || value === null || value === undefined) {
      continue;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => headers.append(key, item));
      continue;
    }

    headers.set(key, value);
  }

  return headers;
}

function readRequestBody(req) {
  if (req.method === 'GET' || req.method === 'HEAD') {
    return undefined;
  }

  if (req.body === null || req.body === undefined) {
    return undefined;
  }

  if (Buffer.isBuffer(req.body) || typeof req.body === 'string') {
    return req.body;
  }

  return JSON.stringify(req.body);
}

module.exports = async function handler(req, res) {
  try {
    cachedBackendBaseUrl ||= resolveBackendBaseUrl();
  } catch (error) {
    res.status(500).json({ message: error.message });
    return;
  }

  const targetUrl = buildTargetUrl(req, cachedBackendBaseUrl);

  try {
    const upstream = await fetch(targetUrl, {
      method: req.method,
      headers: buildForwardHeaders(req),
      body: readRequestBody(req),
      redirect: 'manual',
    });

    res.status(upstream.status);

    upstream.headers.forEach((value, key) => {
      if (!HOP_BY_HOP_HEADERS.has(key.toLowerCase())) {
        res.setHeader(key, value);
      }
    });

    if (req.method === 'HEAD' || upstream.status === 204 || upstream.status === 304) {
      res.end();
      return;
    }

    if (!upstream.body) {
      res.end();
      return;
    }

    Readable.fromWeb(upstream.body).pipe(res);
  } catch (error) {
    res.status(502).json({
      message: 'Não foi possível encaminhar a requisição para o backend.',
      details: error instanceof Error ? error.message : 'Erro desconhecido',
    });
  }
};
