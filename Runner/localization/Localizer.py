
import logging, abc, os

import Config
from utils import Cmd_util, File_util

logger = logging.getLogger()


class Localizer(object, metaclass=abc.ABCMeta):
    def __init__(self, fl_args):
        self.externalProjPath = Config.EXTERNAL_VALIDATOR

        self.srcJavaDir = fl_args["srcJavaDir"]
        self.binJavaDir = fl_args["binJavaDir"]
        self.binTestDir = fl_args["binTestDir"]
        self.dependencies = fl_args["dependencies"]
        self.jvmPath = fl_args["jvmPath"]
        self.failedTests = fl_args["failedTests"]
        self.failedTestMethods = fl_args["failedTestMethods"]
        self.workingDir = fl_args["workingDir"]
        self.outputDir = fl_args['outputDir']

        self.log_out_path = os.path.join(self.outputDir, "fl.log")
        self.log_err_path = os.path.join(self.outputDir, "error.log")

    def run_fl_cmd(self, cmd):
        out_file = open(self.log_out_path, 'w+')
        err_file = open(self.log_err_path, 'w+')
        out_file.write(cmd)
        out_file.flush()
        Cmd_util.run_cmd_to_log(cmd, out_file, err_file)

        if os.path.exists(self.log_err_path):
            error = File_util.read_file(self.log_err_path)
            if len(error) > 0:
                logger.warn(f"error message: {error}")

    @abc.abstractmethod
    def localize(self):
        pass

    def clean_dir(self):
        if os.path.exists(self.outputDir):
            logger.info(f"clean output dir: {self.outputDir}")
            File_util.rm_all_content_in_dir(self.outputDir)

    def get_next_args(self, args, dataset, bug):
        next_args = {}

        next_args["srcJavaDir"] = self.srcJavaDir
        next_args["binJavaDir"] = self.binJavaDir
        next_args["binTestDir"] = self.binTestDir
        next_args["dependencies"] = self.dependencies
        next_args["jvmPath"] = self.jvmPath
        next_args["failedTests"] = self.failedTests
        next_args["failedTestMethods"] = self.failedTestMethods
        next_args["workingDir"] = self.workingDir
        next_args["compliance_level"] = dataset.compliance_level(bug)
        next_args["fl_dir"] = self.outputDir

        output_dir = os.path.join(self.outputDir, "..", args.apr_name.lower())
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        next_args['outputDir'] = output_dir

        return next_args