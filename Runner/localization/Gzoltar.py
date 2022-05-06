import os

import Localizer
import Config
from utils import Cmd_util


class Gzoltar(Localizer.Localizer):
    def __init__(self, fl_args):
        super().__init__(fl_args)

        """
        gzoltar 0.1.1 version
        """
        self.jarPath = Config.GZOLTAR_JAR_PATH
        self.jarJavaVersion = Config.JAVA8_HOME
        self.mainClass = "apr.module.fl.main.Main"
    
    def localize(self):
        cmd = f"""
            cd {self.workingDir};
            export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
            TZ="America/New_York"; export TZ;
            export PATH="{self.jarJavaVersion}:$PATH";
            export JAVA_HOME="{self.jarJavaVersion}";
            time java {Config.JAVA_ARGS} -cp {self.jarPath} {self.mainClass} \\
                --externalProjPath {self.externalProjPath} \\
                --srcJavaDir {self.srcJavaDir} \\
                --binJavaDir {self.binJavaDir} \\
                --binTestDir {self.binTestDir} \\
                --jvmPath {self.jvmPath} \\
                --failedTests {self.failedTests} \\
                --dependencies {self.dependencies} \\
                --outputDir {self.outputDir} \\
                --workingDir {self.workingDir};
        """
        self.run_fl_cmd(cmd)
        pass