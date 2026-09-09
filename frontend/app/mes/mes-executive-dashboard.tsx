"use client";

import {useCallback,useEffect,useMemo,useState} from "react";
import {AppIcon} from "../components/app-icon";
import {mesApi,viNumber} from "./mes-live-api";

type Summary={releases:number;sourceRows:number;metricPoints:number;distinctProducts:number;distinctPartners:number;unresolvedReferences:number;latestReportingDate:string|null;metricTotals:Record<string,number>};
type ReconItem={productId:string;productCode:string;productName:string;unitCode:string;receipts:number;issues:number;reportedClosingStock:number|null;variance:number|null;status:string;reason:string};
type Recon={reportingDate:string;coveredContracts:number;requiredContracts:number;coverageComplete:boolean;totalProducts:number;balanced:number;variances:number;incomplete:number;unverifiedMasterData:number;totalReceipts:number;totalIssues:number;totalVariance:number;items:ReconItem[]};
type DayPoint={date:string;value:number;hasData:boolean};
type SafetyAttention={id:string;title:string;detail:string;severity:string};
type SafetyOverview={attention:SafetyAttention[];kpis:Array<{label:string;value:string;note:string;tone:string}>};

const isoToday=()=>new Date().toLocaleDateString("en-CA");
const metric=(summary:Summary|undefined,...codes:string[])=>codes.reduce((total,code)=>total+Number(summary?.metricTotals?.[code]||0),0);
const hasMetric=(summary:Summary|undefined,...codes:string[])=>codes.some(code=>Object.hasOwn(summary?.metricTotals||{},code));
const formatTonne=(value:number,available=true)=>available?`${viNumber(value)} t`:"—";
const formatDate=(value:string)=>new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN",{day:"2-digit",month:"2-digit"});
const periodLabel=(value:string)=>new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN",{month:"2-digit",year:"numeric"});
const statusLabel=(value:string)=>({BALANCED:"Đạt",VARIANCE:"Chênh lệch",INCOMPLETE:"Thiếu dữ liệu",MASTER_UNVERIFIED:"Chưa xác nhận master"}[value]||value);
const navigate=(route:string)=>{window.history.pushState(null,"",route);window.dispatchEvent(new PopStateEvent("popstate"));window.scrollTo({top:0,behavior:"smooth"});};

const domains=[
  {no:"01",label:"Điều hành tổng hợp",note:"Tình hình & quyết định"},
  {no:"02",label:"Kế hoạch – dự báo",note:"Nhìn trước cuối kỳ"},
  {no:"03",label:"Tiêu thụ – dòng than",note:"Cân đối sản xuất và kho"},
  {no:"04",label:"An toàn",note:"Rủi ro & phòng ngừa"},
  {no:"05",label:"Thiết bị",note:"Sẵn sàng & bảo trì"},
  {no:"06",label:"Tài chính",note:"Giá thành & chi phí"},
  {no:"07",label:"ESG",note:"Xanh & bền vững"},
];

export default function MesExecutiveDashboard({apiUrl}:{apiUrl:string}){
  const[date,setDate]=useState(isoToday);const[scope,setScope]=useState("Toàn công ty");const[summary,setSummary]=useState<Summary>();const[recon,setRecon]=useState<Recon>();const[safety,setSafety]=useState<SafetyOverview>();const[series,setSeries]=useState<DayPoint[]>([]);const[activeDomain,setActiveDomain]=useState("01");const[loading,setLoading]=useState(true);const[error,setError]=useState("");const[lastUpdated,setLastUpdated]=useState("");
  const load=useCallback(async()=>{setLoading(true);setError("");try{
    const selected=new Date(`${date}T00:00:00`);const dates=Array.from({length:14},(_,index)=>{const item=new Date(selected);item.setDate(selected.getDate()-(13-index));return item.toLocaleDateString("en-CA");});
    const[s,r,daily,safetyData]=await Promise.all([
      mesApi<Summary>(apiUrl,`/api/v1/mes/internal-data/summary?from=${date}&to=${date}`),
      mesApi<Recon>(apiUrl,`/api/v1/mes/reconciliation/inventory?date=${date}&size=8`),
      Promise.all(dates.map(day=>mesApi<Summary>(apiUrl,`/api/v1/mes/internal-data/summary?from=${day}&to=${day}`))),
      mesApi<SafetyOverview>(apiUrl,"/api/v1/mes/modules/occupational-safety/overview?tab=Tổng%20quan"),
    ]);
    setSummary(s);setRecon(r);setSafety(safetyData);setSeries(daily.map((item,index)=>({date:dates[index],value:metric(item,"mined_receipt","processing_receipt"),hasData:item.releases>0})));setLastUpdated(new Date().toLocaleTimeString("vi-VN",{hour:"2-digit",minute:"2-digit"}));
  }catch(cause){setError(cause instanceof Error?cause.message:"Không tải được Dashboard điều hành.");}finally{setLoading(false);}},[apiUrl,date]);
  useEffect(()=>{void load();},[load]);

  const production=metric(summary,"mined_receipt","processing_receipt");const consumption=metric(summary,"sale_issue");const stock=metric(summary,"closing_stock");
  const decisions=useMemo(()=>recon?.items.filter(item=>item.status!=="BALANCED").slice(0,4)||[],[recon]);
  const efficiency=useMemo(()=>(recon?.items||[]).slice(0,5).map(item=>{const variance=Math.abs(Number(item.variance||0));const base=Math.max(Math.abs(Number(item.reportedClosingStock||0)),1);const score=item.status==="BALANCED"?100:item.status==="VARIANCE"?Math.max(0,Math.round(100-variance/base*100)):0;return{...item,score};}),[recon]);
  const maxSeries=Math.max(1,...series.map(point=>point.value));

  return <div className="mes-workspace mes-live-workspace mes-executive-dashboard">
    <div className="page-heading mes-page-heading"><div><p className="eyebrow">Hệ thống điều hành sản xuất MES</p><h1>Dashboard điều hành</h1></div></div>
    <section className="mes-executive-intro"><div><p>BAN LÃNH ĐẠO · DỮ LIỆU CẬP NHẬT {lastUpdated||"—"}</p><h2>Điều hành tổng hợp</h2><span>Kỳ {periodLabel(date)} · {scope} · Dữ liệu canonical đã phát hành</span></div><div className="mes-executive-filters"><label>Ngày dữ liệu<input type="date" value={date} onChange={event=>setDate(event.target.value)}/></label><label>Phạm vi<select value={scope} onChange={event=>setScope(event.target.value)}><option>Toàn công ty</option></select></label><button className="secondary-button" disabled={loading} onClick={()=>void load()}><AppIcon name="refresh" size={14}/>{loading?"Đang tải":"Làm mới"}</button></div></section>
    {error&&<p className="operation-message error" role="alert">{error}</p>}
    <section className="mes-executive-domains" aria-label="Lát cắt điều hành">{domains.map(domain=><button key={domain.no} className={activeDomain===domain.no?"active":""} onClick={()=>setActiveDomain(domain.no)}><b>{domain.no}</b><span><strong>{domain.label}</strong><small>{domain.note}</small></span></button>)}</section>
    {activeDomain!=="01"&&<div className="mes-domain-notice"><AppIcon name="database" size={15}/><span>Đang xem lát cắt <strong>{domains.find(item=>item.no===activeDomain)?.label}</strong>. Backend chuyên ngành sẽ được nối ở lát cắt triển khai tương ứng; các KPI canonical tổng hợp vẫn giữ nguyên bên dưới.</span></div>}
    <section className="mes-executive-kpis">
      <ExecutiveKpi icon="factory" label="Sản lượng ngày" value={formatTonne(production,hasMetric(summary,"mined_receipt","processing_receipt"))} note={`${summary?.releases||0} bản phát hành`} route="/mes/production-operations"/>
      <ExecutiveKpi icon="truck" label="Tiêu thụ ngày" value={formatTonne(consumption,hasMetric(summary,"sale_issue"))} note="Nguồn dữ liệu canonical" route="/mes/sales-logistics"/>
      <ExecutiveKpi icon="warehouse" label="Tồn kho" value={formatTonne(stock,hasMetric(summary,"closing_stock"))} note={`${recon?.variances||0} chênh lệch`} tone={(recon?.variances||0)>0?"amber":"green"} route="/mes/coal-flow"/>
      <ExecutiveKpi icon="check-circle" label="Độ phủ đối soát" value={`${recon?.coveredContracts||0}/${recon?.requiredContracts||5}`} note={recon?.coverageComplete?"Đủ hợp đồng dữ liệu":"Chưa đủ hợp đồng dữ liệu"} tone={recon?.coverageComplete?"green":"amber"} route="/mes/coal-flow"/>
      <ExecutiveKpi icon="database" label="Điểm dữ liệu" value={viNumber(summary?.metricPoints)} note={`${summary?.sourceRows||0} dòng nguồn`} route="/mes/reporting-center"/>
      <ExecutiveKpi icon="circle-alert" label="Ngoại lệ dữ liệu" value={viNumber((recon?.variances||0)+(recon?.incomplete||0)+(recon?.unverifiedMasterData||0))} note="Cần chỉ đạo xử lý" tone={(recon?.variances||recon?.incomplete||recon?.unverifiedMasterData||0)>0?"red":"green"} route="/mes/integration-quality"/>
    </section>
    {!loading&&summary?.releases===0&&<section className="panel mes-canonical-empty"><AppIcon name="database" size={26}/><div><h2>Chưa có dữ liệu canonical trong ngày {formatDate(date)}</h2><p>Import, xác nhận, phê duyệt và khóa lô tại Trung tâm báo cáo. Dashboard không thay dữ liệu thật bằng số liệu mô phỏng.</p></div></section>}
    <section className="mes-executive-main-grid">
      <article className="panel mes-executive-chart"><header><div><h3>Nhịp sản xuất 14 ngày gần nhất</h3><p>Sản lượng canonical đã phát hành theo ngày</p></div><span><i/>Sản lượng thực hiện</span></header><div className="mes-executive-bars">{series.map(point=><div key={point.date} title={`${formatDate(point.date)}: ${point.hasData?`${viNumber(point.value)} tấn`:"Chưa có dữ liệu"}`}><b style={{height:point.hasData?`${Math.max(8,point.value/maxSeries*100)}%`:"3%"}} className={point.hasData?"":"empty"}/><small>{new Date(`${point.date}T00:00:00`).getDate()}</small></div>)}</div></article>
      <article className="panel mes-executive-decisions"><header><div><h3>Quyết định cần ban hành</h3><p>Xếp theo ngoại lệ ảnh hưởng đến cân đối kho</p></div><strong>{decisions.length} việc</strong></header>{decisions.map((item,index)=><button key={item.productId} onClick={()=>navigate("/mes/coal-flow")}><em className={index===0?"urgent":"high"}>{index===0?"KHẨN":"CAO"}</em><span><b>{item.productCode} · {item.productName}</b><small>{item.reason||`Chênh lệch ${viNumber(item.variance)} ${item.unitCode}`}</small></span><i>Mở đối soát →</i></button>)}{!loading&&!decisions.length&&<div className="mes-executive-empty"><AppIcon name="check-circle" size={20}/><span>Không có ngoại lệ đối soát cần chỉ đạo.</span></div>}</article>
      <article className="panel mes-executive-efficiency"><header><div><h3>Hiệu quả theo sản phẩm</h3><p>Mức cân bằng nhập · xuất · tồn</p></div><button onClick={()=>navigate("/mes/coal-flow")}>Xem chi tiết</button></header>{efficiency.map((item,index)=><div className="mes-efficiency-row" key={item.productId}><b>{index+1}</b><strong>{item.productName||item.productCode}</strong><span><i style={{width:`${item.score}%`}}/></span><em>{item.score}%</em><small className={item.score>=90?"green":item.score>=70?"amber":"red"}>{statusLabel(item.status)}</small></div>)}{!loading&&!efficiency.length&&<p className="mes-executive-empty">Chưa có sản phẩm canonical để đánh giá.</p>}</article>
      <article className="panel mes-executive-risks"><header><div><h3>An toàn & rủi ro vận hành</h3><p>Điểm nóng từ module An toàn lao động</p></div><button onClick={()=>navigate("/mes/occupational-safety")}>Mở module</button></header><div className="mes-executive-risk-kpis">{(safety?.kpis||[]).slice(0,3).map(item=><span key={item.label}><small>{item.label}</small><strong>{item.value}</strong></span>)}</div>{(safety?.attention||[]).slice(0,4).map(item=><button className="mes-executive-risk-row" key={item.id} onClick={()=>navigate("/mes/occupational-safety")}><i className={item.severity==="Nghiêm trọng"||item.severity==="Cao"?"red":"amber"}/><span><strong>{item.title}</strong><small>{item.detail}</small></span><em>{item.severity}</em></button>)}{!loading&&!(safety?.attention||[]).length&&<div className="mes-executive-empty"><AppIcon name="check-circle" size={20}/><span>Không có điểm nóng an toàn đang mở.</span></div>}</article>
    </section>
  </div>;
}

function ExecutiveKpi({icon,label,value,note,tone="blue",route}:{icon:string;label:string;value:string;note:string;tone?:string;route:string}){return <button className={`panel mes-executive-kpi ${tone}`} onClick={()=>navigate(route)}><span><AppIcon name={icon} size={17}/></span><small>{label}</small><strong>{value}</strong><em>{note}</em></button>}
