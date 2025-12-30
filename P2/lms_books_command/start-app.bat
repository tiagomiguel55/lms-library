@echo off
cd /d C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_command
echo Starting lms_books_command application...
echo Compilation and startup log will be saved to startup.log
mvn spring-boot:run > startup.log 2>&1

