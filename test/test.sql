SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

INSERT INTO `applications` (`_id`, `name`) VALUES
(1, 'Test');

INSERT INTO `consumers` (`public_key`, `secret_key`, `application_id`, `flags`, `scopes`) VALUES
('401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d', 'ba3f5945ba6cfb18ca4869cef2c3daf9d4230e37629f3087b281be6ec8fda2bd', 1, 1, 2);

INSERT INTO `users` (`user_id`, `user_email`, `user_pass`) VALUES
(1, 'test@test.com', NULL);

INSERT INTO `user_tokens` (`user_id`, `application_id`, `public_key`, `secret_key`, `scopes`) VALUES
(1, 1, '9476f5130a07a7c0061de48bc19123f51636af704c5df369701960e0bc151255', 'b96fc9e22532b6bdc2fb760465ea19fa373c520703877ef7e3f0b6a728cefcb1', 2);
