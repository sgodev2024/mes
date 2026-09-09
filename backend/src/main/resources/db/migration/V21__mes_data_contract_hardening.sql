alter table mes.template_definition
  add column field_contract jsonb not null default '[]'::jsonb,
  add column contract_hash char(64) not null default encode(digest('[]', 'sha256'), 'hex'),
  add column effective_from date not null default date '2026-09-08',
  add column effective_to date,
  add column lifecycle_status varchar(20) not null default 'ACTIVE';

alter table mes.template_definition
  add constraint mes_template_field_contract_array_ck
    check (jsonb_typeof(field_contract) = 'array'),
  add constraint mes_template_effective_range_ck
    check (effective_to is null or effective_to >= effective_from),
  add constraint mes_template_lifecycle_ck
    check (lifecycle_status in ('DRAFT','ACTIVE','RETIRED'));

create or replace function mes.protect_template_contract()
returns trigger language plpgsql as $$
begin
  if row(old.filename_pattern, old.worksheet_name, old.parser_type, old.reporting_entity_code,
         old.header_tokens, old.field_contract, old.version)
     is distinct from
     row(new.filename_pattern, new.worksheet_name, new.parser_type, new.reporting_entity_code,
         new.header_tokens, new.field_contract, new.version)
     and not (old.field_contract = '[]'::jsonb
              and old.contract_hash = encode(digest('[]', 'sha256'), 'hex')) then
    raise exception 'MES template contract is immutable; create a new version instead'
      using errcode = '23514';
  end if;
  return new;
end $$;

create trigger mes_template_contract_immutable
before update on mes.template_definition
for each row execute function mes.protect_template_contract();

create unique index identity_account_id_tenant_uidx on identity.account(id, tenant_id);
create unique index file_object_id_tenant_uidx on files.file_object(id, tenant_id);
create unique index mes_template_id_tenant_uidx on mes.template_definition(id, tenant_id);
create unique index mes_batch_id_tenant_uidx on mes.import_batch(id, tenant_id);
create unique index mes_calendar_id_tenant_uidx on mes.reporting_calendar_item(id, tenant_id);

alter table mes.import_batch
  add constraint mes_batch_template_tenant_fk foreign key(template_id, tenant_id)
    references mes.template_definition(id, tenant_id),
  add constraint mes_batch_file_tenant_fk foreign key(file_id, tenant_id)
    references files.file_object(id, tenant_id),
  add constraint mes_batch_duplicate_tenant_fk foreign key(duplicate_of, tenant_id)
    references mes.import_batch(id, tenant_id),
  add constraint mes_batch_submitter_tenant_fk foreign key(submitted_by, tenant_id)
    references identity.account(id, tenant_id),
  add constraint mes_batch_confirmer_tenant_fk foreign key(confirmed_by, tenant_id)
    references identity.account(id, tenant_id),
  add constraint mes_batch_approver_tenant_fk foreign key(approved_by, tenant_id)
    references identity.account(id, tenant_id);

alter table mes.import_record
  add constraint mes_record_batch_tenant_fk foreign key(batch_id, tenant_id)
    references mes.import_batch(id, tenant_id) on delete cascade;

alter table mes.import_issue
  add constraint mes_issue_batch_tenant_fk foreign key(batch_id, tenant_id)
    references mes.import_batch(id, tenant_id) on delete cascade;

alter table mes.reporting_calendar_item
  add constraint mes_calendar_template_tenant_fk foreign key(template_id, tenant_id)
    references mes.template_definition(id, tenant_id),
  add constraint mes_calendar_batch_tenant_fk foreign key(current_batch_id, tenant_id)
    references mes.import_batch(id, tenant_id);

alter table mes.portal_submission
  add constraint mes_portal_calendar_tenant_fk foreign key(calendar_item_id, tenant_id)
    references mes.reporting_calendar_item(id, tenant_id),
  add constraint mes_portal_batch_tenant_fk foreign key(batch_id, tenant_id)
    references mes.import_batch(id, tenant_id),
  add constraint mes_portal_submitter_tenant_fk foreign key(submitted_by, tenant_id)
    references identity.account(id, tenant_id);

insert into identity.role(tenant_id, code, name, system_role)
select tenant.id, role.code, role.name, true
from platform.tenant tenant
cross join (values
  ('mes-report-preparer', 'MES - Người lập báo cáo'),
  ('mes-report-approver', 'MES - Người phê duyệt'),
  ('mes-portal-operator', 'MES - Người nộp Portal'),
  ('mes-report-viewer', 'MES - Người xem báo cáo')
) as role(code, name)
on conflict(tenant_id, code) do nothing;

insert into identity.policy(tenant_id, code, resource_type, action, effect, condition_json)
select tenant.id, policy.code, 'MES_REPORT', policy.action, 'ALLOW', '{}'::jsonb
from platform.tenant tenant
cross join (values
  ('mes-report-read', 'READ'),
  ('mes-report-import', 'IMPORT'),
  ('mes-report-confirm', 'CONFIRM'),
  ('mes-report-approve', 'APPROVE'),
  ('mes-report-submit-portal', 'SUBMIT_PORTAL')
) as policy(code, action)
on conflict(tenant_id, code, version) do nothing;

insert into identity.role_policy(tenant_id, role_id, policy_id)
select role.tenant_id, role.id, policy.id
from identity.role role
join identity.policy policy on policy.tenant_id = role.tenant_id
where (role.code = 'mes-report-preparer' and policy.code in ('mes-report-read','mes-report-import','mes-report-confirm'))
   or (role.code = 'mes-report-approver' and policy.code in ('mes-report-read','mes-report-approve'))
   or (role.code = 'mes-portal-operator' and policy.code in ('mes-report-read','mes-report-submit-portal'))
   or (role.code = 'mes-report-viewer' and policy.code = 'mes-report-read')
on conflict do nothing;

update identity.permission_revision set revision = revision + 1, updated_at = now();
