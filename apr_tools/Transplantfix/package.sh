cd ~/env
java -version
./change-jdk-version.sh 11
cd -
mvn clean package -DskipTests

cd transplantfix
cp target/*-SNAPSHOT-jar-with-dependencies.jar versions/
