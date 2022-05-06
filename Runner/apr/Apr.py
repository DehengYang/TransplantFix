
import logging, abc, os

from utils import Cmd_util, File_util
import Config

logger = logging.getLogger()


class Apr(object, metaclass=abc.ABCMeta):
    def __init__(self, args):
        self.externalProjPath = Config.EXTERNAL_VALIDATOR

        self.srcJavaDir = args["srcJavaDir"]
        self.binJavaDir = args["binJavaDir"]
        self.binTestDir = args["binTestDir"]
        self.dependencies = args["dependencies"]
        self.jvmPath = args["jvmPath"]
        self.failedTests = args["failedTests"]
        self.workingDir = args["workingDir"]
        self.outputDir = args['outputDir']
        self.compliance_level = args["compliance_level"]
        self.fl_dir = args["fl_dir"]
        self.fl_txt_path = args["fl_txt_path"]
        if Config.debugMode:
            self.fl_time_cost = 0 
        else:
            self.fl_time_cost = args["fl_time_cost"]

        self.timeout = Config.TIMEOUT

        self.log_out_path = os.path.join(self.outputDir, "repair.log")
        self.log_err_path = os.path.join(self.outputDir, "error.log")

    def clean_dir(self):
        if os.path.exists(self.outputDir):
            logger.info(f"clean output dir: {self.outputDir}")
            File_util.rm_all_content_in_dir_except(self.outputDir, Config.LOG_NAME)

    def run_repair_cmd(self, cmd):
        out_file = open(self.log_out_path, 'w+')
        err_file = open(self.log_err_path, 'w+')
        out_file.write(cmd)
        out_file.flush()
        Cmd_util.run_cmd_to_log(cmd, out_file, err_file)

        if os.path.exists(self.log_err_path):
            error = File_util.read_file(self.log_err_path)
            if len(error) > 0:
                logger.warn(f"error message: {error}")
        else:
            logger.warn(f"{self.log_err_path} does not exists")

    @abc.abstractmethod
    def repair(self):
        pass