import os, logging

import Apr
import Config
from utils import Exception_util, File_util

logger = logging.getLogger()


def get_failed_cases_str(args, output_dir):
    fault_localizer = args['fault_localizer']
    failed_test_cases_path = os.path.join(output_dir, "..", fault_localizer, "expected_failed_test_replicate.txt")

    if os.path.exists(failed_test_cases_path):
        failed_cases_list = File_util.read_file_to_list_strip(failed_test_cases_path)
        failed_cases_str = ":".join(failed_cases_list)

        d4j_failed_cases = args['failedTestMethods']

        if args['bug'].get_proj_id().lower() in ["lang_10"]:
            return failed_cases_str
        else:
            if len(d4j_failed_cases.split(':')) != len(failed_cases_str.split(':')):
                logger.warn(
                    f"d4j_failed_cases from d4j database: {d4j_failed_cases}, gzoltar expected failed test cases: {failed_cases_str}"
                )
            return d4j_failed_cases
    else:
        failed_cases_str = "null"
        return failed_cases_str


class Transplantfix(Apr.Apr):
    def __init__(self, args, tool_name):
        super().__init__(args)
        self.tool_name = tool_name

        if "_pfl" in self.tool_name:
            self.isPerfectFl = True
            self.timeout = 60
        else:
            self.isPerfectFl = False
            self.timeout = 120

        apr_project_name = 'Transplantfix'
        apr_name = "transplantfix"
        self.mainClass = "apr.aprlab.repair.main.Main"

        self.proj_dir = os.path.join(Config.PARENT_PROJECT_PATH, "apr_tools", f"{apr_project_name}/{apr_name}")
        self.d4j_dir = os.path.join(Config.DATASET_PATH, "defects4j")
        self.patch_diff_dir = os.path.join(self.outputDir, "..", "dataset")
        self.jarPath = os.path.join(self.proj_dir, f"versions/{apr_name}-1.0-SNAPSHOT-jar-with-dependencies.jar")

        self.jarJavaVersion = Config.JAVA11_HOME
        self.dataset_name = args['dataset_name']

        self.failed_cases_str = get_failed_cases_str(args, self.outputDir)

    def repair(self):
        cmd = f"""
        cd {self.workingDir};
        export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
        TZ="America/New_York"; export TZ;
        export PATH="{self.jarJavaVersion}:$PATH";
        export JAVA_HOME="{self.jarJavaVersion}";
        timeout {self.timeout}m java {Config.JAVA_ARGS} -cp {self.jarPath} {self.mainClass} \\
            --dataset {self.dataset_name} \\
            --patchInfoDir {self.patch_diff_dir} \\
            --buggyDir {self.workingDir} \\
            --srcJavaDir {self.srcJavaDir} \\
            --binJavaDir {self.binJavaDir} \\
            --binTestDir {self.binTestDir} \\
            --failedTestsStr {self.failed_cases_str} \\
            --d4jDir {self.d4j_dir} \\
            --dependencies {self.dependencies} \\
            --outputDir {self.outputDir} \\
            --validateJarPath {self.externalProjPath} \\
            --jvmPath {self.jvmPath}  \\
            --flTxtPath {self.fl_txt_path} \\
            --isPerfectFLMode {self.isPerfectFl} \\
        """
        print(f"cmd: {cmd}")
        if Config.debugMode:
            Exception_util.exit()
        self.run_repair_cmd(cmd)
        pass
