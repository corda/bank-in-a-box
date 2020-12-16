FROM corda/corda-zulu-java1.8-4.6

USER "root"

## Add packages, clean cache, create dirs, create corda user and change ownership
RUN apt-get update && \
    apt-get install -y wget

## PostgreSQL JDBC
ADD --chown=corda:corda https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.18/postgresql-42.2.18.jar /opt/corda/drivers/postgresql-42.2.18.jar

## Accounts SDK JAR's
ADD --chown=corda:corda https://software.r3.com/artifactory/corda-lib/com/r3/corda/lib/accounts/accounts-contracts/1.0/accounts-contracts-1.0.jar /opt/corda/cordapps/accounts-contracts-1.0.jar
ADD --chown=corda:corda https://software.r3.com/artifactory/corda-lib/com/r3/corda/lib/accounts/accounts-workflows/1.0/accounts-workflows-1.0.jar /opt/corda/cordapps/accounts-workflows-1.0.jar

## CorDapp JAR's
COPY --chown=corda:corda workflows/build/libs/bank-in-a-box-workflows-0.1.jar /opt/corda/cordapps/bank-in-a-box-workflows-0.1.jar
COPY --chown=corda:corda contracts/build/libs/bank-in-a-box-contracts-0.1.jar /opt/corda/cordapps/bank-in-a-box-contracts-0.1.jar
COPY --chown=corda:corda credit-rating-oracle/build/libs/credit-rating-oracle-0.1.jar /opt/corda/cordapps/credit-rating-oracle-0.1.jar

USER "corda"

CMD ["sh", "-c", "java -jar /opt/corda/bin/corda.jar run-migration-scripts --core-schemas --app-schemas -f /etc/corda/node.conf ; java -jar /opt/corda/bin/corda.jar -f /etc/corda/node.conf"]