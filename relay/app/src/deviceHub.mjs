import crypto from 'node:crypto';

function err(code, message, details) {
  const e = new Error(message); e.code = code; if (details) e.details = details; return e;
}

export class DeviceHub {
  constructor(store, {requestTimeoutMs=180000,reconnectGraceMs=20000}={}) {
    this.store=store;
    this.requestTimeoutMs=requestTimeoutMs;
    this.reconnectGraceMs=reconnectGraceMs;
    this.connections=new Map();
    this.pending=new Map();
  }
  async register(device, ws, hello={}) {
    const key=device.device_id;
    const old=this.connections.get(key);
    if(old?.ws && old.ws!==ws){ try{old.ws.close(4001,'replaced');}catch{} }
    const sessionId=crypto.randomUUID();
    const conn={device,ws,sessionId,lastSeen:Date.now(),closed:false};
    this.connections.set(key,conn);
    await this.store.setDeviceConnection(key,{connected:true,lastSeen:new Date().toISOString(),sessionId,capabilities:hello.capabilities||device.capabilities||[],metadata:{device_name:hello.device_name,platform:hello.platform,app_version:hello.app_version}});
    ws.send(JSON.stringify({type:'welcome',session_id:sessionId,server_time:new Date().toISOString(),heartbeat_interval_ms:15000}));
    for(const p of this.pending.values()){
      if(p.deviceId===key && !p.settled){
        p.sessionId=sessionId;
        try{ws.send(JSON.stringify({...p.message,session_id:sessionId,retry:true}));}catch{}
      }
    }
    return conn;
  }
  async touch(deviceId){ const c=this.connections.get(deviceId); if(c)c.lastSeen=Date.now(); await this.store.setDeviceConnection(deviceId,{connected:true,lastSeen:new Date().toISOString(),sessionId:c?.sessionId||null}); }
  async disconnect(deviceId, ws){
    const c=this.connections.get(deviceId); if(!c||c.ws!==ws)return;
    c.closed=true; this.connections.delete(deviceId);
    await this.store.setDeviceConnection(deviceId,{connected:false,lastSeen:new Date().toISOString(),sessionId:null});
    setTimeout(()=>{
      if(this.connections.has(deviceId)) return;
      for(const [id,p] of this.pending){
        if(p.deviceId===deviceId&&!p.settled){ p.settled=true; clearTimeout(p.timer); p.reject(err('DEVICE_OFFLINE','Device disconnected before the request completed.')); this.pending.delete(id); }
      }
    },this.reconnectGraceMs).unref?.();
  }
  async pickDevice(userId, deviceId){
    if(deviceId){ const d=await this.store.getDevice(userId,deviceId); if(!d)throw err('DEVICE_NOT_FOUND','Device not found for this user.'); return d; }
    const all=await this.store.listDevices(userId);
    const online=all.filter(d=>this.connections.has(d.device_id));
    if(online.length===1)return online[0];
    if(online.length===0)throw err('DEVICE_OFFLINE','No connected Termux device is online.');
    throw err('DEVICE_REQUIRED','Multiple devices are online. Specify device_id.',{devices:online.map(d=>({device_id:d.device_id,device_name:d.device_name}))});
  }
  async route(userId, tool, args={}, {deviceId,timeoutMs}={}){
    const device=await this.pickDevice(userId,deviceId);
    const conn=this.connections.get(device.device_id);
    if(!conn)throw err('DEVICE_OFFLINE','Selected device is offline.');
    const requestId=`req_${crypto.randomUUID()}`;
    const message={type:'tool_request',request_id:requestId,tool,arguments:args,created_at:new Date().toISOString(),session_id:conn.sessionId};
    const limit=Math.max(1000,Math.min(timeoutMs||this.requestTimeoutMs,300000));
    return new Promise((resolve,reject)=>{
      const p={requestId,deviceId:device.device_id,userId,tool,message,sessionId:conn.sessionId,resolve,reject,settled:false,timer:null};
      p.timer=setTimeout(()=>{ if(p.settled)return; p.settled=true; this.pending.delete(requestId); reject(err('REQUEST_TIMEOUT',`Device request timed out after ${limit} ms.`)); },limit);
      this.pending.set(requestId,p);
      try{conn.ws.send(JSON.stringify(message));}catch(e){clearTimeout(p.timer);this.pending.delete(requestId);reject(err('SEND_FAILED',e.message));}
    });
  }
  handleMessage(deviceId, ws, msg){
    const conn=this.connections.get(deviceId); if(!conn||conn.ws!==ws)return;
    conn.lastSeen=Date.now();
    if(msg.type==='heartbeat') { this.touch(deviceId).catch(()=>{}); return; }
    if(msg.type!=='tool_response'||!msg.request_id)return;
    const p=this.pending.get(msg.request_id); if(!p||p.deviceId!==deviceId||p.settled)return;
    p.settled=true; clearTimeout(p.timer); this.pending.delete(msg.request_id);
    if(msg.success) p.resolve({request_id:msg.request_id,device_id:deviceId,result:msg.result});
    else p.reject(err(msg.error?.code||'DEVICE_ERROR',msg.error?.message||'Device tool failed.',msg.error));
  }

  disconnectNow(deviceId, code=4004, reason='device revoked'){
    const c=this.connections.get(deviceId);
    if(c?.ws){try{c.ws.close(code,reason)}catch{try{c.ws.terminate?.()}catch{}}}
  }
  onlineSnapshot(userId){
    return [...this.connections.values()].filter(c=>c.device.user_id===userId).map(c=>({device_id:c.device.device_id,device_name:c.device.device_name,last_seen:new Date(c.lastSeen).toISOString()}));
  }
}
