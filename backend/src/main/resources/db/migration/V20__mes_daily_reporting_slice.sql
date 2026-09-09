create schema if not exists mes;

create table mes.template_definition(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  template_code varchar(100) not null,
  name varchar(240) not null,
  department varchar(160) not null,
  frequency varchar(20) not null check(frequency in ('DAILY','MONTHLY','QUARTERLY','YEARLY','AD_HOC')),
  filename_pattern varchar(300) not null,
  worksheet_name varchar(120) not null,
  parser_type varchar(30) not null check(parser_type in ('TABULAR','MULTI_HEADER','MATRIX','REFERENCE')),
  reporting_entity_code varchar(30) not null default '4100',
  header_tokens jsonb not null default '[]'::jsonb,
  version int not null default 1 check(version > 0),
  due_time time not null default time '17:00',
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(tenant_id, template_code, version)
);

create table mes.import_batch(
  id uuid primary key,
  tenant_id uuid not null references platform.tenant(id),
  template_id uuid references mes.template_definition(id),
  file_id uuid not null references files.file_object(id),
  duplicate_of uuid references mes.import_batch(id),
  reporting_date date not null,
  period_key varchar(30) not null,
  original_name varchar(255) not null,
  checksum_sha256 char(64) not null,
  status varchar(40) not null check(status in (
    'UPLOADED','IDENTIFYING','IDENTIFIED','VALIDATING','VALIDATED','PENDING_CONFIRMATION',
    'PENDING_APPROVAL','APPROVED','LOCKED','UNKNOWN_TEMPLATE','INVALID','REJECTED',
    'SUPERSEDED','CANCELLED')),
  total_rows int not null default 0 check(total_rows >= 0),
  valid_rows int not null default 0 check(valid_rows >= 0),
  error_rows int not null default 0 check(error_rows >= 0),
  warning_rows int not null default 0 check(warning_rows >= 0),
  submitted_by uuid not null references identity.account(id),
  confirmed_by uuid references identity.account(id),
  approved_by uuid references identity.account(id),
  rejected_reason varchar(500),
  version int not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  confirmed_at timestamptz,
  approved_at timestamptz,
  locked_at timestamptz
);

create index mes_import_batch_tenant_date_idx
  on mes.import_batch(tenant_id, reporting_date desc, created_at desc);
create index mes_import_batch_checksum_idx
  on mes.import_batch(tenant_id, checksum_sha256);

create table mes.import_record(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  batch_id uuid not null references mes.import_batch(id) on delete cascade,
  source_sheet varchar(120) not null,
  source_row int not null check(source_row > 0),
  record_status varchar(20) not null check(record_status in ('VALID','WARNING','ERROR')),
  payload jsonb not null,
  created_at timestamptz not null default now(),
  unique(tenant_id, batch_id, source_sheet, source_row)
);

create index mes_import_record_batch_idx on mes.import_record(tenant_id, batch_id, source_row);

create table mes.import_issue(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  batch_id uuid not null references mes.import_batch(id) on delete cascade,
  severity varchar(20) not null check(severity in ('ERROR','WARNING')),
  issue_code varchar(80) not null,
  source_sheet varchar(120),
  source_row int,
  source_cell varchar(30),
  field_name varchar(160),
  raw_value varchar(1000),
  message varchar(1000) not null,
  created_at timestamptz not null default now()
);

create index mes_import_issue_batch_idx on mes.import_issue(tenant_id, batch_id, severity, source_row);

create table mes.reporting_calendar_item(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  template_id uuid not null references mes.template_definition(id),
  reporting_date date not null,
  period_key varchar(30) not null,
  due_at timestamptz not null,
  status varchar(40) not null check(status in (
    'NOT_SUBMITTED','OVERDUE','PROCESSING','INVALID','PENDING_CONFIRMATION','PENDING_APPROVAL',
    'APPROVED','LOCKED','PORTAL_SUBMITTED','PORTAL_ACCEPTED','PORTAL_REJECTED')),
  current_batch_id uuid references mes.import_batch(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(tenant_id, template_id, reporting_date)
);

create index mes_reporting_calendar_tenant_date_idx
  on mes.reporting_calendar_item(tenant_id, reporting_date desc, due_at, status);

create table mes.portal_submission(
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references platform.tenant(id),
  calendar_item_id uuid not null references mes.reporting_calendar_item(id),
  batch_id uuid not null references mes.import_batch(id),
  attempt_no int not null check(attempt_no > 0),
  status varchar(30) not null check(status in ('SUBMITTED','ACCEPTED','REJECTED')),
  receipt_code varchar(160),
  note varchar(1000),
  submitted_by uuid not null references identity.account(id),
  submitted_at timestamptz not null default now(),
  unique(tenant_id, calendar_item_id, attempt_no)
);

create index mes_portal_submission_calendar_idx
  on mes.portal_submission(tenant_id, calendar_item_id, attempt_no desc);

alter table mes.template_definition enable row level security;
alter table mes.template_definition force row level security;
create policy tenant_isolation on mes.template_definition
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.import_batch enable row level security;
alter table mes.import_batch force row level security;
create policy tenant_isolation on mes.import_batch
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.import_record enable row level security;
alter table mes.import_record force row level security;
create policy tenant_isolation on mes.import_record
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.import_issue enable row level security;
alter table mes.import_issue force row level security;
create policy tenant_isolation on mes.import_issue
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.reporting_calendar_item enable row level security;
alter table mes.reporting_calendar_item force row level security;
create policy tenant_isolation on mes.reporting_calendar_item
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

alter table mes.portal_submission enable row level security;
alter table mes.portal_submission force row level security;
create policy tenant_isolation on mes.portal_submission
  using (tenant_id = platform.current_tenant_id())
  with check (tenant_id = platform.current_tenant_id());

do $$
begin
  if exists(select 1 from pg_roles where rolname='core_app') then
    grant usage on schema mes to core_app;
    grant select, insert, update, delete on all tables in schema mes to core_app;
  end if;
  if exists(select 1 from pg_roles where rolname='core_admin') then
    alter default privileges for role core_admin in schema mes
      grant select, insert, update, delete on tables to core_app;
  end if;
end $$;
