SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

CREATE TABLE IF NOT EXISTS `applications` (
  `_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `clipboard_data` (
  `user_id` bigint(20) NOT NULL,
  `data` text NOT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `consumers` (
  `public_key` char(64) NOT NULL,
  `secret_key` char(64) NOT NULL,
  `application_id` int(10) unsigned NOT NULL,
  `flags` smallint(5) unsigned NOT NULL DEFAULT '0',
  `scopes` smallint(1) unsigned NOT NULL DEFAULT '1' COMMENT '1 = read-only, 2 = write/read',
  PRIMARY KEY (`public_key`),
  UNIQUE KEY `application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TRIGGER IF EXISTS `generate_random_keys`;
DELIMITER //
CREATE TRIGGER `generate_random_keys` BEFORE INSERT ON `consumers`
 FOR EACH ROW BEGIN
 
    IF new.public_key = '' AND new.secret_key = '' THEN
    	SET new.public_key = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,64)
           ,new.secret_key = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,64);
    END IF;
END
//
DELIMITER ;

CREATE TABLE IF NOT EXISTS `devices` (
  `device_id` char(36) NOT NULL,
  `user_id` int(11) NOT NULL,
  `device_name` text,
  `device_password` text,
  PRIMARY KEY (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `facebook_user_mapping` (
  `user_id` int(10) NOT NULL,
  `service_user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`service_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `gcm_ids` (
  `user_id` int(10) unsigned NOT NULL,
  `gcm_token` varchar(256) NOT NULL,
  PRIMARY KEY (`user_id`,`gcm_token`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `google_user_mapping` (
  `user_id` int(11) NOT NULL,
  `service_user_id` varchar(128) NOT NULL,
  PRIMARY KEY (`service_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `nonces` (
  `_id` int(10) unsigned NOT NULL,
  `nonce` varchar(8) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`_id`,`nonce`)
) ENGINE=MEMORY DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `request_tokens` (
  `application_id` int(10) unsigned NOT NULL,
  `public_key` varchar(64) NOT NULL DEFAULT '',
  `secret_key` varchar(64) NOT NULL DEFAULT '',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `aid` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Dirty hack because MySQL can''t return last inserted row directly..',
  `callback_uri` varchar(1024) NOT NULL,
  `verifier` varchar(32) NOT NULL DEFAULT '',
  `user_id` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`aid`),
  UNIQUE KEY `public_key` (`public_key`),
  KEY `applications` (`application_id`)
) ENGINE=MEMORY DEFAULT CHARSET=latin1;
DROP TRIGGER IF EXISTS `generate_random_request_tokens`;
DELIMITER //
CREATE TRIGGER `generate_random_request_tokens` BEFORE INSERT ON `request_tokens`
 FOR EACH ROW BEGIN
    IF new.public_key = '' AND new.secret_key = '' THEN
        SET new.public_key = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,64)
           ,new.secret_key = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,64)
           ,new.verifier = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,32);
    END IF;
END
//
DELIMITER ;

CREATE TABLE IF NOT EXISTS `ubuntuone_user_mapping` (
  `user_id` int(11) NOT NULL,
  `service_user_id` varchar(128) NOT NULL,
  PRIMARY KEY (`service_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_email` varchar(64) NOT NULL,
  `user_pass` char(60) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_name` (`user_email`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `user_tokens` (
  `user_id` int(10) unsigned NOT NULL,
  `application_id` int(10) unsigned NOT NULL,
  `public_key` char(64) NOT NULL DEFAULT '',
  `secret_key` char(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`user_id`,`application_id`),
  KEY `application_user_tokens` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TRIGGER IF EXISTS `generate_random_user_tokens`;
DELIMITER //
CREATE TRIGGER `generate_random_user_tokens` BEFORE INSERT ON `user_tokens`
 FOR EACH ROW BEGIN
 
    IF new.public_key = '' AND new.secret_key = '' THEN
        SET new.public_key = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,64)
           ,new.secret_key = SUBSTR(CONCAT(MD5(RAND()),MD5(RAND())),1,64);
    END IF;
END
//
DELIMITER ;


ALTER TABLE `consumers`
  ADD CONSTRAINT `applications` FOREIGN KEY (`application_id`) REFERENCES `applications` (`_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `gcm_ids`
  ADD CONSTRAINT `gcm_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `user_tokens`
  ADD CONSTRAINT `applications_` FOREIGN KEY (`application_id`) REFERENCES `applications` (`_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
