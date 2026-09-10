"use client";

import {useCallback,useEffect,useMemo,useState} from "react";
import {AppIcon} from "../components/app-icon";
import {mesPrototypeDefinitions,type MesPrototypeDefinition} from "./mes-demo-data";
import {mesApi,viNumber} from "./mes-live-api";

type Summary={releases:number;sourceRows:number;metricPoints:number;distinctProducts:number;distinctPartners:number;unresolvedReferences:number;latestReportingDate:string|null;metricTotals:Record<string,number>};
type ReconItem={productId:string;productCode:string;productName:string;unitCode:string;receipts:number;issues:number;reportedClosingStock:number|null;variance:number|null;status:string;reason:string};
type Recon={reportingDate:string;coveredContracts:number;requiredContracts:number;coverageComplete:boolean;totalProducts:number;balanced:number;variances:number;incomplete:number;unverifiedMasterData:number;totalReceipts:number;totalIssues:number;totalVariance:number;items:ReconItem[]};
type DayPoint={date:string;value:number;hasData:boolean};
type SafetyAttention={id:string;title:string;detail:string;severity:string};
type SafetyOverview={attention:SafetyAttention[];kpis:Array<{label:string;value:string;note:string;tone:string}>};
type ExecutiveDomain={no:string;label:string;note:string;icon:string;route:string;fixtureKey?:string};

const isoToday=()=>new Date().toLocaleDateString("en-CA");
const metric=(summary:Summary|undefined,...codes:string[])=>codes.reduce((total,code)=>total+Number(summary?.metricTotals?.[code]||0),0);
const hasMetric=(summary:Summary|undefined,...codes:string[])=>codes.some(code=>Object.hasOwn(summary?.metricTotals||{},code));
const formatTonne=(value:number,available=true)=>available?`${viNumber(value)} t` : "—";
const formatDate=(value:string)=>new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN",{day:"2-digit",month:"2-digit"});
const periodLabel=(value:string)=>new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN",{month:"2-digit",year:"numeric"});
const statusLabel=(value:string)=>({BALANCED:"Đạt",VARIANCE:"Chênh lệch",INCOMPLETE:"Thiếu dữ liệu",MASTER_UNVERIFIED:"Chưa xác nhận master"}[value]||value);
const navigate=(route:string)=>{window.history.pushState(null,"",route);window.dispatchEvent(new PopStateEvent("popstate"));window.scrollTo({top:0,behavior:"smooth"});};

const domains:ExecutiveDomain[]=[
  {no:"01",label:"Điều hành tổng hợp",note:"Tình hình & quyết định",icon:"layout-dashboard",route:"/mes/dashboard"},
  {no:"02",label:"Kế hoạch – dự báo",note:"Tiến độ và cuối kỳ",icon:"calendar",route:"/mes/planning-performance",fixtureKey:"mes-planning-performance"},
  {no:"03",label:"Tiêu thụ – dòng than",note:"Cân đối sản xuất và kho",icon:"truck",route:"/mes/sales-logistics",fixtureKey:"mes-sales-logistics"},
  {no:"04",label:"An toàn",note:"Rủi ro & phòng ngừa",icon:"shield",route:"/mes/occupational-safety",fixtureKey:"mes-occupational-safety"},
  {no:"05",label:"Thiết bị",note:"Sẵn sàng & bảo trì",icon:"settings",route:"/mes/materials-equipment",fixtureKey:"mes-materials-equipment"},
  {no:"06",label:"Tài chính",note:"Giá thành & chi phí",icon:"chart-combined",route:"/mes/finance-accounting",fixtureKey:"mes-finance-accounting"},
  {no:"07",label:"ESG",note:"Xanh & bền vững",icon:"leaf",route:"/mes/esg",fixtureKey:"mes-esg"},
];

const overviewDemoSeries=[18420,19680,21350,20540,22810,24120,23450,24980,25640,23920,26780,25110,27240,24580];
const overviewDemoDecisions=[
  {productId:"demo-1",productCode:"CB-AT-041",productName:"Khí CH4 khu vực XV-120",unitCode:"",receipts:0,issues:0,reportedClosingStock:0,variance:1,status:"VARIANCE",reason:"Vượt ngưỡng cảnh báo lúc 13:42 · cần xử lý trước 16:00"},
  {productId:"demo-2",productCode:"KH-DL2",productName:"Mét lò PX Đào lò 2",unitCode:"m",receipts:0,issues:0,reportedClosingStock:10230,variance:-2270,status:"VARIANCE",reason:"Chậm 12,4% kế hoạch · chờ giải trình đơn vị"},
  {productId:"demo-3",productCode:"GH-260909-014",productName:"Lệnh giao Nhiệt điện A",unitCode:"t",receipts:0,issues:0,reportedClosingStock:1850,variance:-650,status:"INCOMPLETE",reason:"Tàu MK-08 dự kiến chậm cập cảng 4 giờ"},
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

  const liveOverview=Boolean(summary?.releases&&series.some(point=>point.hasData));
  const production=metric(summary,"mined_receipt","processing_receipt");const consumption=metric(summary,"sale_issue");const stock=metric(summary,"closing_stock");
  const liveDecisions=useMemo(()=>recon?.items.filter(item=>item.status!=="BALANCED").slice(0,4)||[],[recon]);
  const decisions=liveOverview?liveDecisions:overviewDemoDecisions;
  const liveEfficiency=useMemo(()=>(recon?.items||[]).slice(0,5).map(item=>{const variance=Math.abs(Number(item.variance||0));const base=Math.max(Math.abs(Number(item.reportedClosingStock||0)),1);const score=item.status==="BALANCED"?100:item.status==="VARIANCE"?Math.max(0,Math.round(100-variance/base*100)):0;return{...item,score};}),[recon]);
  const demoEfficiency=[
    {productId:"e1",productName:"Than nguyên khai",productCode:"TNK",score:96,status:"BALANCED"},
    {productId:"e2",productName:"Than sạch",productCode:"TS",score:87,status:"BALANCED"},
    {productId:"e3",productName:"Than cám",productCode:"TC",score:82,status:"VARIANCE"},
    {productId:"e4",productName:"Than cục",productCode:"TCU",score:78,status:"VARIANCE"},
  ];
  const efficiency=liveOverview&&liveEfficiency.length?liveEfficiency:demoEfficiency;
  const fallbackSeries=overviewDemoSeries.map((value,index)=>{const point=new Date(`${date}T00:00:00`);point.setDate(point.getDate()-(13-index));return{date:point.toLocaleDateString("en-CA"),value,hasData:true};});
  const displaySeries=liveOverview?series:(series.length?series.map((point,index)=>({...point,value:overviewDemoSeries[index],hasData:true})):fallbackSeries);
  const maxSeries=Math.max(1,...displaySeries.map(point=>point.value));
  const active=domains.find(item=>item.no===activeDomain)||domains[0];
  const fixture=active.fixtureKey?mesPrototypeDefinitions[active.fixtureKey]:undefined;

  return <div className="mes-workspace mes-live-workspace mes-executive-dashboard">
    <div className="page-heading mes-page-heading"><div><p className="eyebrow">MES Than Mạo Khê · Trung tâm điều hành</p><h1>Dashboard điều hành</h1><p className="page-description">Tổng hợp tình hình sản xuất, nguồn lực, tài chính và phát triển bền vững trên một màn hình.</p></div></div>
    <section className="mes-executive-intro"><div><p>BAN LÃNH ĐẠO · CẬP NHẬT {lastUpdated||"—"}</p><h2>{active.label}</h2><span>Kỳ {periodLabel(date)} · {scope} · {activeDomain==="01"&&liveOverview?"Dữ liệu canonical":"Dữ liệu mô phỏng phục vụ duyệt UI"}</span></div><div className="mes-executive-filters"><label>Ngày dữ liệu<input type="date" value={date} onChange={event=>setDate(event.target.value)}/></label><label>Phạm vi<select value={scope} onChange={event=>setScope(event.target.value)}><option>Toàn công ty</option><option>Khối sản xuất</option><option>Khối văn phòng</option></select></label><button className="secondary-button" disabled={loading} onClick={()=>void load()}><AppIcon name="refresh" size={16}/>{loading?"Đang tải":"Làm mới"}</button></div></section>
    {error&&<p className="operation-message error" role="alert">{error}</p>}
    <section className="mes-executive-domains" role="tablist" aria-label="Các lát cắt Dashboard điều hành">{domains.map(domain=><button id={`executive-tab-${domain.no}`} role="tab" aria-selected={activeDomain===domain.no} aria-controls="executive-tabpanel" key={domain.no} className={activeDomain===domain.no?"active":""} onClick={()=>setActiveDomain(domain.no)}><b><AppIcon name={domain.icon} size={16}/></b><span><strong>{domain.label}</strong><small>{domain.note}</small></span></button>)}</section>
    <section id="executive-tabpanel" role="tabpanel" aria-labelledby={`executive-tab-${activeDomain}`} className="mes-executive-tabpanel">
      {activeDomain==="01"?<>
        {!liveOverview&&!loading&&<div className="mes-dashboard-demo-banner"><AppIcon name="database" size={17}/><span><strong>Đang hiển thị bộ dữ liệu mô phỏng đã duyệt</strong><small>Hệ thống tự chuyển sang dữ liệu canonical khi kỳ được chọn có bản phát hành hợp lệ.</small></span></div>}
        <section className="mes-executive-kpis">
          <ExecutiveKpi icon="factory" label="Sản lượng ngày" value={liveOverview?formatTonne(production,hasMetric(summary,"mined_receipt","processing_receipt")):"24.580 t"} note={liveOverview?`${summary?.releases||0} bản phát hành`:"+8,4% so với cùng kỳ"} route="/mes/production-operations"/>
          <ExecutiveKpi icon="calendar" label="Tiến độ kế hoạch" value="86,7%" note="+5,2% so với kỳ trước" tone="green" route="/mes/planning-performance"/>
          <ExecutiveKpi icon="warehouse" label="Tồn kho" value={liveOverview?formatTonne(stock,hasMetric(summary,"closing_stock")):"312.450 t"} note={liveOverview?`${recon?.variances||0} chênh lệch`:"-3,1% so với cùng kỳ"} tone={(recon?.variances||0)>0?"amber":"blue"} route="/mes/coal-flow"/>
          <ExecutiveKpi icon="flask" label="Chất lượng đạt" value="97,6%" note="125/128 mẫu đạt" tone="green" route="/mes/quality-acceptance"/>
          <ExecutiveKpi icon="truck" label="Tiêu thụ tháng" value={liveOverview?formatTonne(consumption,hasMetric(summary,"sale_issue")):"432.870 t"} note="86,6% kế hoạch" route="/mes/sales-logistics"/>
          <ExecutiveKpi icon="circle-alert" label="Cảnh báo mở" value={liveOverview?viNumber((recon?.variances||0)+(recon?.incomplete||0)+(recon?.unverifiedMasterData||0)):"12"} note="2 sự cố · 10 vượt ngưỡng" tone="red" route="/mes/alerts-directives"/>
        </section>
        <section className="mes-executive-main-grid">
          <article className="panel mes-executive-chart"><header><div><h3>Nhịp sản xuất 14 ngày gần nhất</h3><p>Sản lượng thực hiện theo ngày · đơn vị tấn</p></div><span><i/>Sản lượng thực hiện</span></header><div className="mes-executive-bars">{displaySeries.map(point=><div key={point.date} title={`${formatDate(point.date)}: ${viNumber(point.value)} tấn`}><b style={{height:`${Math.max(8,point.value/maxSeries*100)}%`}}/><small>{new Date(`${point.date}T00:00:00`).getDate()}</small></div>)}</div></article>
          <article className="panel mes-executive-decisions"><header><div><h3>Quyết định cần ban hành</h3><p>Cảnh báo gần nhất, xếp theo mức độ ảnh hưởng vận hành</p></div><strong>{decisions.length} việc</strong></header>{decisions.map((item,index)=><button key={item.productId} onClick={()=>navigate(index===0?"/mes/occupational-safety":"/mes/alerts-directives")}><em className={index===0?"urgent":"high"}>{index===0?"KHẨN":"CAO"}</em><span><b>{item.productCode} · {item.productName}</b><small>{item.reason||`Chênh lệch ${viNumber(item.variance)} ${item.unitCode}`}</small></span><i>Xem chi tiết →</i></button>)}</article>
          <article className="panel mes-executive-efficiency"><header><div><h3>Hiệu quả theo sản phẩm</h3><p>Mức cân bằng kế hoạch · sản xuất · tồn</p></div><button onClick={()=>navigate("/mes/coal-flow")}>Xem chi tiết</button></header>{efficiency.map((item,index)=><div className="mes-efficiency-row" key={item.productId}><b>{index+1}</b><strong>{item.productName||item.productCode}</strong><span><i style={{width:`${item.score}%`}}/></span><em>{item.score}%</em><small className={item.score>=90?"green":item.score>=70?"amber":"red"}>{statusLabel(item.status)}</small></div>)}</article>
          <article className="panel mes-executive-risks"><header><div><h3>An toàn & rủi ro vận hành</h3><p>Điểm nóng cần theo dõi trong ngày</p></div><button onClick={()=>navigate("/mes/occupational-safety")}>Mở module</button></header><div className="mes-executive-risk-kpis">{(safety?.kpis||[{label:"Ngày công an toàn",value:"128"},{label:"Nguy cơ đang mở",value:"16"},{label:"Đúng hạn",value:"91,2%"}]).slice(0,3).map(item=><span key={item.label}><small>{item.label}</small><strong>{item.value}</strong></span>)}</div>{(safety?.attention||[{id:"s1",title:"Khí CH4 vượt ngưỡng",detail:"Khu vực XV-120 lúc 13:42",severity:"Cao"},{id:"s2",title:"Rào chắn chưa đạt yêu cầu",detail:"Tuyến vận tải số 3",severity:"Trung bình"}]).slice(0,4).map(item=><button className="mes-executive-risk-row" key={item.id} onClick={()=>navigate("/mes/occupational-safety")}><i className={item.severity==="Nghiêm trọng"||item.severity==="Cao"?"red":"amber"}/><span><strong>{item.title}</strong><small>{item.detail}</small></span><em>{item.severity}</em></button>)}</article>
        </section>
      </>:fixture&&<DomainDashboard domain={active} definition={fixture}/>}
    </section>
  </div>;
}

function DomainDashboard({domain,definition}:{domain:ExecutiveDomain;definition:MesPrototypeDefinition}){
  const max=Math.max(1,...definition.trend);
  return <div className="mes-domain-dashboard">
    <div className="mes-dashboard-demo-banner"><AppIcon name={definition.icon} size={17}/><span><strong>{definition.title}</strong><small>{definition.description}</small></span><em>{definition.sourceMode}</em><button type="button" onClick={()=>navigate(domain.route)}>Mở module nghiệp vụ <AppIcon name="chevron-right" size={14}/></button></div>
    <section className="mes-domain-kpis">{definition.kpis.map(item=><article className={`panel mes-domain-kpi ${item.tone}`} key={item.label}><span><AppIcon name={definition.icon} size={18}/></span><div><small>{item.label}</small><strong>{item.value}<i>{item.unit}</i></strong><em>{item.delta}</em></div></article>)}</section>
    <section className="mes-domain-grid">
      <article className="panel mes-domain-trend"><header><div><h3>{definition.trendLabel}</h3><p>Dữ liệu mô phỏng theo 7 kỳ gần nhất</p></div><span><i/>Thực hiện</span></header><div className="mes-domain-bars">{definition.trend.map((value,index)=><div key={`${definition.title}-${index}`}><strong>{String(value).replace(".",",")}</strong><b style={{height:`${Math.max(12,value/max*100)}%`}}/><small>Kỳ {index+1}</small></div>)}</div></article>
      <article className="panel mes-domain-attention"><header><div><h3>Công việc cần chú ý</h3><p>Ngoại lệ cần Ban lãnh đạo theo dõi</p></div><strong>{definition.attention.length} cảnh báo</strong></header>{definition.attention.map((item,index)=><button key={item.title} onClick={()=>navigate(domain.route)}><span className={index===0?"red":"amber"}><AppIcon name="circle-alert" size={16}/></span><div><b>{item.title}</b><small>{item.detail}</small></div><em>{item.severity}</em></button>)}</article>
    </section>
    <article className="panel mes-domain-table"><header><div><h3>Dữ liệu điều hành gần nhất</h3><p>{definition.title} · cập nhật theo phạm vi đang chọn</p></div><button type="button" onClick={()=>navigate(domain.route)}>Xem tất cả</button></header><div className="mes-table-scroll"><table><thead><tr><th>STT</th>{definition.columns.map(column=><th key={column.key} className={column.numeric?"numeric":""}>{column.label}</th>)}</tr></thead><tbody>{definition.rows.map((row,index)=><tr key={`${definition.title}-${index}`}><td>{index+1}</td>{definition.columns.map(column=><td key={column.key} className={column.numeric?"numeric":""}>{column.key==="status"?<span className={`mes-status ${demoStatusTone(String(row[column.key]??""))}`}>{String(row[column.key]??"—")}</span>:String(row[column.key]??"—")}</td>)}</tr>)}</tbody></table></div></article>
  </div>;
}

function demoStatusTone(value:string){const normalized=value.toLowerCase();if(normalized.includes("đạt")||normalized.includes("hoàn")||normalized.includes("hoạt động")||normalized.includes("ngân sách"))return"green";if(normalized.includes("chậm")||normalized.includes("vượt")||normalized.includes("dưới")||normalized.includes("sửa"))return"red";return"amber";}
function ExecutiveKpi({icon,label,value,note,tone="blue",route}:{icon:string;label:string;value:string;note:string;tone?:string;route:string}){return <button className={`panel mes-executive-kpi ${tone}`} onClick={()=>navigate(route)}><span><AppIcon name={icon} size={18}/></span><small>{label}</small><strong>{value}</strong><em>{note}</em></button>}
