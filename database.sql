SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

CREATE TABLE IF NOT EXISTS `applications` (
  `_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 ;

CREATE TABLE IF NOT EXISTS `clipboard_data` (
  `user_id` bigint(20) NOT NULL,
  `data` text NOT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `consumers` (
  `public_key` char(64) NOT NULL,
  `secret_key` char(64) NOT NULL,
  `application_id` int(10) unsigned NOT NULL,
  `flags` smallint(5) unsigned NOT NULL DEFAULT '0',
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

CREATE TABLE IF NOT EXISTS `google_user_mapping` (
  `user_id` int(11) NOT NULL,
  `service_user_id` varchar(128) NOT NULL,
  PRIMARY KEY (`service_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `ubuntuone_user_mapping` (
  `user_id` int(11) NOT NULL,
  `service_user_id` varchar(128) NOT NULL,
  PRIMARY KEY (`service_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_email` varchar(64) NOT NULL,
  `user_pass` char(60) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_name` (`user_email`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 ;


ALTER TABLE `consumers`
  ADD CONSTRAINT `applications` FOREIGN KEY (`application_id`) REFERENCES `applications` (`_id`) ON DELETE CASCADE ON UPDATE CASCADE;

