-- A login can now also fail on a wrong emailed code, so a yes/no column is no
-- longer enough. Existing rows keep their meaning.
ALTER TABLE login_attempts ADD COLUMN outcome VARCHAR(20);
UPDATE login_attempts SET outcome = CASE WHEN successful THEN 'SUCCESS' ELSE 'WRONG_PASSWORD' END;
ALTER TABLE login_attempts ALTER COLUMN outcome SET NOT NULL;
ALTER TABLE login_attempts DROP COLUMN successful;
