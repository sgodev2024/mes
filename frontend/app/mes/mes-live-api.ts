export class MesApiError extends Error {
  constructor(message:string, readonly code?:string, readonly status?:number){super(message);}
}

function accessToken(){
  if(typeof window==="undefined")return "";
  return window.localStorage.getItem("core-access-token")||window.sessionStorage.getItem("core-access-token")||"";
}

function refreshToken(){
  if(typeof window==="undefined")return "";
  return window.localStorage.getItem("core-refresh-token")||window.sessionStorage.getItem("core-refresh-token")||"";
}

function clearSession(){
  if(typeof window==="undefined")return;
  window.localStorage.removeItem("core-access-token");
  window.localStorage.removeItem("core-refresh-token");
  window.sessionStorage.removeItem("core-access-token");
  window.sessionStorage.removeItem("core-refresh-token");
}

type RefreshedSession={accessToken:string;refreshToken:string};
let sessionRefresh:Promise<boolean>|null=null;

async function refreshSession(apiUrl:string){
  if(sessionRefresh)return sessionRefresh;
  const token=refreshToken();
  if(!token)return false;
  const remembered=Boolean(window.localStorage.getItem("core-refresh-token"));
  sessionRefresh=fetch(`${apiUrl}/api/v1/auth/refresh`,{
    method:"POST",
    headers:{"Content-Type":"application/json"},
    body:JSON.stringify({refreshToken:token}),
    cache:"no-store",
  }).then(async response=>{
    if(!response.ok){clearSession();return false;}
    const session=await response.json() as RefreshedSession;
    const target=remembered?window.localStorage:window.sessionStorage;
    const other=remembered?window.sessionStorage:window.localStorage;
    other.removeItem("core-access-token");
    other.removeItem("core-refresh-token");
    target.setItem("core-access-token",session.accessToken);
    target.setItem("core-refresh-token",session.refreshToken);
    return true;
  }).catch(()=>{clearSession();return false;}).finally(()=>{sessionRefresh=null;});
  return sessionRefresh;
}

export async function mesApi<T>(apiUrl:string,path:string,init:RequestInit={}):Promise<T>{
  const request=()=>fetch(`${apiUrl}${path}`,{
    ...init,
    headers:{Authorization:`Bearer ${accessToken()}`,...(init.body?{"Content-Type":"application/json"}:{}),...init.headers},
    cache:"no-store",
  });
  let response=await request();
  if(response.status===401&&await refreshSession(apiUrl))response=await request();
  const body=await response.text();
  let parsed:any;
  if(body){
    try{parsed=JSON.parse(body);}catch{
      if(!response.ok)throw new MesApiError(body,"NON_JSON_RESPONSE",response.status);
      throw new MesApiError("Máy chủ trả về dữ liệu không đúng định dạng JSON.","INVALID_JSON_RESPONSE",response.status);
    }
  }
  if(!response.ok)throw new MesApiError(parsed?.detail||"Không tải được dữ liệu MES.",parsed?.code,response.status);
  return parsed as T;
}

export function viNumber(value:number|string|null|undefined,maximumFractionDigits=2){
  if(value===null||value===undefined||value==="")return "—";
  return Number(value).toLocaleString("vi-VN",{maximumFractionDigits});
}
