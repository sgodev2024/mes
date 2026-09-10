export type MesDemoRow = Record<string, string | number>;
export type MesColumn = { key: string; label: string; numeric?: boolean };
export type MesKpi = { label: string; value: string; unit?: string; delta: string; tone: "blue" | "green" | "amber" | "red" | "violet" };
export type MesAttention = { title: string; detail: string; severity: "Cao" | "Trung bình" | "Nhắc việc" };
export type MesPrototypeDefinition = {
  eyebrow: string;
  title: string;
  description: string;
  action: string;
  icon: string;
  sourceMode: string;
  tabs: string[];
  columns: MesColumn[];
  tabColumns?: Record<string, MesColumn[]>;
  tabActions?: Record<string, string>;
  kpis: MesKpi[];
  trendLabel: string;
  trend: number[];
  attention: MesAttention[];
  rows: MesDemoRow[];
  actionDisabled?: boolean;
};

const kpi = (label:string,value:string,delta:string,tone:MesKpi["tone"],unit=""):MesKpi => ({label,value,delta,tone,unit});
const attention = (title:string,detail:string,severity:MesAttention["severity"]):MesAttention => ({title,detail,severity});

export const mesPrototypeDefinitions: Record<string, MesPrototypeDefinition> = {
  "mes-planning-performance": {
    eyebrow:"Điều hành kế hoạch", title:"Kế hoạch & hiệu quả", icon:"calendar", action:"Lập kế hoạch",
    description:"Theo dõi mục tiêu, phiên bản kế hoạch, mức hoàn thành và hiệu quả theo kỳ, đơn vị.", sourceMode:"Nhập liệu/Excel · sẵn sàng Data Lake",
    tabs:["Tổng quan","Kế hoạch","Dự kiến","Hiệu quả","Phiên bản & phê duyệt"],
    kpis:[kpi("Hoàn thành kế hoạch","86,7","+5,2% so với kỳ trước","green","%"),kpi("Sản lượng lũy kế","615.570","Đạt 86,7% kế hoạch","blue","tấn"),kpi("Đơn vị chậm tiến độ","03","Cần giải trình trong ngày","red"),kpi("Phiên bản chờ duyệt","04","2 phiên bản quá 24 giờ","amber")],
    trendLabel:"Tỷ lệ hoàn thành kế hoạch 7 kỳ gần nhất",trend:[72,76,74,81,83,85,87],
    attention:[attention("Mét lò PX Đào lò 2 chậm 12,4%","Hạn giải trình 16:00 hôm nay","Cao"),attention("Kế hoạch tiêu thụ cần điều chỉnh","Tồn kho thành phẩm vượt ngưỡng","Trung bình")],
    columns:[{key:"indicator",label:"Chỉ tiêu"},{key:"organization",label:"Đơn vị"},{key:"period",label:"Kỳ"},{key:"plan",label:"Kế hoạch",numeric:true},{key:"actual",label:"Thực hiện",numeric:true},{key:"completion",label:"Hoàn thành"},{key:"variance",label:"Chênh lệch",numeric:true},{key:"status",label:"Trạng thái"}],
    rows:[{indicator:"Sản lượng than",organization:"Toàn Công ty",period:"09/2026",plan:"710.000",actual:"615.570",completion:"86,7%",variance:"-94.430",status:"Đang thực hiện"},{indicator:"Đào lò",organization:"PX Đào lò 2",period:"09/2026",plan:"12.500",actual:"10.230",completion:"81,8%",variance:"-2.270",status:"Chậm tiến độ"},{indicator:"Than sạch",organization:"Sàng tuyển",period:"09/2026",plan:"460.000",actual:"398.210",completion:"86,6%",variance:"-61.790",status:"Đang thực hiện"}]
  },
  "mes-production-operations": {
    eyebrow:"Điều hành sản xuất", title:"Sản xuất & điều hành", icon:"factory", action:"Ghi nhận sản lượng",
    description:"Tổng hợp sản lượng theo ngày, ca, công trường và trạng thái bàn giao số liệu.", sourceMode:"Nhập liệu/Excel · sẵn sàng SCADA/Data Lake",
    tabs:["Tổng quan ca","Sản lượng ngày","Bàn giao ca","Giải trình sai lệch"],
    kpis:[kpi("Sản lượng hôm nay","24.580","+8,4% cùng kỳ","blue","tấn"),kpi("Ca đang vận hành","06","6/7 đơn vị đã báo cáo","green"),kpi("Thời gian dừng","145","+35 phút so kế hoạch","red","phút"),kpi("Báo cáo chờ xác nhận","03","Ca 3 chưa đủ dữ liệu","amber")],
    trendLabel:"Sản lượng thực hiện theo ngày",trend:[21,23,22,25,24,27,25],
    attention:[attention("Thiếu số liệu ca 3","PX Khai thác 1 chưa xác nhận","Cao"),attention("Dừng máy ngoài kế hoạch","Máy xúc MX-03 · 42 phút","Trung bình")],
    columns:[{key:"date",label:"Ngày"},{key:"shift",label:"Ca"},{key:"organization",label:"Đơn vị"},{key:"location",label:"Khu vực"},{key:"indicator",label:"Chỉ tiêu"},{key:"actual",label:"Thực hiện",numeric:true},{key:"source",label:"Nguồn"},{key:"status",label:"Trạng thái"}],
    rows:[{date:"09/09/2026",shift:"Ca 1",organization:"PX Khai thác 1",location:"Lò XV-120",indicator:"Than nguyên khai",actual:"8.440 tấn",source:"Excel",status:"Đã xác nhận"},{date:"09/09/2026",shift:"Ca 2",organization:"PX Đào lò 2",location:"Lò XV-121",indicator:"Mét lò đào",actual:"23 m",source:"Nhập tay",status:"Chờ xác nhận"},{date:"09/09/2026",shift:"Ca 3",organization:"PX Khai thác 2",location:"Lò XV-203",indicator:"Than nguyên khai",actual:"7.810 tấn",source:"Excel",status:"Bản nháp"}]
  },
  "mes-quality-acceptance": {
    eyebrow:"Chất lượng sản phẩm", title:"Chất lượng & nghiệm thu", icon:"flask", action:"Tạo kết quả KCS",
    description:"Theo dõi mẫu kiểm tra, chỉ tiêu chất lượng, giám định và biên bản nghiệm thu.", sourceMode:"Nhập liệu/Excel · sẵn sàng LIS/Data Lake",
    tabs:["Tổng quan","Kết quả KCS","Giám định","Nghiệm thu","Sai lệch"],
    kpis:[kpi("Độ tro TB","16,85","-0,42% so giới hạn","green","%"),kpi("Độ ẩm TB","9,32","Trong ngưỡng","blue","%"),kpi("Tỷ lệ thu hồi","72,41","+1,8% so kỳ trước","violet","%"),kpi("Mẫu không đạt","03","3/128 mẫu kiểm tra","red")],
    trendLabel:"Tỷ lệ mẫu đạt 7 ngày",trend:[95,97,96,98,97,99,98],
    attention:[attention("Lô LT250923-002 không đạt","Độ tro 21,35% vượt ngưỡng","Cao"),attention("02 mẫu chờ giám định lại","Kho Than sạch 2","Trung bình")],
    columns:[{key:"lot",label:"Lô than"},{key:"product",label:"Chủng loại"},{key:"source",label:"Nguồn mẫu"},{key:"ash",label:"Độ tro"},{key:"moisture",label:"Độ ẩm"},{key:"recovery",label:"Thu hồi"},{key:"result",label:"Kết quả"},{key:"status",label:"Trạng thái"}],
    rows:[{lot:"LT250923-001",product:"Than cục",source:"Kho Than sạch 1",ash:"16,45%",moisture:"9,10%",recovery:"72,80%",result:"Đạt",status:"Đã nghiệm thu"},{lot:"LT250923-002",product:"Than bùn",source:"Kho Nguyên khai 1",ash:"21,35%",moisture:"12,30%",recovery:"68,40%",result:"Không đạt",status:"Chờ xử lý"},{lot:"LT250923-003",product:"Than cám",source:"Kho Than sạch 2",ash:"15,90%",moisture:"8,95%",recovery:"73,20%",result:"Đạt",status:"Đã xác nhận"}]
  },
  "mes-sales-logistics": {
    eyebrow:"Chuỗi tiêu thụ", title:"Tiêu thụ & giao vận", icon:"truck", action:"Tạo lệnh giao",
    description:"Theo dõi hợp đồng, lệnh giao, phương tiện, cân hàng và tiến độ bàn giao khách hàng.", sourceMode:"Nhập liệu/Excel · sẵn sàng ERP/Data Lake",
    tabs:["Tổng quan","Kế hoạch giao","Đang giao","Đã bàn giao","Lịch tàu"],
    kpis:[kpi("Tiêu thụ tháng","432.870","86,6% kế hoạch","green","tấn"),kpi("Đang vận chuyển","18","6 lệnh ưu tiên","blue","lệnh"),kpi("Chờ cân xác nhận","07","2 lệnh quá 2 giờ","amber","lệnh"),kpi("Giao chậm","02","Cần xử lý trong ca","red","lệnh")],
    trendLabel:"Khối lượng giao nhận 7 ngày",trend:[48,52,49,58,61,56,64],
    attention:[attention("Tàu MK-08 chậm cập cảng","Dự kiến chậm 4 giờ","Cao"),attention("Lệnh GH-026 thiếu phiếu cân","Khách hàng Nhiệt điện A","Trung bình")],
    columns:[{key:"order",label:"Lệnh giao"},{key:"customer",label:"Khách hàng"},{key:"product",label:"Sản phẩm"},{key:"vehicle",label:"Phương tiện"},{key:"planned",label:"Kế hoạch",numeric:true},{key:"actual",label:"Đã giao",numeric:true},{key:"eta",label:"Dự kiến"},{key:"status",label:"Trạng thái"}],
    rows:[{order:"GH-260909-014",customer:"Nhiệt điện A",product:"Than sạch",vehicle:"Tàu MK-08",planned:"2.500 tấn",actual:"1.850 tấn",eta:"10/09 08:00",status:"Đang giao"},{order:"GH-260909-015",customer:"Xi măng B",product:"Than cám",vehicle:"29H-621.42",planned:"620 tấn",actual:"620 tấn",eta:"09/09 17:30",status:"Đã bàn giao"},{order:"GH-260909-016",customer:"Cảng Cẩm Phả",product:"Than sạch",vehicle:"14C-246.80",planned:"850 tấn",actual:"—",eta:"09/09 22:00",status:"Chờ cân"}]
  },
  "mes-materials-equipment": {
    eyebrow:"Nguồn lực vận hành", title:"Vật tư & thiết bị", icon:"settings", action:"Tạo yêu cầu vật tư",
    description:"Theo dõi tồn vật tư trọng yếu, tình trạng thiết bị, sửa chữa và thời gian dừng máy.", sourceMode:"Nhập liệu/Excel · sẵn sàng EAM/ERP/Data Lake",
    tabs:["Tổng quan","Vật tư","Thiết bị","Sửa chữa","Dừng máy"],
    kpis:[kpi("Thiết bị sẵn sàng","92,4","+1,2% tuần trước","green","%"),kpi("Lệnh sửa chữa mở","18","5 lệnh ưu tiên","blue"),kpi("Vật tư dưới định mức","07","3 mã mức nghiêm trọng","red"),kpi("Dừng máy tháng","36,5","-4,2 giờ so tháng trước","amber","giờ")],
    trendLabel:"Mức sẵn sàng thiết bị 7 ngày",trend:[88,90,89,91,93,92,92],
    attention:[attention("Băng tải BT-07 dừng đột xuất","Chờ vòng bi thay thế","Cao"),attention("Dầu thủy lực dưới tồn tối thiểu","Kho vật tư trung tâm","Trung bình")],
    columns:[{key:"code",label:"Mã"},{key:"name",label:"Thiết bị/Vật tư"},{key:"organization",label:"Đơn vị"},{key:"quantity",label:"Số lượng/Tình trạng"},{key:"threshold",label:"Định mức"},{key:"owner",label:"Phụ trách"},{key:"updated",label:"Cập nhật"},{key:"status",label:"Trạng thái"}],
    rows:[{code:"TB-BT-07",name:"Băng tải số 07",organization:"PX Sàng tuyển",quantity:"Dừng 2,5 giờ",threshold:"Sẵn sàng ≥ 95%",owner:"Tổ Cơ điện",updated:"09/09 14:20",status:"Đang sửa chữa"},{code:"VT-DTL-01",name:"Dầu thủy lực ISO 46",organization:"Kho vật tư",quantity:"420 lít",threshold:"600 lít",owner:"Phòng Vật tư",updated:"09/09 13:05",status:"Dưới định mức"},{code:"TB-MX-03",name:"Máy xúc MX-03",organization:"PX Khai thác 1",quantity:"Vận hành",threshold:"—",owner:"Tổ Thiết bị",updated:"09/09 14:35",status:"Hoạt động"}]
  },
  "mes-occupational-safety": {
    eyebrow:"An toàn và tuân thủ", title:"An toàn lao động", icon:"shield", action:"Ghi nhận nguy cơ",
    description:"Giám sát sự cố, nguy cơ, hành động khắc phục, kiểm tra hiện trường và huấn luyện.", sourceMode:"Nhập liệu hiện trường/Excel · sẵn sàng HSE/Data Lake",
    tabs:["Tổng quan","Sự cố","Nguy cơ","Hành động khắc phục","Kiểm tra & huấn luyện"],
    kpis:[kpi("Ngày công an toàn","128","Không tai nạn mất ngày công","green","ngày"),kpi("Nguy cơ đang mở","16","5 mức cao","red"),kpi("Hành động đúng hạn","91,2","+3,4% so tháng trước","blue","%"),kpi("Huấn luyện đến hạn","24","Trong 30 ngày tới","amber","người")],
    trendLabel:"Tỷ lệ đóng hành động đúng hạn",trend:[78,82,80,86,88,89,91],
    attention:[attention("Khí CH4 vượt ngưỡng cảnh báo","Khu vực XV-120 lúc 13:42","Cao"),attention("Rào chắn chưa đạt yêu cầu","Tuyến vận tải số 3","Trung bình")],
    columns:[{key:"code",label:"Mã ghi nhận"},{key:"type",label:"Loại"},{key:"location",label:"Khu vực"},{key:"description",label:"Nội dung"},{key:"severity",label:"Mức độ"},{key:"owner",label:"Người xử lý"},{key:"due",label:"Hạn xử lý"},{key:"status",label:"Trạng thái"}],
    rows:[{code:"AT-2609-018",type:"Nguy cơ",location:"Lò XV-120",description:"Khí CH4 vượt ngưỡng",severity:"Cao",owner:"PX Khai thác 1",due:"09/09 16:00",status:"Đang xử lý"},{code:"AT-2609-017",type:"Kiểm tra",location:"Tuyến vận tải 3",description:"Rào chắn chưa đạt",severity:"Trung bình",owner:"PX Vận tải",due:"10/09 10:00",status:"Đã tiếp nhận"},{code:"AT-2609-016",type:"Hành động",location:"Kho vật tư",description:"Bổ sung biển cảnh báo",severity:"Nhắc việc",owner:"Phòng An toàn",due:"09/09 12:00",status:"Đã hoàn thành"}]
  },
  "mes-finance-accounting": {
    eyebrow:"Hiệu quả tài chính", title:"Tài chính & Kế toán", icon:"chart-combined", action:"Nhập số liệu tài chính",
    description:"Cung cấp góc nhìn quản trị về doanh thu, chi phí, công nợ, dòng tiền và giá trị tồn kho.", sourceMode:"Nhập liệu/Excel · sẵn sàng ERP/Data Lake",
    tabs:["Tổng quan","Doanh thu & chi phí","Công nợ","Dòng tiền","Giá trị tồn"],
    kpis:[kpi("Doanh thu tháng","1.248","+6,8% so kế hoạch","green","tỷ"),kpi("Chi phí sản xuất","982","78,7% ngân sách","blue","tỷ"),kpi("Công nợ quá hạn","46,2","8 khách hàng","red","tỷ"),kpi("Dòng tiền ròng","+82,5","Tăng 12,1 tỷ","violet","tỷ")],
    trendLabel:"Doanh thu và chi phí lũy kế",trend:[52,60,65,69,77,83,88],
    attention:[attention("Công nợ Nhiệt điện A quá hạn","18,6 tỷ · quá hạn 12 ngày","Cao"),attention("Chi phí vật tư PX KT1 vượt 5%","Cần giải trình trước ngày 10/09","Trung bình")],
    columns:[{key:"indicator",label:"Chỉ tiêu"},{key:"organization",label:"Đơn vị"},{key:"period",label:"Kỳ"},{key:"plan",label:"Kế hoạch",numeric:true},{key:"actual",label:"Thực hiện",numeric:true},{key:"variance",label:"Chênh lệch",numeric:true},{key:"updated",label:"Cập nhật"},{key:"status",label:"Trạng thái"}],
    rows:[{indicator:"Doanh thu tiêu thụ",organization:"Toàn Công ty",period:"09/2026",plan:"1.168 tỷ",actual:"1.248 tỷ",variance:"+80 tỷ",updated:"09/09 15:00",status:"Đạt"},{indicator:"Chi phí sản xuất",organization:"Toàn Công ty",period:"09/2026",plan:"1.248 tỷ",actual:"982 tỷ",variance:"-266 tỷ",updated:"09/09 15:00",status:"Trong ngân sách"},{indicator:"Công nợ quá hạn",organization:"Phòng TCKT",period:"09/2026",plan:"≤ 30 tỷ",actual:"46,2 tỷ",variance:"+16,2 tỷ",updated:"09/09 14:50",status:"Vượt ngưỡng"}]
  },
  "mes-workforce-labor": {
    eyebrow:"Nguồn nhân lực", title:"Nhân sự & lao động", icon:"users", action:"Thêm nhân sự",
    description:"Quản lý hồ sơ nhân sự và theo dõi chức danh, đơn vị, ca công, năng suất, đào tạo; tách biệt với tài khoản đăng nhập Core.", sourceMode:"Nhập liệu/Excel · sẵn sàng HRM/Data Lake",
    tabs:["Danh sách nhân sự","Hồ sơ nhân sự","Chức danh và đơn vị","Phân ca và ngày công","Năng suất lao động","Đào tạo và chứng chỉ"],
    tabActions:{
      "Danh sách nhân sự":"Thêm nhân sự",
      "Hồ sơ nhân sự":"Bổ sung hồ sơ",
      "Chức danh và đơn vị":"Thêm chức danh và định biên",
      "Phân ca và ngày công":"Ghi nhận ca và ngày công",
      "Năng suất lao động":"Ghi nhận năng suất",
      "Đào tạo và chứng chỉ":"Thêm đào tạo hoặc chứng chỉ"
    },
    kpis:[kpi("Lao động hiện có","3.842","98,2% định biên","blue","người"),kpi("Có mặt hôm nay","3.516","91,5% tổng lao động","green","người"),kpi("Năng suất bình quân","6,99","+4,3% cùng kỳ","violet","tấn/công"),kpi("Thiếu vị trí trọng yếu","12","4 đơn vị bị ảnh hưởng","red","vị trí")],
    trendLabel:"Năng suất lao động 7 kỳ",trend:[5.8,6.1,6.0,6.4,6.6,6.8,7.0],
    attention:[attention("Thiếu thợ lò bậc cao ca 3","PX Khai thác 2 thiếu 7 người","Cao"),attention("24 chứng chỉ sắp hết hạn","Huấn luyện lại trong 30 ngày","Trung bình")],
    columns:[{key:"organization",label:"Đơn vị"},{key:"headcount",label:"Hiện có",numeric:true},{key:"present",label:"Có mặt",numeric:true},{key:"absent",label:"Vắng",numeric:true},{key:"output",label:"Sản lượng",numeric:true},{key:"productivity",label:"Năng suất"},{key:"manager",label:"Phụ trách"},{key:"status",label:"Trạng thái"}],
    tabColumns:{
      "Danh sách nhân sự":[{key:"employeeCode",label:"Mã nhân sự"},{key:"fullName",label:"Họ và tên"},{key:"organization",label:"Đơn vị"},{key:"position",label:"Chức danh"},{key:"startDate",label:"Ngày bắt đầu"},{key:"endDate",label:"Ngày kết thúc"},{key:"status",label:"Trạng thái"}],
      "Hồ sơ nhân sự":[{key:"employeeCode",label:"Mã nhân sự"},{key:"fullName",label:"Họ và tên"},{key:"profileType",label:"Loại hồ sơ"},{key:"contact",label:"Liên hệ"},{key:"contractType",label:"Hợp đồng"},{key:"updated",label:"Cập nhật"},{key:"status",label:"Trạng thái"}],
      "Chức danh và đơn vị":[{key:"positionCode",label:"Mã chức danh"},{key:"position",label:"Chức danh"},{key:"organization",label:"Đơn vị"},{key:"headcount",label:"Hiện có",numeric:true},{key:"quota",label:"Định biên",numeric:true},{key:"manager",label:"Phụ trách"},{key:"status",label:"Trạng thái"}],
      "Phân ca và ngày công":[{key:"date",label:"Ngày"},{key:"shift",label:"Ca"},{key:"organization",label:"Đơn vị"},{key:"planned",label:"Kế hoạch",numeric:true},{key:"present",label:"Có mặt",numeric:true},{key:"absent",label:"Vắng",numeric:true},{key:"manager",label:"Phụ trách"},{key:"status",label:"Trạng thái"}],
      "Năng suất lao động":[{key:"organization",label:"Đơn vị"},{key:"period",label:"Kỳ"},{key:"output",label:"Sản lượng",numeric:true},{key:"workdays",label:"Ngày công",numeric:true},{key:"productivity",label:"Năng suất"},{key:"manager",label:"Phụ trách"},{key:"status",label:"Trạng thái"}],
      "Đào tạo và chứng chỉ":[{key:"employeeCode",label:"Mã nhân sự"},{key:"fullName",label:"Họ và tên"},{key:"course",label:"Khóa học/Chứng chỉ"},{key:"issued",label:"Ngày cấp"},{key:"expires",label:"Hết hạn"},{key:"owner",label:"Đơn vị quản lý"},{key:"status",label:"Trạng thái"}]
    },
    rows:[{organization:"PX Khai thác 1",headcount:486,present:452,absent:34,output:"8.440 tấn",productivity:"18,67 tấn/công",manager:"Nguyễn Văn Hùng",status:"Đủ nguồn lực"},{organization:"PX Khai thác 2",headcount:472,present:421,absent:51,output:"7.810 tấn",productivity:"18,55 tấn/công",manager:"Trần Văn Nam",status:"Thiếu ca 3"},{organization:"PX Đào lò 2",headcount:318,present:291,absent:27,output:"23 m",productivity:"0,079 m/công",manager:"Lê Văn Bình",status:"Đang theo dõi"}]
  },
  "mes-investment-projects": {
    eyebrow:"Đầu tư phát triển", title:"Đầu tư & dự án", icon:"folder", action:"Cập nhật tiến độ",
    description:"Theo dõi danh mục đầu tư, mốc tiến độ, giải ngân, vướng mắc và hiệu quả dự kiến.", sourceMode:"Nhập liệu/Excel · sẵn sàng PMIS/Data Lake",
    tabs:["Danh mục dự án","Tiến độ","Giải ngân","Vướng mắc","Hiệu quả đầu tư"],
    kpis:[kpi("Dự án đang triển khai","18","4 dự án trọng điểm","blue"),kpi("Giải ngân năm","68,4","+6,2% so tháng trước","green","%"),kpi("Mốc chậm tiến độ","05","2 mốc ảnh hưởng đường găng","red"),kpi("Vướng mắc mở","11","3 việc chờ cấp TKV","amber")],
    trendLabel:"Tỷ lệ giải ngân lũy kế",trend:[18,26,31,40,49,58,68],
    attention:[attention("Dự án mở rộng mức -250 chậm 18 ngày","Vướng phê duyệt thiết kế kỹ thuật","Cao"),attention("Gói TB-04 chậm bàn giao","Nhà thầu đề nghị lùi 7 ngày","Trung bình")],
    columns:[{key:"code",label:"Mã dự án"},{key:"name",label:"Tên dự án"},{key:"budget",label:"Tổng mức đầu tư",numeric:true},{key:"progress",label:"Tiến độ"},{key:"disbursement",label:"Giải ngân"},{key:"milestone",label:"Mốc gần nhất"},{key:"owner",label:"Chủ trì"},{key:"status",label:"Trạng thái"}],
    rows:[{code:"DA-2026-01",name:"Mở rộng khai thác mức -250",budget:"1.250 tỷ",progress:"64%",disbursement:"58%",milestone:"Nghiệm thu hạng mục 2",owner:"Ban QLDA",status:"Chậm tiến độ"},{code:"DA-2026-04",name:"Nâng cấp băng tải trung tâm",budget:"186 tỷ",progress:"82%",disbursement:"78%",milestone:"Chạy thử liên động",owner:"Phòng Cơ điện",status:"Đang thực hiện"},{code:"DA-2026-07",name:"Số hóa giám sát an toàn",budget:"42 tỷ",progress:"91%",disbursement:"86%",milestone:"Nghiệm thu hệ thống",owner:"Phòng KHCN",status:"Đúng tiến độ"}]
  },
  "mes-science-digital": {
    eyebrow:"Đổi mới và số hóa", title:"KHCN & Chuyển đổi số", icon:"cpu", action:"Tạo nhiệm vụ",
    description:"Theo dõi nhiệm vụ KHCN, sáng kiến, sản phẩm số, tích hợp và mức độ ứng dụng.", sourceMode:"Nhập liệu/Excel · sẵn sàng hệ sinh thái số",
    tabs:["Nhiệm vụ KHCN","Sáng kiến","Sản phẩm số","Tích hợp","Hiệu quả ứng dụng"],
    kpis:[kpi("Nhiệm vụ đang triển khai","26","8 nhiệm vụ trọng điểm","blue"),kpi("Sáng kiến áp dụng","14","Tiết kiệm 18,6 tỷ","green"),kpi("Quy trình đã số hóa","62","+7 quy trình trong quý","violet","%"),kpi("Tích hợp chưa ổn định","03","Cần khắc phục trong tuần","red")],
    trendLabel:"Mức độ hoàn thành chương trình số",trend:[32,39,43,48,53,58,62],
    attention:[attention("Tích hợp cân điện tử gián đoạn","Mất đồng bộ 47 bản ghi","Cao"),attention("Nhiệm vụ KHCN-08 chờ nghiệm thu","Hồ sơ thiếu biên bản thử nghiệm","Trung bình")],
    columns:[{key:"code",label:"Mã"},{key:"name",label:"Nhiệm vụ/Sản phẩm"},{key:"type",label:"Nhóm"},{key:"progress",label:"Tiến độ"},{key:"benefit",label:"Hiệu quả dự kiến"},{key:"owner",label:"Chủ trì"},{key:"milestone",label:"Mốc tiếp theo"},{key:"status",label:"Trạng thái"}],
    rows:[{code:"KHCN-2026-08",name:"Tối ưu vận tải bằng mô hình số",type:"Nhiệm vụ KHCN",progress:"78%",benefit:"Giảm 6% thời gian chờ",owner:"Phòng KHCN",milestone:"Nghiệm thu 20/09",status:"Đang thực hiện"},{code:"CDS-2026-03",name:"Trung tâm điều hành MES",type:"Chuyển đổi số",progress:"42%",benefit:"Điều hành theo dữ liệu",owner:"Ban CĐS",milestone:"UAT Slice 1",status:"Đang thực hiện"},{code:"SK-2026-12",name:"Cải tiến sàng tuyển tuyến 2",type:"Sáng kiến",progress:"100%",benefit:"Tiết kiệm 3,2 tỷ/năm",owner:"PX Sàng tuyển",milestone:"Đánh giá hiệu quả",status:"Đã áp dụng"}]
  },
  "mes-alerts-directives": {
    eyebrow:"Điều hành theo ngoại lệ", title:"Cảnh báo & chỉ đạo", icon:"bell", action:"Ban hành chỉ đạo",
    description:"Tập trung cảnh báo liên phòng ban, giao chỉ đạo, hạn xử lý và bằng chứng hoàn thành.", sourceMode:"Tổng hợp từ các phân hệ MES",
    tabs:["Cần xử lý","Đang xử lý","Chờ xác nhận","Đã đóng","Quy tắc cảnh báo"],
    kpis:[kpi("Cảnh báo đang mở","18","5 cảnh báo mức cao","red"),kpi("Chỉ đạo đang thực hiện","12","9 đúng hạn","blue"),kpi("Đóng đúng hạn","88,6","+4,1% so tháng trước","green","%"),kpi("Quá hạn","04","Lâu nhất 3 ngày","amber")],
    trendLabel:"Tỷ lệ xử lý đúng hạn",trend:[76,79,81,80,84,86,89],
    attention:[attention("Chậm tiến độ mét lò","Phòng Kế hoạch chưa nhận giải trình","Cao"),attention("Công nợ vượt ngưỡng","Chỉ đạo số 118/CĐ-MK","Trung bình")],
    columns:[{key:"code",label:"Mã"},{key:"type",label:"Loại"},{key:"title",label:"Nội dung"},{key:"source",label:"Phân hệ nguồn"},{key:"severity",label:"Mức độ"},{key:"owner",label:"Đơn vị xử lý"},{key:"due",label:"Hạn xử lý"},{key:"status",label:"Trạng thái"}],
    rows:[{code:"CB-2609-041",type:"Cảnh báo",title:"Mét lò chậm 12,4%",source:"Kế hoạch & hiệu quả",severity:"Cao",owner:"PX Đào lò 2",due:"09/09 16:00",status:"Đang xử lý"},{code:"CĐ-2609-118",type:"Chỉ đạo",title:"Xử lý công nợ quá hạn",source:"Tài chính & Kế toán",severity:"Trung bình",owner:"Phòng TCKT",due:"11/09 17:00",status:"Đã tiếp nhận"},{code:"CB-2609-039",type:"Cảnh báo",title:"Thiếu dữ liệu ca 3",source:"Sản xuất & điều hành",severity:"Nhắc việc",owner:"PX Khai thác 1",due:"09/09 15:30",status:"Chờ xác nhận"}]
  },
  "mes-esg": {
    eyebrow:"Phát triển bền vững", title:"ESG", icon:"leaf", action:"Nhập chỉ tiêu ESG",
    description:"Theo dõi chỉ tiêu môi trường, xã hội, quản trị, mục tiêu giảm phát thải và hồ sơ bằng chứng.", sourceMode:"Nhập liệu/Excel · sẵn sàng IoT/Data Lake",
    tabs:["Tổng quan","Môi trường","Xã hội","Quản trị","Mục tiêu & bằng chứng"],
    kpis:[kpi("Phát thải quy đổi","18.420","-4,8% cùng kỳ","green","tCO₂e"),kpi("Nước tái sử dụng","72,6","+6,1% so mục tiêu","blue","%"),kpi("Phục hồi môi trường","86,2","Đạt tiến độ năm","violet","%"),kpi("Chỉ tiêu rủi ro","05","2 chỉ tiêu mức cao","red")],
    trendLabel:"Cường độ phát thải theo tháng",trend:[98,95,94,91,89,87,84],
    attention:[attention("Bụi khu vực kho than vượt ngưỡng","Kết quả quan trắc lúc 13:30","Cao"),attention("Thiếu hồ sơ tái sử dụng nước","Báo cáo tháng 08/2026","Trung bình")],
    columns:[{key:"code",label:"Mã chỉ tiêu"},{key:"pillar",label:"Trụ cột"},{key:"indicator",label:"Chỉ tiêu"},{key:"target",label:"Mục tiêu",numeric:true},{key:"actual",label:"Thực hiện",numeric:true},{key:"unit",label:"ĐVT"},{key:"evidence",label:"Bằng chứng"},{key:"status",label:"Trạng thái"}],
    rows:[{code:"E-01",pillar:"Môi trường",indicator:"Phát thải khí nhà kính",target:"≤ 19.500",actual:"18.420",unit:"tCO₂e",evidence:"Đủ 9/9 tháng",status:"Đạt"},{code:"E-04",pillar:"Môi trường",indicator:"Nước tái sử dụng",target:"68",actual:"72,6",unit:"%",evidence:"Thiếu tháng 08",status:"Cần bổ sung"},{code:"S-02",pillar:"Xã hội",indicator:"Tỷ lệ huấn luyện an toàn",target:"100",actual:"96,8",unit:"%",evidence:"3.718 hồ sơ",status:"Đang thực hiện"}]
  },
  "mes-portal-tkv": {
    eyebrow:"Liên thông báo cáo", title:"Portal/TKV", icon:"webhook", action:"Kiểm tra kết nối", actionDisabled:true,
    description:"Chuẩn bị bộ dữ liệu chuẩn và theo dõi trạng thái liên thông Portal/TKV; chưa phát sinh gửi khi API chưa cấu hình.", sourceMode:"Tích hợp Portal: Chưa cấu hình",
    tabs:["Trạng thái","Sẵn sàng dữ liệu","Lịch sử gửi","Hợp đồng API"],
    kpis:[kpi("Bộ báo cáo đã ánh xạ","12","12/18 mẫu pilot","blue"),kpi("Sẵn sàng gửi","08","Đã qua kiểm tra dữ liệu","green"),kpi("Cần bổ sung dữ liệu","04","Có lỗi bắt buộc","amber"),kpi("Lần gửi API","0","API Portal chưa cấu hình","violet")],
    trendLabel:"Mức độ sẵn sàng liên thông",trend:[18,26,35,44,52,61,67],
    attention:[attention("Chưa cấu hình endpoint Portal","Hệ thống đang ở chế độ an toàn, không gửi dữ liệu","Cao"),attention("04 bộ báo cáo thiếu ánh xạ","Hoàn thiện trước khi kiểm thử API","Trung bình")],
    columns:[{key:"report",label:"Bộ báo cáo"},{key:"period",label:"Kỳ"},{key:"contract",label:"Phiên bản contract"},{key:"records",label:"Bản ghi",numeric:true},{key:"validation",label:"Kiểm tra dữ liệu"},{key:"approvedBy",label:"Phê duyệt"},{key:"lastAttempt",label:"Lần gửi gần nhất"},{key:"status",label:"Trạng thái"}],
    rows:[{report:"Chỉ tiêu chủ yếu",period:"09/2026",contract:"Chưa cấu hình",records:126,validation:"Hợp lệ",approvedBy:"Đã duyệt nội bộ",lastAttempt:"—",status:"Sẵn sàng nội bộ"},{report:"Sản lượng theo ca",period:"09/09/2026",contract:"Chưa cấu hình",records:42,validation:"Thiếu 3 dòng",approvedBy:"Chờ duyệt",lastAttempt:"—",status:"Cần bổ sung"},{report:"Tồn kho cuối ngày",period:"09/09/2026",contract:"Chưa cấu hình",records:18,validation:"Hợp lệ",approvedBy:"Đã duyệt nội bộ",lastAttempt:"—",status:"Sẵn sàng nội bộ"}]
  },
  "mes-integration-quality": {
    eyebrow:"Quản trị dữ liệu", title:"Tích hợp & chất lượng dữ liệu", icon:"server", action:"Đăng ký nguồn dữ liệu",
    description:"Theo dõi nguồn, job đồng bộ, quy tắc chất lượng, lineage và sự cố dữ liệu xuyên hệ thống.", sourceMode:"Giai đoạn 1 Excel/API · Giai đoạn 2 Data Lake",
    tabs:["Nguồn dữ liệu","Job đồng bộ","Chất lượng","Lineage","Sự cố tích hợp"],
    kpis:[kpi("Nguồn đang quản lý","14","9 Excel · 5 API nội bộ","blue"),kpi("Tỷ lệ dữ liệu đạt","96,4","+1,8% so tuần trước","green","%"),kpi("Quy tắc đang chạy","86","12 quy tắc trọng yếu","violet"),kpi("Sự cố đang mở","04","1 sự cố mức cao","red")],
    trendLabel:"Điểm chất lượng dữ liệu 7 ngày",trend:[89,91,90,93,94,95,96],
    attention:[attention("Nguồn cân điện tử mất đồng bộ","47 bản ghi chờ xử lý","Cao"),attention("Mã kho K-TT02 chưa ánh xạ","Ảnh hưởng báo cáo tồn kho","Trung bình")],
    columns:[{key:"source",label:"Nguồn dữ liệu"},{key:"type",label:"Loại"},{key:"domain",label:"Phân hệ"},{key:"lastSync",label:"Đồng bộ gần nhất"},{key:"records",label:"Bản ghi",numeric:true},{key:"quality",label:"Điểm chất lượng"},{key:"owner",label:"Chủ dữ liệu"},{key:"status",label:"Trạng thái"}],
    rows:[{source:"Excel sản lượng ca",type:"Workbook",domain:"Sản xuất & điều hành",lastSync:"09/09 14:35",records:126,quality:"98,4%",owner:"Phòng Điều độ",status:"Hoạt động"},{source:"Cân điện tử WB-01",type:"API nội bộ",domain:"Tiêu thụ & giao vận",lastSync:"09/09 13:48",records:824,quality:"91,2%",owner:"Phòng Tiêu thụ",status:"Gián đoạn"},{source:"Excel KCS ngày",type:"Workbook",domain:"Chất lượng & nghiệm thu",lastSync:"09/09 14:10",records:128,quality:"97,6%",owner:"Phòng KCS",status:"Hoạt động"}]
  }
};
