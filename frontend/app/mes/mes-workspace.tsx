"use client";

import {FormEvent,ReactNode,useCallback,useEffect,useMemo,useState} from "react";
import {AppIcon} from "../components/app-icon";
import DailyReportingCenter from "./daily-reporting-center";
import MesExecutiveDashboard from "./mes-executive-dashboard";
import MesInventoryReconciliation from "./mes-inventory-reconciliation";
import MesMasterData from "./mes-master-data";
import {mesPrototypeDefinitions,type MesColumn,type MesDemoRow,type MesPrototypeDefinition} from "./mes-demo-data";
import {mesApi} from "./mes-live-api";

export type MesView =
  | "mes-dashboard" | "mes-planning-performance" | "mes-production-operations" | "mes-coal-flow"
  | "mes-quality-acceptance" | "mes-sales-logistics" | "mes-materials-equipment"
  | "mes-occupational-safety" | "mes-finance-accounting" | "mes-workforce-labor"
  | "mes-investment-projects" | "mes-science-digital" | "mes-alerts-directives" | "mes-esg"
  | "mes-reporting-center" | "mes-portal-tkv" | "mes-data-mapping" | "mes-integration-quality";

type OperationalView = Exclude<MesView,"mes-dashboard"|"mes-coal-flow"|"mes-reporting-center"|"mes-data-mapping">;
type Kpi={label:string;value:string;note:string;tone:string};
type Trend={date:string;value:number};
type Attention={id:string;title:string;detail:string;severity:string};
type OperationalRecord={id:string;code:string;title:string;tab:string;statusCode:string;statusLabel:string;organization:string;period:string;correlationKey:string;source:string;severity:string;version:number;data:MesDemoRow;actions:string[];updatedAt:string};
type Overview={module:string;tab:string;kpis:Kpi[];trend:Trend[];attention:Attention[];items:OperationalRecord[];total:number};

const workflowLabels:Record<string,string>={START:"Bắt đầu",SUBMIT:"Gửi duyệt",APPROVE:"Phê duyệt",REJECT:"Trả lại",CLOSE:"Đóng",REOPEN:"Mở lại"};
const moduleCode=(view:OperationalView)=>view.replace("mes-","");

function MesModal({title,children,onClose}:{title:string;children:ReactNode;onClose:()=>void}){
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="panel mes-modal" role="dialog" aria-modal="true" aria-label={title} onMouseDown={event=>event.stopPropagation()}><header><div><p className="eyebrow">MES Than Mạo Khê</p><h2>{title}</h2></div><button onClick={onClose} aria-label="Đóng"><AppIcon name="x" size={16}/></button></header>{children}</section></div>;
}

function StatusBadge({value}:{value:string}){
  const text=value.toLowerCase();const tone=/đã|hoạt động|bình thường|sẵn sàng|đạt|đúng|đủ|thấp/.test(text)?"green":/cao|nghiêm trọng|lỗi|trễ|chậm|vượt|gián đoạn|không đạt/.test(text)?"red":/chờ|đang|gần|nháp|thiếu|trung bình|trả lại/.test(text)?"amber":"blue";
  return <span className={`mes-status ${tone}`}>{value}</span>;
}

function FilterBar({period,organization,source,status,onChange,onReset}:{period:string;organization:string;source:string;status:string;onChange:(key:string,value:string)=>void;onReset:()=>void}){
  const current=new Date().toLocaleDateString("vi-VN",{month:"2-digit",year:"numeric"});
  return <section className="panel mes-filter-bar mes-prototype-filters">
    <label>Kỳ dữ liệu<select value={period} onChange={event=>onChange("period",event.target.value)}><option value="all">Tất cả kỳ</option><option value={current}>{current}</option></select></label>
    <label>Đơn vị<select value={organization} onChange={event=>onChange("organization",event.target.value)}><option value="all">Tất cả đơn vị</option><option value="TOAN-CONG-TY">Toàn công ty</option><option value="PX-KT1">PX Khai thác 1</option><option value="PHONG-KH">Phòng Kế hoạch</option></select></label>
    <label>Nguồn dữ liệu<select value={source} onChange={event=>onChange("source",event.target.value)}><option value="all">Tất cả nguồn</option><option>Nhập trực tiếp</option><option>Excel</option><option>API nội bộ</option></select></label>
    <label>Trạng thái<select value={status} onChange={event=>onChange("status",event.target.value)}><option value="">Tất cả trạng thái</option><option value="DRAFT">Nháp</option><option value="IN_PROGRESS">Đang xử lý</option><option value="PENDING_APPROVAL">Chờ phê duyệt</option><option value="APPROVED">Đã duyệt</option><option value="CLOSED">Đã đóng</option><option value="REJECTED">Bị trả lại</option></select></label>
    <button className="text-button" onClick={onReset}><AppIcon name="refresh" size={13}/> Đặt lại</button>
  </section>;
}

function KpiGrid({definition,kpis}:{definition:MesPrototypeDefinition;kpis:Kpi[]}){
  return <section className="mes-kpi-grid">{kpis.map(item=><article className="panel mes-kpi" key={item.label}><span className={`mes-kpi-icon ${item.tone}`}><AppIcon name={item.tone==="red"?"bell":definition.icon} size={19}/></span><div><small>{item.label}</small><strong>{item.value}</strong><span className={item.tone==="red"?"negative":"positive"}>{item.note}</span></div></article>)}</section>;
}

function ColumnChart({trend}:{trend:Trend[]}){
  const max=Math.max(1,...trend.map(item=>Number(item.value)));
  return <div className="mes-column-chart" aria-label="Biểu đồ cột xu hướng">{trend.map(item=><div key={item.date} title={`${item.date}: ${item.value}`}><b style={{height:`${Math.max(4,Number(item.value)/max*100)}%`}}/><span>{new Date(`${item.date}T00:00:00`).toLocaleDateString("vi-VN",{day:"2-digit",month:"2-digit"})}</span></div>)}</div>;
}

function InsightGrid({definition,trend,attention}:{definition:MesPrototypeDefinition;trend:Trend[];attention:Attention[]}){
  return <section className="mes-prototype-insights"><article className="panel mes-production-chart"><header><div><h2>{definition.trendLabel}</h2><p>Dữ liệu tổng hợp trực tiếp từ PostgreSQL</p></div><span className="mes-source-chip"><AppIcon name="database" size={13}/>Dữ liệu hoạt động</span></header><ColumnChart trend={trend}/></article><article className="panel mes-alert-panel"><header><div><h2>Vấn đề cần chú ý</h2><p>Ưu tiên theo mức độ và hạn xử lý</p></div><span className="mes-alert-count">{attention.length}</span></header>{attention.map(item=><div className="mes-attention-row" key={item.id}><i className={item.severity==="Cao"||item.severity==="Nghiêm trọng"?"red":"amber"}/><span><strong>{item.title}</strong><small>{item.detail}</small></span><StatusBadge value={item.severity}/></div>)}{attention.length===0&&<p className="mes-empty-block">Không có ngoại lệ cần xử lý.</p>}</article></section>;
}

function DataTable({columns,records,onView,onWorkflow,busy}:{columns:MesColumn[];records:OperationalRecord[];onView:(row:OperationalRecord)=>void;onWorkflow:(row:OperationalRecord,action:string)=>void;busy:boolean}){
  return <section className="panel table-panel mes-table-panel"><div className="mes-table-scroll"><table><thead><tr><th>STT</th>{columns.map(column=><th className={column.numeric?"numeric":""} key={column.key}>{column.label}</th>)}<th>Thao tác</th></tr></thead><tbody>{records.map((record,index)=><tr key={record.id}><td>{index+1}</td>{columns.map(column=><td className={column.numeric?"numeric":""} key={column.key}>{["status","severity","result"].includes(column.key)?<StatusBadge value={String(record.data[column.key]??"—")}/>:String(record.data[column.key]??"—")}</td>)}<td><div className="mes-row-actions"><button className="mes-icon-button" title="Xem chi tiết" onClick={()=>onView(record)}><AppIcon name="eye" size={15}/></button>{record.actions.map(action=><button disabled={busy} className={`mes-workflow-button ${action.toLowerCase()}`} key={action} onClick={()=>onWorkflow(record,action)}>{workflowLabels[action]||action}</button>)}</div></td></tr>)}{records.length===0&&<tr><td colSpan={columns.length+2} className="mes-empty">Không có dữ liệu phù hợp với bộ lọc.</td></tr>}</tbody></table></div><footer className="mes-table-footer"><span>Hiển thị {records.length} bản ghi từ backend</span><div><button disabled>‹</button><button className="active">1</button><button disabled>›</button></div></footer></section>;
}

function Detail({definition,record}:{definition:MesPrototypeDefinition;record:OperationalRecord}){
  return <><div className="mes-record-meta"><span>Mã: <strong>{record.code}</strong></span><span>Luồng liên thông: <strong>{record.correlationKey||"—"}</strong></span><StatusBadge value={record.statusLabel}/><span>Phiên bản {record.version}</span></div><div className="mes-detail-grid">{definition.columns.map(column=><div key={column.key}><span>{column.label}</span><strong>{String(record.data[column.key]??"—")}</strong></div>)}</div></>;
}

function ActionForm({definition,tab,busy,onClose,onSubmit}:{definition:MesPrototypeDefinition;tab:string;busy:boolean;onClose:()=>void;onSubmit:(input:Record<string,unknown>)=>void}){
  const submit=(event:FormEvent<HTMLFormElement>)=>{event.preventDefault();const data=new FormData(event.currentTarget);onSubmit({tab,title:data.get("title"),organization:data.get("organization"),period:data.get("period"),value:Number(data.get("value")||0),unit:"PERCENT",owner:data.get("owner"),severity:data.get("severity"),source:data.get("source"),note:data.get("note")});};
  return <form className="mes-form" onSubmit={submit}><label>Kỳ dữ liệu<input name="period" required defaultValue={new Date().toLocaleDateString("vi-VN",{month:"2-digit",year:"numeric"})}/></label><label>Đơn vị<select name="organization" defaultValue="TOAN-CONG-TY"><option value="TOAN-CONG-TY">Toàn công ty</option><option value="PX-KT1">PX Khai thác 1</option><option value="PHONG-KH">Phòng Kế hoạch</option></select></label><label>Giá trị<input name="value" required type="number" min="0" defaultValue="75"/></label><label>Chủ trì<input name="owner" required minLength={3} defaultValue="Phòng Điều độ"/></label><label>Mức độ<select name="severity" defaultValue="MEDIUM"><option value="LOW">Thấp</option><option value="MEDIUM">Trung bình</option><option value="HIGH">Cao</option><option value="CRITICAL">Nghiêm trọng</option></select></label><label>Nguồn dữ liệu<select name="source" defaultValue="MANUAL"><option value="MANUAL">Nhập trực tiếp</option><option value="EXCEL">Excel</option><option value="API">API nội bộ</option><option value="DATALAKE">Data Lake</option></select></label><label className="wide">Nội dung nghiệp vụ<textarea name="title" required minLength={3} placeholder={`Nhập thông tin cho ${definition.title.toLowerCase()}`}/></label><label className="wide">Ghi chú<textarea name="note" maxLength={1000} defaultValue={`Tạo tại tab ${tab}`}/></label><div className="mes-form-note"><AppIcon name="check-circle" size={15}/><span>Bản ghi được lưu vào PostgreSQL, bắt đầu ở trạng thái Nháp và đi qua workflow phê duyệt có audit log.</span></div><footer><button type="button" className="secondary-button" onClick={onClose}>Hủy</button><button disabled={busy} className="primary-button">{busy?"Đang lưu":"Lưu bản ghi"}</button></footer></form>;
}

function exportCsv(definition:MesPrototypeDefinition,records:OperationalRecord[]){const header=definition.columns.map(column=>`"${column.label}"`).join(",");const body=records.map(record=>definition.columns.map(column=>`"${String(record.data[column.key]??"").replaceAll('"','""')}"`).join(",")).join("\n");const blob=new Blob(["\uFEFF"+header+"\n"+body],{type:"text/csv;charset=utf-8"});const url=URL.createObjectURL(blob);const link=document.createElement("a");link.href=url;link.download=`${definition.title.toLowerCase().replaceAll(" ","-")}.csv`;link.click();URL.revokeObjectURL(url);}

function OperationalMesWorkspace({view,apiUrl}:{view:OperationalView;apiUrl:string}){
  const definition=mesPrototypeDefinitions[view];const module=moduleCode(view);
  const[activeTab,setActiveTab]=useState(definition.tabs[0]);const[query,setQuery]=useState("");const[status,setStatus]=useState("");const[period,setPeriod]=useState("all");const[organization,setOrganization]=useState("all");const[source,setSource]=useState("all");const[data,setData]=useState<Overview>();const[busy,setBusy]=useState(false);const[error,setError]=useState("");const[notice,setNotice]=useState("");const[modal,setModal]=useState<"action"|"detail"|null>(null);const[selected,setSelected]=useState<OperationalRecord>();
  const load=useCallback(async()=>{setBusy(true);setError("");try{const params=new URLSearchParams({tab:activeTab});if(status)params.set("status",status);setData(await mesApi<Overview>(apiUrl,`/api/v1/mes/modules/${module}/overview?${params}`));}catch(cause){setError(cause instanceof Error?cause.message:"Không tải được dữ liệu nghiệp vụ.");}finally{setBusy(false);}},[activeTab,apiUrl,module,status]);
  useEffect(()=>{void load();},[load]);
  const records=useMemo(()=>(data?.items||[]).filter(record=>(period==="all"||record.period===period)&&(organization==="all"||record.organization===organization)&&(source==="all"||record.source===source)&&Object.values(record.data).join(" ").toLowerCase().includes(query.trim().toLowerCase())),[data,period,organization,source,query]);
  const changeFilter=(key:string,value:string)=>{if(key==="period")setPeriod(value);if(key==="organization")setOrganization(value);if(key==="source")setSource(value);if(key==="status")setStatus(value);};
  const reset=()=>{setPeriod("all");setOrganization("all");setSource("all");setStatus("");setQuery("");};
  const create=async(input:Record<string,unknown>)=>{setBusy(true);setError("");try{await mesApi(apiUrl,`/api/v1/mes/modules/${module}/records`,{method:"POST",body:JSON.stringify(input)});setModal(null);setNotice("Đã tạo bản ghi. Dữ liệu được lưu và audit thành công.");await load();}catch(cause){setError(cause instanceof Error?cause.message:"Không tạo được bản ghi.");}finally{setBusy(false);}};
  const workflow=async(record:OperationalRecord,action:string)=>{setBusy(true);setError("");try{await mesApi(apiUrl,`/api/v1/mes/modules/${module}/records/${record.id}/workflow`,{method:"PATCH",body:JSON.stringify({action,expectedVersion:record.version})});setNotice(`Đã thực hiện: ${workflowLabels[action]||action}.`);await load();}catch(cause){setError(cause instanceof Error?cause.message:"Không cập nhật được trạng thái.");}finally{setBusy(false);}};
  return <div className="mes-workspace mes-prototype-workspace mes-operational-workspace"><div className="page-heading mes-page-heading"><div><p className="eyebrow">MES Than Mạo Khê · {definition.eyebrow}</p><h1>{definition.title}</h1><p className="page-description">{definition.description}</p></div><div className="mes-heading-actions"><span className="mes-live-pill"><i/>Dữ liệu hoạt động</span><button className="secondary-button" disabled={busy} onClick={()=>void load()}><AppIcon name="refresh" size={14}/>{busy?"Đang tải":"Làm mới"}</button><button className="primary-button" disabled={definition.actionDisabled} title={definition.actionDisabled?"Chỉ bật gửi sau khi cấu hình API Portal/TKV":""} onClick={()=>setModal("action")}><AppIcon name={definition.icon} size={14}/>{definition.action}</button></div></div>{error&&<p className="operation-message error" role="alert">{error}</p>}{notice&&<p className="operation-message" role="status">{notice}</p>}<FilterBar period={period} organization={organization} source={source} status={status} onChange={changeFilter} onReset={reset}/><div className="mes-tabs" role="tablist" aria-label={`Điều hướng ${definition.title}`}>{definition.tabs.map(tab=><button role="tab" aria-selected={activeTab===tab} key={tab} className={activeTab===tab?"active":""} onClick={()=>{setActiveTab(tab);setNotice("");}}>{tab}</button>)}</div><KpiGrid definition={definition} kpis={data?.kpis||[]}/><InsightGrid definition={definition} trend={data?.trend||[]} attention={data?.attention||[]}/><div className="mes-list-tools"><div><h2>{activeTab}</h2><p>Dữ liệu PostgreSQL · workflow, audit và outbox đang hoạt động.</p></div><label><AppIcon name="search" size={14}/><input value={query} onChange={event=>setQuery(event.target.value)} placeholder="Tìm kiếm bản ghi..."/></label><button className="secondary-button" onClick={()=>exportCsv(definition,records)}><AppIcon name="download" size={14}/> Xuất dữ liệu</button></div><DataTable columns={definition.columns} records={records} onView={record=>{setSelected(record);setModal("detail");}} onWorkflow={workflow} busy={busy}/>{modal==="action"&&<MesModal title={definition.action} onClose={()=>setModal(null)}><ActionForm definition={definition} tab={activeTab} busy={busy} onClose={()=>setModal(null)} onSubmit={create}/></MesModal>}{modal==="detail"&&selected&&<MesModal title="Chi tiết bản ghi" onClose={()=>setModal(null)}><Detail definition={definition} record={selected}/><footer className="mes-detail-actions"><button className="primary-button" onClick={()=>setModal(null)}>Đóng</button></footer></MesModal>}</div>;
}

export default function MesWorkspace({view,apiUrl}:{view:MesView;apiUrl:string}){
  if(view==="mes-dashboard")return <MesExecutiveDashboard apiUrl={apiUrl}/>;
  if(view==="mes-coal-flow")return <MesInventoryReconciliation apiUrl={apiUrl}/>;
  if(view==="mes-reporting-center")return <DailyReportingCenter apiUrl={apiUrl}/>;
  if(view==="mes-data-mapping")return <MesMasterData apiUrl={apiUrl}/>;
  return <OperationalMesWorkspace view={view} apiUrl={apiUrl}/>;
}
