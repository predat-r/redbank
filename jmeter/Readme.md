# JMeter Load Test

This directory contains the JMeter load test for the RedBank API.

## Prerequisites

- Spring Boot application running on `http://localhost:8080`
- Apache JMeter 5.6.3
- Java 21 for running JMeter
- A valid admin account

## Test flow

Each thread:

1. Logs in as admin
2. Registers two users
3. Approves both users
4. Logs both users in
5. Deposits money into the sender account
6. Transfers money to the receiver
7. Withdraws money
8. Checks balances and transaction histories

## Run

From the project root:

```bash
mkdir -p jmeter/results jmeter/reports jmeter/logs

rm -f jmeter/results/results.jtl
rm -f jmeter/logs/jmeter.log
rm -rf jmeter/reports/html-report

JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.12-amzn" \
PATH="$HOME/.sdkman/candidates/java/21.0.12-amzn/bin:$PATH" \
jmeter -n \
  -t jmeter/banking-load-test.jmx \
  -Jadmin_email="ADMIN_EMAIL" \
  -Jadmin_password="ADMIN_PASSWORD" \
  -Jusers=20 \
  -Jramp_up=60 \
  -Jloops=1 \
  -l jmeter/results/results.jtl \
  -j jmeter/logs/jmeter.log \
  -e \
  -o jmeter/reports/html-report