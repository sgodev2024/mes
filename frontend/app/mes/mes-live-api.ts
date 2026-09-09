export class MesApiError extends Error {
  constructor(message:string, readonly code?:string, readonly status?:number){super(message);}
}

function accessToken(){
  if(typeof window==="undefined")return "";
  return window.localStorage.getItem("core-access-token")||window.sessionStorage.getItem("core-access-token")||"";
}

export async function mesApi<T>(apiUrl:string,path:string,init:RequestInit={}):Promise<T>{
  const response=await fetch(`${apiUrl}${path}`,{
    ...init,
    headers:{Authorization:`Bearer ${accessToken()}`,...(init.body?{"Content-Type":"application/json"}:{}),...init.headers},
    cache:"no-store",
  });
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
