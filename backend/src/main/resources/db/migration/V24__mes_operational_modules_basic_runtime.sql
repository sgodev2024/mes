create table mes.operational_record(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  module_code varchar(80) not null,
  tab_name varchar(120) not null,
  record_code varchar(100) not null,
  title varchar(300) not null,
  organization_code varchar(80) not null,
  period_key varchar(30) not null,
  metric_value numeric(20,4),
  target_value numeric(20,4),
  unit_code varchar(40),
  owner_name varchar(160),
  severity varchar(20) not null check(severity in ('LOW','MEDIUM','HIGH','CRITICAL')),
  source_type varchar(30) not null check(source_type in ('MANUAL','EXCEL','API','DATALAKE')),
  status varchar(30) not null check(status in ('DRAFT','IN_PROGRESS','PENDING_APPROVAL','APPROVED','CLOSED','REJECTED')),
  correlation_key varchar(120) not null,
  details jsonb not null default '{}'::jsonb,
  version int not null default 1 check(version > 0),
  occurred_on date not null,
  due_on date,
  created_by uuid,
  updated_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(tenant_id,module_code,record_code),
  foreign key(created_by,tenant_id) references identity.account(id,tenant_id),
  foreign key(updated_by,tenant_id) references identity.account(id,tenant_id)
);

create index mes_operational_record_scope_idx
  on mes.operational_record(tenant_id,module_code,tab_name,period_key,status,occurred_on desc);
create index mes_operational_record_correlation_idx
  on mes.operational_record(tenant_id,correlation_key,module_code);

alter table mes.operational_record enable row level security;
alter table mes.operational_record force row level security;
create policy tenant_isolation on mes.operational_record
  using (tenant_id=platform.current_tenant_id())
  with check (tenant_id=platform.current_tenant_id());

do $$
begin
  if exists(select 1 from pg_roles where rolname='core_app') then
    grant usage on schema mes to core_app;
    grant select,insert,update,delete on mes.operational_record to core_app;
  end if;
end $$;

insert into identity.policy(tenant_id,code,resource_type,action,effect,condition_json)
select tenant.id,policy.code,'MES_OPERATION',policy.action,'ALLOW','{}'::jsonb
from platform.tenant tenant
cross join (values
  ('mes-operation-read','READ'),
  ('mes-operation-create','CREATE'),
  ('mes-operation-update','UPDATE'),
  ('mes-operation-approve','APPROVE')
) as policy(code,action)
on conflict(tenant_id,code,version) do nothing;

insert into identity.role_policy(tenant_id,role_id,policy_id)
select role.tenant_id,role.id,policy.id
from identity.role role
join identity.policy policy on policy.tenant_id=role.tenant_id
where (role.code='mes-report-viewer' and policy.code='mes-operation-read')
   or (role.code='mes-report-preparer' and policy.code in ('mes-operation-read','mes-operation-create','mes-operation-update'))
   or (role.code='mes-report-approver' and policy.code in ('mes-operation-read','mes-operation-create','mes-operation-update','mes-operation-approve'))
on conflict do nothing;

with module_tab(module_code,prefix,tab_name) as (values
  ('planning-performance','KH','Tổng quan'),('planning-performance','KH','Kế hoạch năm'),('planning-performance','KH','Kế hoạch tháng'),('planning-performance','KH','Tiến độ'),('planning-performance','KH','Dự báo'),
  ('production-operations','SX','Điều hành ngày'),('production-operations','SX','Theo ca'),('production-operations','SX','Sản lượng'),('production-operations','SX','Mét lò'),('production-operations','SX','Nhật ký vận hành'),
  ('quality-acceptance','KCS','Kết quả KCS'),('quality-acceptance','KCS','Giám định'),('quality-acceptance','KCS','Xác nhận chất lượng'),('quality-acceptance','KCS','Ngoại lệ'),
  ('sales-logistics','GH','Kế hoạch tiêu thụ'),('sales-logistics','GH','Đơn hàng'),('sales-logistics','GH','Giao vận'),('sales-logistics','GH','Phương tiện'),('sales-logistics','GH','Đối soát'),
  ('materials-equipment','VT','Tổng quan'),('materials-equipment','VT','Vật tư'),('materials-equipment','VT','Thiết bị'),('materials-equipment','VT','Sửa chữa'),('materials-equipment','VT','Dừng máy'),
  ('occupational-safety','AT','Tổng quan'),('occupational-safety','AT','Sự cố'),('occupational-safety','AT','Nguy cơ'),('occupational-safety','AT','Hành động khắc phục'),('occupational-safety','AT','Kiểm tra & huấn luyện'),
  ('finance-accounting','TC','Tổng quan'),('finance-accounting','TC','Doanh thu & chi phí'),('finance-accounting','TC','Công nợ'),('finance-accounting','TC','Dòng tiền'),('finance-accounting','TC','Giá trị tồn'),
  ('workforce-labor','NS','Cơ cấu'),('workforce-labor','NS','Lao động ca'),('workforce-labor','NS','Ngày công'),('workforce-labor','NS','Năng suất'),('workforce-labor','NS','Đào tạo'),
  ('investment-projects','DA','Danh mục dự án'),('investment-projects','DA','Tiến độ'),('investment-projects','DA','Giải ngân'),('investment-projects','DA','Vướng mắc'),('investment-projects','DA','Hiệu quả đầu tư'),
  ('science-digital','KHCN','Nhiệm vụ KHCN'),('science-digital','KHCN','Sáng kiến'),('science-digital','KHCN','Sản phẩm số'),('science-digital','KHCN','Tích hợp'),('science-digital','KHCN','Hiệu quả ứng dụng'),
  ('alerts-directives','CD','Cần xử lý'),('alerts-directives','CD','Đang xử lý'),('alerts-directives','CD','Chờ xác nhận'),('alerts-directives','CD','Đã đóng'),('alerts-directives','CD','Quy tắc cảnh báo'),
  ('esg','ESG','Tổng quan'),('esg','ESG','Môi trường'),('esg','ESG','Xã hội'),('esg','ESG','Quản trị'),('esg','ESG','Mục tiêu & bằng chứng'),
  ('portal-tkv','TKV','Trạng thái'),('portal-tkv','TKV','Sẵn sàng dữ liệu'),('portal-tkv','TKV','Lịch sử gửi'),('portal-tkv','TKV','Hợp đồng API'),
  ('integration-quality','DL','Nguồn dữ liệu'),('integration-quality','DL','Job đồng bộ'),('integration-quality','DL','Chất lượng'),('integration-quality','DL','Lineage'),('integration-quality','DL','Sự cố tích hợp')
), seeded as (
  select tenant.id tenant_id,module_tab.*,series.no,
    row_number() over(partition by tenant.id,module_tab.module_code order by module_tab.tab_name,series.no) sequence_no
  from platform.tenant tenant cross join module_tab cross join generate_series(1,3) series(no)
)
insert into mes.operational_record(
  tenant_id,module_code,tab_name,record_code,title,organization_code,period_key,
  metric_value,target_value,unit_code,owner_name,severity,source_type,status,
  correlation_key,details,occurred_on,due_on)
select tenant_id,module_code,tab_name,prefix||'-'||lpad(sequence_no::text,3,'0'),
  tab_name||' · bản ghi '||no,
  case no when 1 then 'TOAN-CONG-TY' when 2 then 'PX-KT1' else 'PHONG-KH' end,
  to_char(current_date,'MM/YYYY'),
  68+sequence_no,100,'PERCENT',
  case no when 1 then 'Phòng Điều độ' when 2 then 'Phân xưởng Khai thác 1' else 'Phòng Kế hoạch' end,
  case no when 1 then 'HIGH' when 2 then 'MEDIUM' else 'LOW' end,
  case no when 1 then 'MANUAL' when 2 then 'EXCEL' else 'API' end,
  case no when 1 then 'IN_PROGRESS' when 2 then 'PENDING_APPROVAL' else 'APPROVED' end,
  'MK-'||to_char(current_date,'YYYYMMDD')||'-'||no,
  jsonb_build_object('note','Dữ liệu vận hành khởi tạo để kiểm thử luồng MES','seeded',true),
  current_date-(no-1),current_date+(4-no)
from seeded
on conflict(tenant_id,module_code,record_code) do nothing;

update identity.permission_revision set revision=revision+1,updated_at=now();
