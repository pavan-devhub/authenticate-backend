-- Logging in no longer needs an emailed code, so remove what only login codes used.
-- Dropping the column also drops its foreign key.
DELETE FROM otp_challenges WHERE purpose = 'LOGIN';
ALTER TABLE otp_challenges DROP COLUMN user_id;
