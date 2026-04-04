@echo off
cd /d d:\codex_test
set AI_MODE=openai
mvn spring-boot:run -Dspring-boot.run.profiles=rag -Dspring-boot.run.arguments="--server.port=8081"