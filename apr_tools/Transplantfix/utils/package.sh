cd ~/env
java -version
./change-jdk-version.sh 11
cd -
mvn clean install -DskipTests

