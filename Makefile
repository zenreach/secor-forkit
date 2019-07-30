CONFIG=src/main/config
TEST_HOME=/tmp/secor_test
TEST_CONFIG=src/test/config
JAR_FILE=target/secor-*-SNAPSHOT-bin.tar.gz
MVN_PROFILE?=kafka-0.10.2.0
MVN_OPTS=-DskipTests=true -Dmaven.javadoc.skip=true -P $(MVN_PROFILE) -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

CONTAINERS=$(shell ls containers)

.PHONY: build container

build:
	@mvn package $(MVN_OPTS) -P $(MVN_PROFILE)

dependency_tree:
	@mvn dependency:tree $(MVN_OPTS) -P $(MVN_PROFILE)

unit:
	@mvn test $(MVN_OPTS) -P $(MVN_PROFILE)

integration: build
	@rm -rf $(TEST_HOME)
	@mkdir -p $(TEST_HOME)
	@tar -xzf $(JAR_FILE) -C $(TEST_HOME)
	@cp $(TEST_CONFIG)/* $(TEST_HOME)
	@cp docker-compose.yaml $(TEST_HOME)
	@[ ! -e $(CONFIG)/core-site.xml ] && jar uf $(TEST_HOME)/secor-*.jar -C $(TEST_CONFIG) core-site.xml
	@[ ! -e $(CONFIG)/jets3t.properties ] && jar uf $(TEST_HOME)/secor-*.jar -C $(TEST_CONFIG) jets3t.properties
	cd $(TEST_HOME) && ./scripts/run_tests.sh

test: build unit integration

container: build
	docker build -t secor .

container_%:
	docker build -t secor_$* containers/$*

test_%: container_%
	@mkdir -p .m2
	docker run -v $(CURDIR)/.m2:/root/.m2:rw -v $(CURDIR):/work:rw secor_$* sh -c "echo 127.0.0.1 test-bucket.localhost >> /etc/hosts && make clean test"

docker_test: $(foreach container, $(CONTAINERS), test_$(container))

clean:
	rm -rf target/
