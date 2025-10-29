#!/bin/bash
cd subscriptiontracker
./mvnw clean package
java -jar target/*.jar