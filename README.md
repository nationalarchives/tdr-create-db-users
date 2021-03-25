# Create database users
Once the consignment api database is created, we need to create users for the API and the migration task to use. This means that we don't have to use the master username and password to access the database, we can control permissions using GRANT and we can use IAM authentication for the database. 

## Running the lambda in AWS
As this is something that we only need to run once after the database is created, we just need to run the lambda manually.

`aws lambda invoke --function-name tdr-create-db-users-$STAGE`

## Running the lambda locally.
If you want to run this against a local database, there are a few extra steps.

Create the database if it doesn't exist. It will need the correct username and password. You can use docker

`docker run --name postgres-db-users -e POSTGRES_PASSWORD=password -e POSTGRES_USER=tdr -e POSTGRES_DB=consignmentapi -d -p 5432:5432 postgres`

Connect to the local database

`PGPASSWORD=password psql -h localhost -p 5432 -U tdr -d consignmentapi`
  
Create the `rds_iam` role. This exists by default on RDS instances but not on the postgres docker image.

`CREATE ROLE rds_iam;`

Run the `Main` object. This object is not used by the lambda, it is only for running this locally.

## Running the tests
The tests run against a local postgres instance. We can't use h2 for testing because the syntax for creating users is different to postgres so you will need to run the docker command above to create a local postgres instance to run the tests from.

## Adding new environment variables to the tests
The environment variables in the deployed lambda are encrypted using KMS and then base64 encoded. These are then decoded in the lambda. Because of this, any variables in `src/test/resources/application.conf` which come from environment variables in `src/main/resources/application.conf` need to be stored base64 encoded. There are comments next to each variable to say what the base64 string decodes to. If you want to add a new variable you can run `echo -n "value of variable" | base64 -w 0` and paste the output into the test application.conf
