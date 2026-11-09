-- A login can now also fail on a wrong emailed code, so a yes/no column is no
-- longer enough. Existing rows keep their meaning.
ALTER TABLE login_attempts ADD COLUMN outcome VARCHAR(20) NULL AFTER user_id;
UPDATE login_attempts SET outcome = IF(successful, 'SUCCESS', 'WRONG_PASSWORD');
ALTER TABLE login_attempts MODIFY COLUMN outcome VARCHAR(20) NOT NULL, DROP COLUMN successful;
