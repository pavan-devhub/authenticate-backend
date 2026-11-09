-- Logging in no longer needs an emailed code, so remove what only login codes used.
DELETE FROM otp_challenges WHERE purpose = 'LOGIN';
ALTER TABLE otp_challenges DROP FOREIGN KEY fk_otp_challenges_user;
ALTER TABLE otp_challenges DROP COLUMN user_id;
