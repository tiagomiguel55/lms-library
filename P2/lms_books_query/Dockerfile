# Use Microsoft OpenJDK 17 (Ubuntu base) hosted on MCR
FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

# Set a working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/LMSBooks-0.0.1-SNAPSHOT.jar /app/app.jar

# Informative only; it has no influence in the port effectively exposed by the container.
#EXPOSE 8080

# Copy entrypoint script
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Set the command to run the JAR file
ENTRYPOINT ["/entrypoint.sh"]
