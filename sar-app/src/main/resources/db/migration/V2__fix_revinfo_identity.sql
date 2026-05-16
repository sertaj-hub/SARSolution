-- Fix revinfo.rev to auto-generate from sequence so Hibernate Envers can insert rows
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'revinfo') THEN
    ALTER TABLE revinfo ALTER COLUMN rev SET DEFAULT nextval('revinfo_seq');
  END IF;
END $$;
