const OAUTH=[{type:'oauth2',scopes:[]}];
function tool(name,title,description,inputSchema,outputSchema,annotations,extra={}){
  return {name,title,description,inputSchema,outputSchema,annotations,securitySchemes:OAUTH,...extra};
}

const deviceSelector={device_id:{type:'string',description:'Optional stable ID of the paired device to use when more than one device is online.'}};

export const MCP_TOOLS=[
  tool(
    'termux_profile','Bridge profile',
    'Returns the authenticated Termux Android Bridge account profile. Use it only to identify the connected account.',
    {type:'object',properties:{},additionalProperties:false},
    {type:'object',properties:{id:{type:'string'},name:{type:'string'},nickname:{type:'string'}},required:['id'],additionalProperties:false},
    {readOnlyHint:true,openWorldHint:false,destructiveHint:false},
    {_meta:{'openai/profile':true}}
  ),
  tool(
    'termux_status','Check Termux device status',
    'Checks whether a paired Termux Android device is online and returns its user-relevant runtime details. If multiple devices are online, pass device_id.',
    {type:'object',properties:{...deviceSelector},additionalProperties:false},
    {type:'object',properties:{connected:{type:'boolean'},device_id:{type:'string'},device_name:{type:'string'},platform:{type:'string'},app_version:{type:'string'},android_release:{type:'string'},arch:{type:'string'},last_seen:{type:'string'},capabilities:{type:'array',items:{type:'string'}},connected_devices:{type:'array',items:{type:'object'}},known_devices:{type:'array',items:{type:'object'}}},additionalProperties:true},
    {readOnlyHint:true,openWorldHint:false,destructiveHint:false}
  ),
  tool(
    'termux_run','Run a Termux command',
    'Runs a shell command explicitly requested by the user on the selected paired Termux device and returns the real stdout, stderr and exit status. This tool can modify device state depending on the command.',
    {type:'object',properties:{command:{type:'string',minLength:1,maxLength:12000,description:'Shell command to run in Termux.'},timeout_seconds:{type:'integer',minimum:1,maximum:300,description:'Maximum runtime before the device stops the command.'},...deviceSelector},required:['command'],additionalProperties:false},
    {type:'object',properties:{device_name:{type:'string'},stdout:{type:'string'},stderr:{type:'string'},exit_code:{type:['integer','null']},signal:{type:['string','null']},timed_out:{type:'boolean'},output_truncated:{type:'boolean'},duration_ms:{type:'integer'}},required:['stdout','stderr','exit_code','duration_ms'],additionalProperties:false},
    {readOnlyHint:false,openWorldHint:true,destructiveHint:true}
  ),
  tool(
    'termux_list_audits','List AuditAi reports',
    'Lists text reports stored in Android Documents/AuditAi on the selected paired device without modifying them.',
    {type:'object',properties:{...deviceSelector},additionalProperties:false},
    {type:'object',properties:{files:{type:'array',items:{type:'object',properties:{filename:{type:'string'},size_bytes:{type:'integer'},modified:{type:'string'}},required:['filename','size_bytes','modified'],additionalProperties:false}}},required:['files'],additionalProperties:false},
    {readOnlyHint:true,openWorldHint:false,destructiveHint:false}
  ),
  tool(
    'termux_read_audit','Read an AuditAi report',
    'Reads one named .txt report from Android Documents/AuditAi on the selected paired device.',
    {type:'object',properties:{filename:{type:'string',minLength:1,maxLength:160},...deviceSelector},required:['filename'],additionalProperties:false},
    {type:'object',properties:{filename:{type:'string'},content:{type:'string'}},required:['filename','content'],additionalProperties:false},
    {readOnlyHint:true,openWorldHint:false,destructiveHint:false}
  ),
  tool(
    'termux_save_audit','Save an AuditAi report',
    'Creates a new uniquely named .txt report in Android Documents/AuditAi on the selected paired device. Existing files are never overwritten.',
    {type:'object',properties:{title:{type:'string',minLength:1,maxLength:120},content:{type:'string',minLength:1,maxLength:200000},...deviceSelector},required:['title','content'],additionalProperties:false},
    {type:'object',properties:{saved:{type:'boolean'},filename:{type:'string'},bytes:{type:'integer'}},required:['saved','filename','bytes'],additionalProperties:false},
    {readOnlyHint:false,openWorldHint:false,destructiveHint:false}
  )
];
