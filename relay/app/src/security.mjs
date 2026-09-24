import crypto from 'node:crypto';

export function randomToken(prefix = '', bytes = 32) {
  return prefix + crypto.randomBytes(bytes).toString('base64url');
}

export function randomCode(length = 6) {
  const alphabet = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
  let out = '';
  const bytes = crypto.randomBytes(length * 2);
  for (const b of bytes) {
    out += alphabet[b % alphabet.length];
    if (out.length >= length) break;
  }
  return out;
}

export function sha256(value) {
  return crypto.createHash('sha256').update(String(value)).digest('hex');
}

export function safeEqual(a, b) {
  const A = Buffer.from(String(a));
  const B = Buffer.from(String(b));
  if (A.length !== B.length) return false;
  return crypto.timingSafeEqual(A, B);
}

export async function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const key = await new Promise((resolve, reject) => {
    crypto.scrypt(password, salt, 64, (err, derived) => err ? reject(err) : resolve(derived));
  });
  return `scrypt$${salt.toString('base64url')}$${key.toString('base64url')}`;
}

export async function verifyPassword(password, encoded) {
  const [scheme, saltB64, keyB64] = String(encoded || '').split('$');
  if (scheme !== 'scrypt' || !saltB64 || !keyB64) return false;
  const salt = Buffer.from(saltB64, 'base64url');
  const expected = Buffer.from(keyB64, 'base64url');
  const actual = await new Promise((resolve, reject) => {
    crypto.scrypt(password, salt, expected.length, (err, derived) => err ? reject(err) : resolve(derived));
  });
  return expected.length === actual.length && crypto.timingSafeEqual(expected, actual);
}

function aesKey(secret) {
  return crypto.createHash('sha256').update(secret).digest();
}

export function encryptString(plain, secret) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', aesKey(secret), iv);
  const data = Buffer.concat([cipher.update(String(plain), 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return [iv, tag, data].map(x => x.toString('base64url')).join('.');
}

export function decryptString(encoded, secret) {
  const [ivB64, tagB64, dataB64] = String(encoded || '').split('.');
  if (!ivB64 || !tagB64 || !dataB64) throw new Error('Invalid encrypted payload');
  const decipher = crypto.createDecipheriv('aes-256-gcm', aesKey(secret), Buffer.from(ivB64, 'base64url'));
  decipher.setAuthTag(Buffer.from(tagB64, 'base64url'));
  return Buffer.concat([decipher.update(Buffer.from(dataB64, 'base64url')), decipher.final()]).toString('utf8');
}

export function pkceS256(verifier) {
  return crypto.createHash('sha256').update(verifier).digest('base64url');
}

export function normalizeBaseUrl(v) {
  return String(v || '').replace(/\/+$/, '');
}

export function bearer(req) {
  const raw = req.headers.authorization || '';
  const m = /^Bearer\s+(.+)$/i.exec(raw);
  return m ? m[1].trim() : null;
}
