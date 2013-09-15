#!/bin/bash
mysql -ucopyit -pcopyit -e "DELETE FROM request_tokens WHERE timestamp < NOW() - INTERVAL 5 MINUTE;" copyit
