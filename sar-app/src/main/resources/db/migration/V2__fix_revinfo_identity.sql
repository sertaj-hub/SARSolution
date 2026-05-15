-- Fix revinfo.rev to auto-generate from sequence so Hibernate Envers can insert rows
ALTER TABLE revinfo ALTER COLUMN rev SET DEFAULT nextval('revinfo_seq');
