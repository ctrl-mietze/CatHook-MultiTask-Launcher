import http from 'node:http';
import crypto from 'node:crypto';
import { WebSocketServer } from 'ws';
import { createStore, futureIso, nowIso } from './store.mjs';
import { hashPassword, verifyPassword, randomToken, randomCode, sha256, pkceS256, encryptString, decryptString, normalizeBaseUrl } from './security.mjs';
import { DeviceHub } from './deviceHub.mjs';
import { MCP_TOOLS } from './mcp.mjs';

const VERSION='0.3.3';
const PORT=Number(process.env.PORT||3000);
const baseUrl=normalizeBaseUrl(process.env.PUBLIC_BASE_URL||`http://127.0.0.1:${PORT}`);
const mcpResource=`${baseUrl}/mcp`;
const challengeToken=String(process.env.OPENAI_APPS_CHALLENGE||'').trim();
const serverSecret=process.env.SERVER_SECRET||crypto.randomBytes(32).toString('hex');
const bridgeUsername=process.env.BRIDGE_USERNAME||'raphael';
const bridgePassword=process.env.BRIDGE_PASSWORD||'change-me-now';
const reviewUsername=String(process.env.REVIEW_USERNAME||'').trim();
const reviewPassword=String(process.env.REVIEW_PASSWORD||'');
const accessTtl=Number(process.env.ACCESS_TOKEN_TTL_SECONDS||3600);
const refreshTtl=Number(process.env.REFRESH_TOKEN_TTL_SECONDS||2592000);
const pairTtl=Number(process.env.PAIRING_TTL_SECONDS||600);
const requestTimeoutMs=Number(process.env.REQUEST_TIMEOUT_SECONDS||180)*1000;
const reconnectGraceMs=Number(process.env.RECONNECT_GRACE_SECONDS||20)*1000;
if(process.env.NODE_ENV==='production'){
  if(!process.env.SERVER_SECRET){console.error('SERVER_SECRET is required in production');process.exit(2);}
  if(bridgePassword==='change-me-now'){console.error('BRIDGE_PASSWORD must be changed in production');process.exit(2);}
  if(!process.env.DATABASE_URL){console.error('DATABASE_URL is required in production');process.exit(2);}
}

const store=createStore(); await store.init();
const existing=await store.getUserByUsername(bridgeUsername);
const pwHash=existing && process.env.BRIDGE_PASSWORD_ROTATE!=='1' ? existing.password_hash : await hashPassword(bridgePassword);
await store.upsertBootstrapUser({userId:existing?.user_id||`usr_${crypto.randomUUID()}`,username:bridgeUsername,passwordHash:pwHash});
if(reviewUsername&&reviewPassword){const reviewer=await store.getUserByUsername(reviewUsername);const reviewHash=reviewer&&process.env.REVIEW_PASSWORD_ROTATE!=='1'?reviewer.password_hash:await hashPassword(reviewPassword);await store.upsertBootstrapUser({userId:reviewer?.user_id||`usr_${crypto.randomUUID()}`,username:reviewUsername,passwordHash:reviewHash});}
const hub=new DeviceHub(store,{requestTimeoutMs,reconnectGraceMs});

function sendJson(res,code,obj,headers={}){res.writeHead(code,{'content-type':'application/json; charset=utf-8','cache-control':'no-store','x-content-type-options':'nosniff','referrer-policy':'no-referrer',...headers});res.end(JSON.stringify(obj));}
function sendText(res,code,text,headers={}){res.writeHead(code,{'content-type':'text/plain; charset=utf-8','cache-control':'no-store',...headers});res.end(text);}
function h(s=''){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function sendHtml(res,code,title,body){const html=`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>${h(title)}</title><style>body{font-family:system-ui;background:#0b0d10;color:#eef2f5;max-width:680px;margin:48px auto;padding:0 18px}form{display:grid;gap:12px;background:#15191f;padding:20px;border-radius:16px}input,button{font:inherit;padding:12px;border-radius:10px;border:1px solid #343b45;background:#0f1318;color:#fff}button{cursor:pointer;background:#fff;color:#111;font-weight:700}.muted{color:#9da8b5}.code{font-size:2rem;letter-spacing:.16em;font-weight:800}</style></head><body>${body}</body></html>`;res.writeHead(code,{'content-type':'text/html; charset=utf-8','cache-control':'no-store'});res.end(html);}
async function body(req,limit=1024*1024){const chunks=[];let n=0;for await(const c of req){n+=c.length;if(n>limit)throw Object.assign(new Error('body too large'),{status:413});chunks.push(c);}return Buffer.concat(chunks).toString('utf8');}
async function jsonBody(req,limit){const raw=await body(req,limit);try{return raw?JSON.parse(raw):{}}catch{throw Object.assign(new Error('invalid json'),{status:400});}}
async function formBody(req,limit){return Object.fromEntries(new URLSearchParams(await body(req,limit)));}
function getBearer(req){const m=/^Bearer\s+(.+)$/i.exec(req.headers.authorization||'');return m?m[1].trim():null;}
async function requireUser(req,res){const t=getBearer(req);const challenge=`Bearer resource_metadata="${baseUrl}/.well-known/oauth-protected-resource/mcp"`;if(!t){sendJson(res,401,{error:'unauthorized'},{'www-authenticate':challenge});return null;}const row=await store.getAccessToken(sha256(t));if(!row||row.resource&&row.resource!==mcpResource){sendJson(res,401,{error:'invalid_token'},{'www-authenticate':challenge});return null;}return row.user_id;}
const rl=new Map();function rateLimit(req,key,limit,windowMs){const now=Date.now(),id=`${key}:${req.socket.remoteAddress||'unknown'}`;let x=rl.get(id);if(!x||now-x.start>windowMs)x={start:now,count:0};x.count++;rl.set(id,x);return x.count<=limit;}
async function issueTokens(userId,clientId,res,resource=mcpResource){const access=randomToken('atk_',32),refresh=randomToken('rtk_',40);await store.saveAccessToken({token_hash:sha256(access),user_id:userId,client_id:clientId,resource,expires_at:futureIso(accessTtl)});await store.saveRefreshToken({token_hash:sha256(refresh),user_id:userId,client_id:clientId,resource,expires_at:futureIso(refreshTtl)});sendJson(res,200,{access_token:access,token_type:'Bearer',expires_in:accessTtl,refresh_token:refresh});}
function toolResult(obj){return {content:[{type:'text',text:typeof obj==='string'?obj:JSON.stringify(obj,null,2)}],structuredContent:typeof obj==='object'&&obj?obj:{}};}

function publicPage(title,heading,body){return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>${h(title)}</title><style>body{font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#0b0d10;color:#eef2f5;max-width:820px;margin:0 auto;padding:48px 20px;line-height:1.55}a{color:#9dccff}nav{display:flex;gap:16px;flex-wrap:wrap;margin-bottom:32px}.card{background:#15191f;border:1px solid #2a313a;border-radius:18px;padding:24px;margin:18px 0}code{background:#11161c;padding:2px 6px;border-radius:6px}.muted{color:#9da8b5}h1,h2{line-height:1.15}</style></head><body><nav><a href="/">Home</a><a href="/support">Support</a><a href="/privacy">Privacy</a><a href="/terms">Terms</a></nav><h1>${h(heading)}</h1>${body}<p class="muted">Termux Android Remote Bridge ${VERSION}</p></body></html>`;}
function sendPublicPage(res,title,heading,body){res.writeHead(200,{'content-type':'text/html; charset=utf-8','cache-control':'public, max-age=300','x-content-type-options':'nosniff','referrer-policy':'no-referrer'});res.end(publicPage(title,heading,body));}
function profileResult(user){return {id:user.user_id,name:user.username,nickname:`${user.username} — Termux Android Bridge`};}
function cleanDeviceResult(name,result={}){if(name==='termux_run')return {device_name:result.device_name||undefined,stdout:String(result.stdout||''),stderr:String(result.stderr||''),exit_code:result.exit_code??null,signal:result.signal??null,timed_out:!!result.timed_out,output_truncated:!!result.output_truncated,duration_ms:Number(result.duration_ms||0)};if(name==='termux_list_audits')return {files:Array.isArray(result.files)?result.files:[]};if(name==='termux_read_audit')return {filename:String(result.filename||''),content:String(result.content||'')};if(name==='termux_save_audit')return {saved:!!result.saved,filename:String(result.filename||''),bytes:Number(result.bytes||0)};if(name==='termux_status'){const {audit_directory,node,...safe}=result;return safe;}return result;}

const server=http.createServer(async(req,res)=>{
  try{
    const url=new URL(req.url,baseUrl), p=url.pathname, method=req.method||'GET';
    if(method==='GET'&&p==='/health')return sendJson(res,200,{ok:true,service:'termux-android-remote-bridge',version:VERSION,time:new Date().toISOString()});
    if(method==='GET'&&p==='/')return sendPublicPage(res,'Termux Android Bridge','Termux Android Remote Bridge',`<div class="card"><p>Connect ChatGPT to your own paired Android Termux devices through a public MCP relay and a persistent authenticated outbound WebSocket connection.</p><p><strong>MCP:</strong> <code>/mcp</code><br><strong>Device transport:</strong> <code>/device</code></p><p><a href="/register">Create a Bridge account</a></p></div>`);
    if(method==='GET'&&p==='/support')return sendPublicPage(res,'Support — Termux Android Bridge','Support',`<div class="card"><p>For setup help, connection problems, privacy requests, or plugin-review questions, contact the developer through the project repository or the support contact listed in the ChatGPT plugin listing.</p><p>Useful checks: <code>termux-android-bridge doctor</code>, <code>termux-android-bridge status</code>, and <code>termux-android-bridge logs</code>.</p></div>`);
    if(method==='GET'&&p==='/privacy')return sendPublicPage(res,'Privacy — Termux Android Bridge','Privacy Policy',`<div class="card"><h2>Data processed</h2><p>The service processes account authentication data, paired-device metadata, connection state, commands explicitly sent through the plugin, and the command or AuditAi results returned by the paired device.</p><h2>Purpose</h2><p>Data is used only to authenticate the user, route requests to the correct paired device, return requested results, prevent abuse, and keep the service operational.</p><h2>Retention</h2><p>OAuth and pairing records are retained only as needed for authentication and revocation. Device records remain until revoked. Command stdout/stderr and AuditAi contents are relayed to the requesting ChatGPT session and are not intentionally persisted by the relay application. Operational hosting logs may be retained by the hosting provider according to its service settings.</p><h2>Recipients</h2><p>Data is processed by the relay hosting provider and database service solely to operate the bridge. Device tokens and OAuth tokens are treated as secrets and are not exposed in normal tool results.</p><h2>Controls</h2><p>Users can revoke a device with <code>termux-android-bridge logout</code> and can rotate the local device token. For privacy requests, use the support contact.</p></div>`);
    if(method==='GET'&&p==='/terms')return sendPublicPage(res,'Terms — Termux Android Bridge','Terms of Use',`<div class="card"><p>Termux Android Bridge is a remote-control developer utility for devices the user is authorized to control. Users are responsible for commands they request and for complying with applicable law, service terms, and device-owner policies.</p><p>The service is provided without a guarantee of uninterrupted availability. Do not use it to access devices or data without authorization.</p><p>Commands run with the permissions of the Termux client unless a separately implemented and explicitly authorized privileged path is used.</p></div>`);
    if(method==='GET'&&p==='/.well-known/openai-apps-challenge'){if(!challengeToken)return sendText(res,404,'not configured');res.writeHead(200,{'content-type':'text/plain; charset=utf-8','cache-control':'no-store'});return res.end(challengeToken);}
    if(method==='GET'&&(p==='/.well-known/oauth-protected-resource'||p==='/.well-known/oauth-protected-resource/mcp'))return sendJson(res,200,{resource:mcpResource,authorization_servers:[baseUrl],bearer_methods_supported:['header']});
    if(method==='GET'&&p==='/.well-known/oauth-authorization-server')return sendJson(res,200,{issuer:baseUrl,authorization_endpoint:`${baseUrl}/oauth/authorize`,token_endpoint:`${baseUrl}/oauth/token`,registration_endpoint:`${baseUrl}/oauth/register`,response_types_supported:['code'],grant_types_supported:['authorization_code','refresh_token'],code_challenge_methods_supported:['S256'],token_endpoint_auth_methods_supported:['none'],authorization_response_iss_parameter_supported:true});
    if(method==='POST'&&p==='/oauth/register'){
      if(!rateLimit(req,'oauth-register',30,60_000))return sendJson(res,429,{error:'rate_limited'});
      const b=await jsonBody(req,32*1024),uris=Array.isArray(b.redirect_uris)?b.redirect_uris.filter(x=>typeof x==='string'&&/^https?:\/\//.test(x)):[];
      if(!uris.length)return sendJson(res,400,{error:'invalid_redirect_uri'});
      const clientId=randomToken('client_',18);await store.createOAuthClient({client_id:clientId,client_name:String(b.client_name||'ChatGPT MCP Client').slice(0,120),redirect_uris:uris});return sendJson(res,201,{client_id:clientId,client_id_issued_at:Math.floor(Date.now()/1000),client_name:b.client_name||'ChatGPT MCP Client',redirect_uris:uris,grant_types:['authorization_code','refresh_token'],response_types:['code'],token_endpoint_auth_method:'none'});
    }
    if(method==='GET'&&p==='/register')return sendPublicPage(res,'Register — Termux Android Bridge','Create a Bridge account',`<div class="card"><p>Create an account used only to pair your own Termux devices and authorize ChatGPT access.</p><form method="post" action="/register"><input name="username" autocomplete="username" minlength="3" maxlength="40" placeholder="Username" required><input name="password" type="password" autocomplete="new-password" minlength="12" placeholder="Password (12+ characters)" required><input name="confirm" type="password" autocomplete="new-password" minlength="12" placeholder="Confirm password" required><label><input type="checkbox" name="accept" value="yes" required> I accept the <a href="/terms">Terms</a> and <a href="/privacy">Privacy Policy</a>.</label><button type="submit">Create account</button></form></div>`);
    if(method==='POST'&&p==='/register'){
      if(!rateLimit(req,'account-register',8,60_000))return sendHtml(res,429,'Try later','<h1>Too many registration attempts.</h1>');
      const b=await formBody(req,16*1024),username=String(b.username||'').trim(),password=String(b.password||''),confirm=String(b.confirm||'');
      if(!/^[A-Za-z0-9._-]{3,40}$/.test(username)||password.length<12||password.length>200||password!==confirm||b.accept!=='yes')return sendHtml(res,400,'Registration failed','<h1>Check the username, password and terms acceptance.</h1>');
      const passwordHash=await hashPassword(password),created=await store.createUser({userId:`usr_${crypto.randomUUID()}`,username,passwordHash});
      if(!created)return sendHtml(res,409,'Registration failed','<h1>That username is unavailable.</h1>');
      return sendHtml(res,201,'Account created','<h1>Account created ✅</h1><p>You can now pair your Termux device and use the same credentials when ChatGPT asks you to connect the plugin.</p><p><a href="/">Back to Termux Android Bridge</a></p>');
    }
    if(method==='GET'&&p==='/oauth/authorize'){
      const q=Object.fromEntries(url.searchParams),c=await store.getOAuthClient(q.client_id),redirects=c?.redirect_uris||[];
      const resource=q.resource||mcpResource;if(!c||!redirects.includes(q.redirect_uri)||q.response_type!=='code'||!q.code_challenge||q.code_challenge_method!=='S256'||resource!==mcpResource)return sendHtml(res,400,'OAuth error','<h1>Ungültige OAuth-Anfrage</h1>');
      const hidden=Object.entries({client_id:q.client_id,redirect_uri:q.redirect_uri,state:q.state||'',code_challenge:q.code_challenge,code_challenge_method:q.code_challenge_method,resource}).map(([k,v])=>`<input type="hidden" name="${k}" value="${h(v)}">`).join('');
      return sendHtml(res,200,'Termux Bridge Login',`<h1>Termux Android Bridge</h1><p class="muted">Melde dich an, damit ChatGPT auf deine verbundenen Geräte zugreifen darf.</p><form method="post" action="/oauth/authorize">${hidden}<input name="username" autocomplete="username" placeholder="Benutzername" required><input name="password" type="password" autocomplete="current-password" placeholder="Passwort" required><button type="submit">Verbinden</button></form><p class="muted">Noch kein Konto? <a href="/register">Bridge-Konto erstellen</a>.</p>`);
    }
    if(method==='POST'&&p==='/oauth/authorize'){
      const b=await formBody(req,32*1024),c=await store.getOAuthClient(b.client_id),u=await store.getUserByUsername(b.username);
      const resource=b.resource||mcpResource;if(!c||!(c.redirect_uris||[]).includes(b.redirect_uri)||resource!==mcpResource||!u||!(await verifyPassword(b.password||'',u.password_hash)))return sendHtml(res,401,'Login fehlgeschlagen','<h1>Login fehlgeschlagen</h1>');
      const code=randomToken('code_',28);await store.saveAuthCode({code_hash:sha256(code),client_id:b.client_id,user_id:u.user_id,redirect_uri:b.redirect_uri,code_challenge:b.code_challenge,resource,expires_at:futureIso(300)});
      const dest=new URL(b.redirect_uri);dest.searchParams.set('code',code);dest.searchParams.set('iss',baseUrl);if(b.state)dest.searchParams.set('state',b.state);res.writeHead(302,{location:dest.toString(),'cache-control':'no-store'});return res.end();
    }
    if(method==='POST'&&p==='/oauth/token'){
      const b=await formBody(req,32*1024);
      if(b.grant_type==='authorization_code'){
        const row=await store.consumeAuthCode(sha256(b.code||''));
        const resource=b.resource||row?.resource||mcpResource;if(!row||Date.parse(row.expires_at)<=Date.now()||row.client_id!==b.client_id||row.redirect_uri!==b.redirect_uri||resource!==mcpResource||row.resource&&row.resource!==resource||pkceS256(b.code_verifier||'')!==row.code_challenge)return sendJson(res,400,{error:'invalid_grant'});
        return issueTokens(row.user_id,row.client_id,res,resource);
      }
      if(b.grant_type==='refresh_token'){
        const row=await store.consumeRefreshToken(sha256(b.refresh_token||''));const resource=b.resource||row?.resource||mcpResource;if(!row||row.client_id!==b.client_id||resource!==mcpResource||row.resource&&row.resource!==resource)return sendJson(res,400,{error:'invalid_grant'});return issueTokens(row.user_id,row.client_id,res,resource);
      }
      return sendJson(res,400,{error:'unsupported_grant_type'});
    }
    if(method==='POST'&&p==='/api/pair/request'){
      if(!rateLimit(req,'pair-request',20,60_000))return sendJson(res,429,{error:'rate_limited'});
      const b=await jsonBody(req,32*1024),deviceId=String(b.device_id||'').trim();if(!/^[A-Za-z0-9._:-]{8,128}$/.test(deviceId))return sendJson(res,400,{error:'invalid_device_id'});
      const metadata={device_name:String(b.device_name||'Android Device').slice(0,120),device_type:String(b.device_type||'phone').slice(0,40),platform:String(b.platform||'android-termux').slice(0,80),app_version:String(b.app_version||'unknown').slice(0,40),capabilities:Array.isArray(b.capabilities)?b.capabilities.slice(0,50):[]};
      let code=randomCode(6);for(let i=0;i<8&&(await store.getPairingByCode(code));i++)code=randomCode(6);
      const pairingId=crypto.randomUUID(),nonce=randomToken('pair_',24);await store.createPairing({pairing_id:pairingId,pair_code:code,nonce_hash:sha256(nonce),device_id:deviceId,metadata,created_at:nowIso(),expires_at:futureIso(pairTtl)});
      return sendJson(res,200,{pairing_id:pairingId,pairing_nonce:nonce,pairing_code:code,claim_url:`${baseUrl}/pair?code=${encodeURIComponent(code)}`,expires_in:pairTtl});
    }
    if(method==='GET'&&p==='/pair'){
      const code=String(url.searchParams.get('code')||'').toUpperCase();return sendHtml(res,200,'Gerät koppeln',`<h1>Termux-Gerät koppeln</h1><p class="muted">Gib deinen Bridge-Login ein und bestätige den Code.</p><form method="post" action="/pair"><input name="code" value="${h(code)}" placeholder="Pairing-Code" required><input name="username" autocomplete="username" placeholder="Benutzername" required><input name="password" type="password" autocomplete="current-password" placeholder="Passwort" required><button type="submit">Gerät verbinden</button></form><p class="muted">Noch kein Konto? <a href="/register">Bridge-Konto erstellen</a>.</p>`);
    }
    if(method==='POST'&&p==='/pair'){
      const b=await formBody(req,32*1024),code=String(b.code||'').toUpperCase(),pair=await store.getPairingByCode(code);
      if(!pair||Date.parse(pair.expires_at)<=Date.now()||pair.claimed_at)return sendHtml(res,400,'Pairing fehlgeschlagen','<h1>Code ungültig oder abgelaufen.</h1>');
      const u=await store.getUserByUsername(String(b.username||''));if(!u||!(await verifyPassword(String(b.password||''),u.password_hash)))return sendHtml(res,401,'Login fehlgeschlagen','<h1>Login fehlgeschlagen.</h1>');
      const deviceToken=randomToken('dev_',40);await store.upsertDevice({device_id:pair.device_id,user_id:u.user_id,device_name:pair.metadata?.device_name||'Android Device',device_type:pair.metadata?.device_type||'phone',platform:pair.metadata?.platform||'android-termux',app_version:pair.metadata?.app_version||'unknown',token_hash:sha256(deviceToken),token_version:1,connected:false,capabilities:pair.metadata?.capabilities||[]});await store.claimPairing(pair.pairing_id,{claimed_user_id:u.user_id,claimed_at:nowIso(),issued_token_enc:encryptString(deviceToken,serverSecret)});return sendHtml(res,200,'Gerät verbunden',`<h1>Gerät verbunden ✅</h1><p>${h(pair.metadata?.device_name||pair.device_id)} darf sich jetzt verbinden.</p><p class="muted">Das Device-Token wird ausschließlich an den wartenden Termux-Client ausgeliefert.</p>`);
    }
    if(method==='POST'&&p==='/api/pair/status'){
      const b=await jsonBody(req,16*1024),pair=await store.getPairing(String(b.pairing_id||''));if(!pair||sha256(String(b.pairing_nonce||''))!==pair.nonce_hash)return sendJson(res,404,{error:'pairing_not_found'});if(Date.parse(pair.expires_at)<=Date.now())return sendJson(res,410,{status:'expired'});if(!pair.claimed_at)return sendJson(res,200,{status:'pending',expires_at:pair.expires_at});if(pair.delivered_at||!pair.issued_token_enc)return sendJson(res,410,{status:'already_delivered'});const token=decryptString(pair.issued_token_enc,serverSecret);await store.markPairDelivered(pair.pairing_id);return sendJson(res,200,{status:'paired',device_id:pair.device_id,device_token:token,user_id:pair.claimed_user_id});
    }
    if(method==='POST'&&p==='/api/device/token/rotate'){
      const t=getBearer(req);if(!t)return sendJson(res,401,{error:'unauthorized'});const d=await store.getDeviceByToken(sha256(t));if(!d)return sendJson(res,401,{error:'invalid_device_token'});const next=randomToken('dev_',40);await store.rotateDeviceToken(d.device_id,sha256(next));return sendJson(res,200,{device_token:next,device_id:d.device_id});
    }
    if(method==='POST'&&p==='/api/device/revoke-self'){
      const t=getBearer(req);if(!t)return sendJson(res,401,{error:'unauthorized'});const d=await store.getDeviceByToken(sha256(t));if(!d)return sendJson(res,401,{error:'invalid_device_token'});await store.revokeDevice(d.user_id,d.device_id);hub.disconnectNow(d.device_id);return sendJson(res,200,{revoked:true,device_id:d.device_id});
    }
    if(method==='GET'&&p==='/api/devices'){
      const userId=await requireUser(req,res);if(!userId)return;const ds=await store.listDevices(userId);return sendJson(res,200,{devices:ds.map(d=>({device_id:d.device_id,device_name:d.device_name,device_type:d.device_type,platform:d.platform,app_version:d.app_version,connected:!!d.connected,last_seen:d.last_seen,capabilities:d.capabilities,revoked:!!d.revoked_at}))});
    }
    const revoke=/^\/api\/devices\/([^/]+)\/revoke$/.exec(p);
    if(method==='POST'&&revoke){const userId=await requireUser(req,res);if(!userId)return;const id=decodeURIComponent(revoke[1]);const ok=await store.revokeDevice(userId,id);if(ok)hub.disconnectNow(id);return sendJson(res,ok?200:404,ok?{revoked:true,device_id:id}:{error:'device_not_found'});}
    if(p==='/mcp'){
      if(method!=='POST')return sendText(res,405,'MCP uses Streamable HTTP POST.\n',{allow:'POST'});
      if(!rateLimit(req,'mcp',240,60_000))return sendJson(res,429,{error:'rate_limited'});const userId=await requireUser(req,res);if(!userId)return;const msg=await jsonBody(req,1024*1024),base={jsonrpc:'2.0',id:msg.id??null};
      if(msg.method==='initialize')return sendJson(res,200,{...base,result:{protocolVersion:'2025-03-26',capabilities:{tools:{listChanged:false}},serverInfo:{name:'termux-android-remote-bridge',version:VERSION},instructions:'Use these tools only for the authenticated user’s paired Termux Android devices. Never fabricate device output. Read-only status and audit reads may be used for diagnostics; shell commands can change device state and must reflect the user’s request. Keep secrets and internal transport identifiers out of user-facing results.'}});
      if(msg.method==='notifications/initialized'){res.writeHead(202);return res.end();}
      if(msg.method==='ping')return sendJson(res,200,{...base,result:{}});
      if(msg.method==='tools/list')return sendJson(res,200,{...base,result:{tools:MCP_TOOLS}});
      if(msg.method==='tools/call'){
        const name=msg.params?.name,args=msg.params?.arguments||{};
        try{
          if(name==='termux_profile'){
            const user=await store.getUserById(userId);if(!user)throw Object.assign(new Error('Profile not found.'),{code:'PROFILE_NOT_FOUND'});return sendJson(res,200,{...base,result:toolResult(profileResult(user))});
          }
          if(name==='termux_status'){
            const ds=await store.listDevices(userId),online=hub.onlineSnapshot(userId);
            if(args.device_id){const d=await store.getDevice(userId,args.device_id);if(!d)throw Object.assign(new Error('Device not found.'),{code:'DEVICE_NOT_FOUND'});if(!online.some(x=>x.device_id===d.device_id))return sendJson(res,200,{...base,result:toolResult({connected:false,device_id:d.device_id,device_name:d.device_name,last_seen:d.last_seen,platform:d.platform,app_version:d.app_version})});const routed=await hub.route(userId,'termux_status',{}, {deviceId:d.device_id,timeoutMs:30000});return sendJson(res,200,{...base,result:toolResult({device_id:d.device_id,...cleanDeviceResult('termux_status',routed.result)})});}
            if(online.length===1){const d=await store.getDevice(userId,online[0].device_id),routed=await hub.route(userId,'termux_status',{}, {deviceId:online[0].device_id,timeoutMs:30000});return sendJson(res,200,{...base,result:toolResult({device_id:online[0].device_id,device_name:d?.device_name||online[0].device_name,...cleanDeviceResult('termux_status',routed.result)})});}
            return sendJson(res,200,{...base,result:toolResult({connected_devices:online,known_devices:ds.map(d=>({device_id:d.device_id,device_name:d.device_name,connected:!!d.connected,last_seen:d.last_seen}))})});
          }
          if(!MCP_TOOLS.some(t=>t.name===name))throw Object.assign(new Error(`Unknown tool: ${name}`),{code:'UNKNOWN_TOOL'});
          const deviceId=args.device_id,forwarded={...args};delete forwarded.device_id;const timeoutMs=name==='termux_run'?Math.min(310000,((forwarded.timeout_seconds||180)+10)*1000):60000;const routed=await hub.route(userId,name,forwarded,{deviceId,timeoutMs});const d=await store.getDevice(userId,routed.device_id);const clean=cleanDeviceResult(name,routed.result);if(name==='termux_run'&&d?.device_name)clean.device_name=d.device_name;return sendJson(res,200,{...base,result:toolResult(clean)});
        }catch(e){return sendJson(res,200,{...base,result:{content:[{type:'text',text:JSON.stringify({code:e.code||'RELAY_ERROR',message:e.message,details:e.details||null},null,2)}],isError:true}});}
      }
      return sendJson(res,200,{...base,error:{code:-32601,message:'Method not found'}});
    }
    return sendJson(res,404,{error:'not_found'});
  }catch(e){console.error('[http]',e.message);return sendJson(res,e.status||500,{error:e.status?'bad_request':'internal_error'});}
});

const wss=new WebSocketServer({noServer:true,maxPayload:1024*1024});
server.on('upgrade',(req,socket,head)=>{try{const u=new URL(req.url,baseUrl);if(u.pathname!=='/device'){socket.destroy();return;}wss.handleUpgrade(req,socket,head,ws=>wss.emit('connection',ws,req));}catch{socket.destroy();}});
wss.on('connection',(ws)=>{
  ws.isAlive=true;let registered=false,device=null;
  ws.on('pong',()=>{ws.isAlive=true;if(device)hub.touch(device.device_id).catch(()=>{});});
  ws.on('message',async data=>{let msg;try{msg=JSON.parse(data.toString())}catch{return}if(!registered){if(msg.type!=='auth'||!msg.device_token){ws.close(4003,'authentication required');return;}device=await store.getDeviceByToken(sha256(msg.device_token));if(!device){ws.close(4003,'invalid device token');return;}registered=true;await hub.register(device,ws,msg);return;}hub.handleMessage(device.device_id,ws,msg);});
  ws.on('close',()=>{if(device)hub.disconnect(device.device_id,ws).catch(()=>{});});ws.on('error',()=>{});
});
const ping=setInterval(()=>{for(const ws of wss.clients){if(ws.isAlive===false){try{ws.terminate()}catch{};continue;}ws.isAlive=false;try{ws.ping()}catch{}}},20000);ping.unref();
server.listen(PORT,'0.0.0.0',()=>console.log(`Termux Android Remote Bridge listening on ${PORT}; public base ${baseUrl}`));
