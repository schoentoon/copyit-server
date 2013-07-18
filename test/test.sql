SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

INSERT INTO `applications` (`_id`, `name`) VALUES
(1, 'Test');

INSERT INTO `consumers` (`public_key`, `secret_key`, `application_id`, `flags`) VALUES
('401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d', 'ba3f5945ba6cfb18ca4869cef2c3daf9d4230e37629f3087b281be6ec8fda2bd', 1, 0);
