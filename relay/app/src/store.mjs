import { sha256 } from './security.mjs';

function nowIso() { return new Date().toISOString(); }
function futureIso(seconds) { return new Date(Date.now() + seconds * 1000).toISOString(); }

export class MemoryStore {
  constructor() {
    this.users = new Map();
    this.clients = new Map();
    this.authCodes = new Map();
    this.accessTokens = new Map();
    this.refreshTokens = new Map();
    this.pairings = new Map();
    this.devices = new Map();
  }
  async init() {}
  async upsertBootstrapUser({ userId, username, passwordHash }) {
    const existing = [...this.users.values()].find(u => u.username === username);
    const row = { user_id: existing?.user_id || userId, username, password_hash: passwordHash, created_at: existing?.created_at || nowIso() };
    this.users.set(row.user_id, row); return row;
  }
  async getUserByUsername(username) { return [...this.users.values()].find(u => u.username === username) || null; }
  async getUserById(userId) { return this.users.get(userId) || null; }
  async createUser({userId,username,passwordHash}) { if(await this.getUserByUsername(username)) return null; const row={user_id:userId,username,password_hash:passwordHash,created_at:nowIso()}; this.users.set(userId,row); return row; }
  async createOAuthClient(row) { this.clients.set(row.client_id, row); return row; }
  async getOAuthClient(clientId) { return this.clients.get(clientId) || null; }
  async saveAuthCode(row) { this.authCodes.set(row.code_hash, row); }
  async consumeAuthCode(hash) {
    const row = this.authCodes.get(hash); if (!row || row.used_at) return null;
    row.used_at = nowIso(); return row;
  }
  async saveAccessToken(row) { this.accessTokens.set(row.token_hash, row); }
  async getAccessToken(hash) {
    const r = this.accessTokens.get(hash); if (!r || Date.parse(r.expires_at) <= Date.now() || r.revoked_at) return null; return r;
  }
  async saveRefreshToken(row) { this.refreshTokens.set(row.token_hash, row); }
  async consumeRefreshToken(hash) {
    const r = this.refreshTokens.get(hash); if (!r || Date.parse(r.expires_at) <= Date.now() || r.revoked_at) return null;
    r.revoked_at = nowIso(); return r;
  }
  async createPairing(row) { this.pairings.set(row.pairing_id, row); return row; }
  async getPairingByCode(code) { return [...this.pairings.values()].find(p => p.pair_code === code) || null; }
  async getPairing(id) { return this.pairings.get(id) || null; }
  async claimPairing(id, patch) { const p = this.pairings.get(id); if (!p) return null; Object.assign(p, patch); return p; }
  async markPairDelivered(id) { const p=this.pairings.get(id); if(p){p.delivered_at=nowIso(); p.issued_token_enc=null;} }
  async upsertDevice(row) {
    const prior=this.devices.get(row.device_id) || {};
    const merged={...prior,...row,created_at:prior.created_at||row.created_at||nowIso()};
    this.devices.set(row.device_id, merged); return merged;
  }
  async getDeviceByToken(tokenHash) { return [...this.devices.values()].find(d => d.token_hash === tokenHash && !d.revoked_at) || null; }
  async getDevice(userId, deviceId) { const d=this.devices.get(deviceId); return d && d.user_id===userId ? d : null; }
  async listDevices(userId) { return [...this.devices.values()].filter(d => d.user_id===userId).sort((a,b)=>String(b.last_seen||'').localeCompare(String(a.last_seen||''))); }
  async setDeviceConnection(deviceId, {connected, lastSeen, sessionId, capabilities, metadata}) {
    const d=this.devices.get(deviceId); if(!d) return;
    Object.assign(d,{connected,last_seen:lastSeen||nowIso(),session_id:sessionId??d.session_id});
    if(capabilities!==undefined) d.capabilities=capabilities;
    if(metadata) Object.assign(d, metadata);
  }
  async revokeDevice(userId, deviceId) { const d=await this.getDevice(userId,deviceId); if(!d)return false; d.revoked_at=nowIso(); d.connected=false; return true; }
  async rotateDeviceToken(deviceId, tokenHash) { const d=this.devices.get(deviceId); if(!d||d.revoked_at)return null; d.token_hash=tokenHash; d.token_version=(d.token_version||1)+1; return d; }
}

export class PostgresStore {
  constructor(url) {
    this.url=url;
    this.sql=null;
  }
  async init() {
    const mod=await import('postgres');
    const postgres=mod.default;
    const ssl=process.env.PGSSLMODE==='disable' ? false : 'require';
    this.sql=postgres(this.url,{max:10,ssl,prepare:false,idle_timeout:20,connect_timeout:20});
    const statements=[
      `CREATE TABLE IF NOT EXISTS bridge_users (user_id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())`,
      `CREATE TABLE IF NOT EXISTS oauth_clients (client_id TEXT PRIMARY KEY, client_name TEXT, redirect_uris JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())`,
      `CREATE TABLE IF NOT EXISTS oauth_auth_codes (code_hash TEXT PRIMARY KEY, client_id TEXT NOT NULL, user_id TEXT NOT NULL, redirect_uri TEXT NOT NULL, code_challenge TEXT NOT NULL, resource TEXT, expires_at TIMESTAMPTZ NOT NULL, used_at TIMESTAMPTZ)`,
      `CREATE TABLE IF NOT EXISTS oauth_access_tokens (token_hash TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT, resource TEXT, expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ)`,
      `CREATE TABLE IF NOT EXISTS oauth_refresh_tokens (token_hash TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT, resource TEXT, expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ)`,
      `ALTER TABLE oauth_auth_codes ADD COLUMN IF NOT EXISTS resource TEXT`,
      `ALTER TABLE oauth_access_tokens ADD COLUMN IF NOT EXISTS resource TEXT`,
      `ALTER TABLE oauth_refresh_tokens ADD COLUMN IF NOT EXISTS resource TEXT`,
      `CREATE TABLE IF NOT EXISTS pairings (pairing_id TEXT PRIMARY KEY, pair_code TEXT UNIQUE NOT NULL, nonce_hash TEXT NOT NULL, device_id TEXT NOT NULL, metadata JSONB NOT NULL DEFAULT '{}'::jsonb, created_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL, claimed_user_id TEXT, claimed_at TIMESTAMPTZ, issued_token_enc TEXT, delivered_at TIMESTAMPTZ)`,
      `CREATE TABLE IF NOT EXISTS devices (device_id TEXT PRIMARY KEY, user_id TEXT NOT NULL, device_name TEXT NOT NULL, device_type TEXT, platform TEXT, app_version TEXT, token_hash TEXT UNIQUE NOT NULL, token_version INTEGER NOT NULL DEFAULT 1, connected BOOLEAN NOT NULL DEFAULT false, last_seen TIMESTAMPTZ, session_id TEXT, capabilities JSONB NOT NULL DEFAULT '[]'::jsonb, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), revoked_at TIMESTAMPTZ)`,
      `CREATE INDEX IF NOT EXISTS devices_user_idx ON devices(user_id)`,
      `CREATE INDEX IF NOT EXISTS devices_token_idx ON devices(token_hash)`
    ];
    for(const q of statements) await this.sql.unsafe(q);
  }
  async upsertBootstrapUser({userId,username,passwordHash}) { const r=await this.sql`INSERT INTO bridge_users(user_id,username,password_hash) VALUES(${userId},${username},${passwordHash}) ON CONFLICT(username) DO UPDATE SET password_hash=EXCLUDED.password_hash RETURNING *`; return r[0]; }
  async getUserByUsername(username){const r=await this.sql`SELECT * FROM bridge_users WHERE username=${username}`;return r[0]||null;}
  async getUserById(id){const r=await this.sql`SELECT * FROM bridge_users WHERE user_id=${id}`;return r[0]||null;}
  async createUser({userId,username,passwordHash}){try{const r=await this.sql`INSERT INTO bridge_users(user_id,username,password_hash) VALUES(${userId},${username},${passwordHash}) RETURNING *`;return r[0]||null;}catch(e){if(e?.code==='23505')return null;throw e;}}
  async createOAuthClient(r){const x=await this.sql`INSERT INTO oauth_clients(client_id,client_name,redirect_uris) VALUES(${r.client_id},${r.client_name||null},${this.sql.json(r.redirect_uris)}) RETURNING *`;return x[0];}
  async getOAuthClient(id){const r=await this.sql`SELECT * FROM oauth_clients WHERE client_id=${id}`;return r[0]||null;}
  async saveAuthCode(r){await this.sql`INSERT INTO oauth_auth_codes(code_hash,client_id,user_id,redirect_uri,code_challenge,resource,expires_at) VALUES(${r.code_hash},${r.client_id},${r.user_id},${r.redirect_uri},${r.code_challenge},${r.resource||null},${r.expires_at})`;}
  async consumeAuthCode(hash){const r=await this.sql`UPDATE oauth_auth_codes SET used_at=now() WHERE code_hash=${hash} AND used_at IS NULL RETURNING *`;return r[0]||null;}
  async saveAccessToken(r){await this.sql`INSERT INTO oauth_access_tokens(token_hash,user_id,client_id,resource,expires_at) VALUES(${r.token_hash},${r.user_id},${r.client_id||null},${r.resource||null},${r.expires_at})`;}
  async getAccessToken(hash){const r=await this.sql`SELECT * FROM oauth_access_tokens WHERE token_hash=${hash} AND revoked_at IS NULL AND expires_at>now()`;return r[0]||null;}
  async saveRefreshToken(r){await this.sql`INSERT INTO oauth_refresh_tokens(token_hash,user_id,client_id,resource,expires_at) VALUES(${r.token_hash},${r.user_id},${r.client_id||null},${r.resource||null},${r.expires_at})`;}
  async consumeRefreshToken(hash){const r=await this.sql`UPDATE oauth_refresh_tokens SET revoked_at=now() WHERE token_hash=${hash} AND revoked_at IS NULL AND expires_at>now() RETURNING *`;return r[0]||null;}
  async createPairing(r){const x=await this.sql`INSERT INTO pairings(pairing_id,pair_code,nonce_hash,device_id,metadata,created_at,expires_at) VALUES(${r.pairing_id},${r.pair_code},${r.nonce_hash},${r.device_id},${this.sql.json(r.metadata||{})},${r.created_at},${r.expires_at}) RETURNING *`;return x[0];}
  async getPairingByCode(code){const r=await this.sql`SELECT * FROM pairings WHERE pair_code=${code}`;return r[0]||null;}
  async getPairing(id){const r=await this.sql`SELECT * FROM pairings WHERE pairing_id=${id}`;return r[0]||null;}
  async claimPairing(id,p){const r=await this.sql`UPDATE pairings SET claimed_user_id=${p.claimed_user_id},claimed_at=${p.claimed_at},issued_token_enc=${p.issued_token_enc} WHERE pairing_id=${id} RETURNING *`;return r[0]||null;}
  async markPairDelivered(id){await this.sql`UPDATE pairings SET delivered_at=now(),issued_token_enc=NULL WHERE pairing_id=${id}`;}
  async upsertDevice(d){const r=await this.sql`INSERT INTO devices(device_id,user_id,device_name,device_type,platform,app_version,token_hash,token_version,connected,last_seen,session_id,capabilities,revoked_at) VALUES(${d.device_id},${d.user_id},${d.device_name},${d.device_type||null},${d.platform||null},${d.app_version||null},${d.token_hash},${d.token_version||1},${!!d.connected},${d.last_seen||null},${d.session_id||null},${this.sql.json(d.capabilities||[])},${d.revoked_at||null}) ON CONFLICT(device_id) DO UPDATE SET user_id=EXCLUDED.user_id,device_name=EXCLUDED.device_name,device_type=EXCLUDED.device_type,platform=EXCLUDED.platform,app_version=EXCLUDED.app_version,token_hash=EXCLUDED.token_hash,token_version=EXCLUDED.token_version,revoked_at=NULL RETURNING *`;return r[0];}
  async getDeviceByToken(hash){const r=await this.sql`SELECT * FROM devices WHERE token_hash=${hash} AND revoked_at IS NULL`;return r[0]||null;}
  async getDevice(user,id){const r=await this.sql`SELECT * FROM devices WHERE user_id=${user} AND device_id=${id}`;return r[0]||null;}
  async listDevices(user){return await this.sql`SELECT * FROM devices WHERE user_id=${user} ORDER BY last_seen DESC NULLS LAST,created_at DESC`;}
  async setDeviceConnection(id,{connected,lastSeen,sessionId,capabilities,metadata}){const caps=capabilities===undefined?null:this.sql.json(capabilities);await this.sql`UPDATE devices SET connected=${!!connected},last_seen=${lastSeen||nowIso()},session_id=${sessionId||null},capabilities=COALESCE(${caps},capabilities),device_name=COALESCE(${metadata?.device_name||null},device_name),platform=COALESCE(${metadata?.platform||null},platform),app_version=COALESCE(${metadata?.app_version||null},app_version) WHERE device_id=${id}`;}
  async revokeDevice(user,id){const r=await this.sql`UPDATE devices SET revoked_at=now(),connected=false WHERE user_id=${user} AND device_id=${id} RETURNING device_id`;return !!r[0];}
  async rotateDeviceToken(id,hash){const r=await this.sql`UPDATE devices SET token_hash=${hash},token_version=token_version+1 WHERE device_id=${id} AND revoked_at IS NULL RETURNING *`;return r[0]||null;}
}

export function createStore() { return process.env.DATABASE_URL ? new PostgresStore(process.env.DATABASE_URL) : new MemoryStore(); }
export { futureIso, nowIso, sha256 };
