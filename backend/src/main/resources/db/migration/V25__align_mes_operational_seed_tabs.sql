-- Căn chỉnh dữ liệu khởi tạo với tab canonical của frontend.
-- Chỉ thay các dòng seed; dữ liệu người dùng tạo không bị tác động.
delete from mes.operational_record
where module_code in ('planning-performance','production-operations','quality-acceptance','sales-logistics')
  and details @> '{"seeded":true}'::jsonb;

with module_tab(module_code,prefix,tab_name) as (values
  ('planning-performance','KH','Tổng quan'),
  ('planning-performance','KH','Kế hoạch'),
  ('planning-performance','KH','Dự kiến'),
  ('planning-performance','KH','Hiệu quả'),
  ('planning-performance','KH','Phiên bản & phê duyệt'),
  ('production-operations','SX','Tổng quan ca'),
  ('production-operations','SX','Sản lượng ngày'),
  ('production-operations','SX','Bàn giao ca'),
  ('production-operations','SX','Giải trình sai lệch'),
  ('quality-acceptance','KCS','Tổng quan'),
  ('quality-acceptance','KCS','Kết quả KCS'),
  ('quality-acceptance','KCS','Giám định'),
  ('quality-acceptance','KCS','Nghiệm thu'),
  ('quality-acceptance','KCS','Sai lệch'),
  ('sales-logistics','GH','Tổng quan'),
  ('sales-logistics','GH','Kế hoạch giao'),
  ('sales-logistics','GH','Đang giao'),
  ('sales-logistics','GH','Đã bàn giao'),
  ('sales-logistics','GH','Lịch tàu')
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
