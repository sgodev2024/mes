-- Chuẩn hóa 6 lát cắt Nhân sự & lao động. Chỉ thay dữ liệu seed, bảo toàn bản ghi người dùng.
delete from mes.operational_record
where module_code='workforce-labor' and details @> '{"seeded":true}'::jsonb;

with workforce_tab(tab_name,prefix) as (values
  ('Danh sách nhân sự','NV'),
  ('Hồ sơ nhân sự','HS'),
  ('Chức danh và đơn vị','CD'),
  ('Phân ca và ngày công','CA'),
  ('Năng suất lao động','NS'),
  ('Đào tạo và chứng chỉ','DT')
), seeded as (
  select tenant.id tenant_id,workforce_tab.*,series.no,
    row_number() over(partition by tenant.id order by workforce_tab.tab_name,series.no) sequence_no
  from platform.tenant tenant cross join workforce_tab cross join generate_series(1,3) series(no)
)
insert into mes.operational_record(
  tenant_id,module_code,tab_name,record_code,title,organization_code,period_key,
  metric_value,target_value,unit_code,owner_name,severity,source_type,status,
  correlation_key,details,occurred_on,due_on)
select tenant_id,'workforce-labor',tab_name,prefix||'-'||lpad(sequence_no::text,3,'0'),
  case tab_name
    when 'Danh sách nhân sự' then (array['Nguyễn Văn Hùng','Trần Văn Nam','Lê Văn Bình'])[no]
    when 'Hồ sơ nhân sự' then (array['Nguyễn Văn Hùng','Trần Văn Nam','Lê Văn Bình'])[no]
    when 'Chức danh và đơn vị' then (array['QUẢN ĐỐC PHÂN XƯỞNG','KỸ SƯ KHAI THÁC','THỢ LÒ BẬC 5/7'])[no]
    when 'Phân ca và ngày công' then (array['Ca 1 · Khai thác','Ca 2 · Khai thác','Ca 3 · Đào lò'])[no]
    when 'Năng suất lao động' then (array['Năng suất PX Khai thác 1','Năng suất PX Khai thác 2','Năng suất PX Đào lò 2'])[no]
    else (array['An toàn vệ sinh lao động','Vận hành thiết bị hầm lò','Chứng chỉ thợ lò bậc cao'])[no]
  end,
  (array['PX-KT1','PX-KT2','PX-DL2'])[no],to_char(current_date,'MM/YYYY'),
  (array[452,421,291])[no],(array[486,472,318])[no],
  case when tab_name='Năng suất lao động' then 'TON_PER_WORKDAY' else 'PERSON' end,
  (array['Nguyễn Văn Hùng','Trần Văn Nam','Lê Văn Bình'])[no],
  case no when 1 then 'LOW' when 2 then 'MEDIUM' else 'HIGH' end,
  case no when 1 then 'MANUAL' when 2 then 'EXCEL' else 'API' end,
  case no when 1 then 'APPROVED' when 2 then 'IN_PROGRESS' else 'PENDING_APPROVAL' end,
  'MK-HR-'||to_char(current_date,'YYYYMMDD')||'-'||prefix||'-'||no,
  jsonb_build_object('note','Dữ liệu nhân sự khởi tạo phục vụ vận hành MES','seeded',true),
  current_date-(no*30),case when no=3 then current_date+25 else current_date+(365*no) end
from seeded
on conflict(tenant_id,module_code,record_code) do nothing;
