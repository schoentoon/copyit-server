#!/bin/bash
mysql -ucopyit -pcopyit -e "DELETE FROM nonces WHERE timestamp < NOW() - INTERVAL 5 MINUTE;" copyit
