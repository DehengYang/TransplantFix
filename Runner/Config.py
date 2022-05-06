import os

rerun_dataset = True

MAX_INT = 1000000000000

PROJECT_PATH = os.path.realpath(os.path.dirname(__file__))
PARENT_PROJECT_PATH = os.path.join(os.path.realpath(os.path.dirname(__file__)), "..")

M2_REPO = os.path.expanduser("~/.m2/repository")
MAVEN_BIN = os.environ.get("MAVEN_BIN", "/usr/bin/mvn")

DATASET_PATH = os.path.abspath(os.path.join(PROJECT_PATH, "..", "datasets"))
DATASET_INFO_PATH = os.path.abspath(os.path.join(PROJECT_PATH, "..", "datasets", "dataset_info"))
assert os.path.exists(DATASET_PATH)
assert os.path.exists(DATASET_INFO_PATH)

JAVA7_HOME = os.path.expanduser("~/env/jdk1.7.0_80/bin/")
JAVA8_HOME = os.path.expanduser("~/env/jdk1.8.0_202/bin/")
JAVA11_HOME = os.path.expanduser("~/env/jdk-11/bin/")
JAVA_ARGS = "-Xmx4g -Xms1g"
assert os.path.exists(JAVA7_HOME)
assert os.path.exists(JAVA8_HOME)

TIMEOUT = 120  

TMP_BUGGY_DIR = "/tmp/buggy"
TMP_FIXED_DIR = "/tmp/fixed"

TMP_OUTPUT_DIR = os.path.join(PARENT_PROJECT_PATH, "results")
LOG_NAME = "execution_framework.log"

GZOLTAR_JAR_PATH = os.path.join(
    PARENT_PROJECT_PATH,
    "fl_modules/fault_localizer/versions/gzoltar_localizer-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
EXTERNAL_VALIDATOR = os.path.join(
    PARENT_PROJECT_PATH, "patch_validator/patch_validator/versions/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar")

debugMode = False
transplantfix_tool_version = 0
debug_tool_name_list = ['transplantfix_pfl', 'transplantfix_sfl'] 
debug_dataset = 'defects4j'   #'defects4j' defects4j2
bugs_id_to_run_file_name = "/home/apr/data/TransplantFix/bugs_info/defects4j.txt" 

rerun_d4j2_info_loads = True
D4J2 = 'defects4j2'
d4j2_uniq_bug_id_path = os.path.join(PROJECT_PATH, "bugs_info", f"{D4J2}_uniq.txt")