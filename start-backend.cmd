@echo off
cd /d d:\codex_test
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"