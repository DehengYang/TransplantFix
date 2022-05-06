# -*- encoding: utf-8 -*-
'''
@Description: 
@Date       : 2021/08/12 15:42:04
@Author     : apr
'''

import logging
import os
import json, collections
import re

import Dataset
from dataset import Bug
import Config
from utils import Cmd_util, File_util

logger = logging.getLogger()


class Defects4j(Dataset.Dataset):
    def __init__(self):
        self.name = self.__class__.__name__.lower()
        super().__init__(self.name)

        self.path = os.path.join(Config.DATASET_PATH, "defects4j")

    def checkout(self, bug, output_dir):
        """
        output_dir is the parent dir of the bug working dir
        """
        self.set_bug_working_dir(bug, output_dir)

        cmd = f"""
        export PATH="{Config.JAVA7_HOME}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(Config.JAVA7_HOME, '..')}";
        defects4j checkout -p {bug.project} -v {bug.bug_id}b -w {bug.get_working_dir()};
        """

        Cmd_util.run_cmd(cmd)

    def checkout_fixed(self, bug, fixed_dir):
        if not fixed_dir.endswith(bug.name):
            fixed_dir = os.path.join(fixed_dir, bug.name)
        if not os.path.exists(fixed_dir):
            os.makedirs(fixed_dir)
        else:
            File_util.rm_dir_safe_contain(fixed_dir, bug.name)

        cmd = f"""
        export PATH="{Config.JAVA7_HOME}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(Config.JAVA7_HOME, '..')}";
        defects4j checkout -p {bug.project} -v {bug.bug_id}f -w {fixed_dir};
        """

        Cmd_util.run_cmd(cmd)
        return fixed_dir

    def _get_dataset_path(self):
        return os.path.join(Config.DATASET_PATH, "defects4j", "framework", "bin")

    def get_info_path(self):
        return os.path.join(Config.DATASET_INFO_PATH, "defects4j")

    def compile(self, bug):
        cmd = f"""
        export PATH="{Config.JAVA7_HOME}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(Config.JAVA7_HOME, '..')}";
        export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
        cd {bug.get_working_dir()};
        defects4j compile;
        """

        Cmd_util.run_cmd(cmd)

    def run_test(self, bug):
        cmd = f"""
        export PATH="{Config.JAVA7_HOME}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(Config.JAVA7_HOME, '..')}";
        export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
        cd {bug.get_working_dir()};
        defects4j test;
        """
        Cmd_util.run_cmd(cmd)

    def failing_tests(self, bug):
        cmd = f"""
        export PATH="{Config.JAVA7_HOME}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(Config.JAVA7_HOME, '..')}";
        export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
        defects4j info -p {bug.project} -b {bug.bug_id};
        """
        output = Cmd_util.run_cmd(cmd)

        tests = []
        pattern = re.compile('- (.*)::(.*)')
        matches = re.findall(pattern, output)
        for match in matches:
            tests.append(match[0])
        return list(set(tests))

    def failing_test_methods(self, bug):
        cmd = f"""
        export PATH="{Config.JAVA7_HOME}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(Config.JAVA7_HOME, '..')}";
        export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
        defects4j info -p {bug.project} -b {bug.bug_id};
        """
        output = Cmd_util.run_cmd(cmd)

        test_methods = []
        pattern = re.compile('- (.*)::(.*)')
        matches = re.findall(pattern, output)
        for match in matches:
            test_methods.append(f"{match[0]}#{match[1]}")
        return list(set(test_methods))

    def get_bug(self, bug_id):
        separator = "-"
        if "_" in bug_id:
            separator = "_"
        (project, id) = bug_id.split(separator)
        for bug in self.get_bugs():  # bug class instance
            if bug.project.lower() == project.lower():
                if int(bug.bug_id) == int(id):
                    return bug
        return None

    def get_bugs(self):
        if self.bugs is not None:
            return self.bugs
        bugs = []
        defects4j_info_path = self.get_info_path()
        for project_name in os.listdir(defects4j_info_path):
            project_info_path = os.path.join(defects4j_info_path, project_name)
            if os.path.isfile(project_info_path):
                with open(project_info_path) as f:
                    data = json.load(f)
                    self.project_data[data['project']] = data
                    for i in range(1, data['nbBugs'] + 1):
                        bug = Bug.Bug(self, data['project'], i)
                        bug.project_data = data
                        bugs += [bug]
        return bugs

    def source_folders(self, bug):
        sources = self.project_data[bug.project]["src"]
        sources = collections.OrderedDict(sorted(sources.items(), key=lambda t: int(t[0])))

        source = None
        for index, src in sources.items():
            if bug.bug_id <= int(index):
                source = src['srcjava']
                break
        return [source]

    def test_folders(self, bug):
        sources = self.project_data[bug.project]["src"]
        sources = collections.OrderedDict(sorted(sources.items(), key=lambda t: int(t[0])))

        source = None
        for index, src in sources.items():
            if bug.bug_id <= int(index):
                source = src['srctest']
                break
        return [source]

    def bin_folders(self, bug):
        sources = self.project_data[bug.project]["src"]
        sources = collections.OrderedDict(sorted(sources.items(), key=lambda t: int(t[0])))
        source = None
        for index, src in sources.items():
            if bug.bug_id <= int(index):
                source = src['binjava']
                break
        return [source]

    def test_bin_folders(self, bug):
        sources = self.project_data[bug.project]["src"]
        sources = collections.OrderedDict(sorted(sources.items(), key=lambda t: int(t[0])))

        source = None
        for index, src in sources.items():
            if bug.bug_id <= int(index):
                source = src['bintest']
                break
        return [source]

    def classpath(self, bug):
        classpath = ""
        workdir = bug.get_working_dir()

        sources = self.project_data[bug.project]["classpath"]
        sources = collections.OrderedDict(sorted(sources.items(), key=lambda t: int(t[0])))
        for index, cp in sources.items():
            if bug.bug_id <= int(index):
                for c in cp.split(":"):
                    if classpath != "":
                        classpath += ":"
                    classpath += os.path.join(workdir, c)
                break
        for (root, _, files) in os.walk(os.path.join(workdir, "lib")):
            for f in files:
                if f[-4:] == ".jar":
                    classpath += ":" + (os.path.join(root, f))
        libs = []
        cmd = """export PATH="%s:%s:$PATH";export JAVA_HOME="%s";
        cd %s;
        defects4j export -p cp.test 2> /dev/null;
        """ % (Config.JAVA7_HOME, self._get_dataset_path(), os.path.join(Config.JAVA7_HOME,
                                                                         '..'), bug.get_working_dir())
        libs_split = Cmd_util.run_cmd(cmd).split(":")
        for lib_str in libs_split:
            lib = os.path.basename(lib_str)
            if lib[-4:] == ".jar":
                libs.append(lib)
        libs_path = os.path.join(self.path, "framework", "projects", bug.project, "lib")
        for (root, _, files) in os.walk(libs_path):
            for f in files:
                if f in libs:
                    classpath += ":" + (os.path.join(root, f))
        libs_path = os.path.join(self.path, "framework", "projects", "lib")
        for (root, _, files) in os.walk(libs_path):
            for f in files:
                if f in libs:
                    classpath += ":" + (os.path.join(root, f))

        # add junit jar
        if "/junit-4" not in classpath and "/junit-3" not in classpath:
            classpath = (os.path.join(libs_path, "junit-4.11.jar")) + ":" + classpath

        # add bin folders of src and tests of the buggy program
        bin_folder_path = os.path.join(bug.get_working_dir(), self.bin_folders(bug)[0])
        test_bin_folder_path = os.path.join(bug.get_working_dir(), self.test_bin_folders(bug)[0])
        if bin_folder_path not in classpath:
            classpath = bin_folder_path + ":" + classpath
        if test_bin_folder_path not in classpath:
            classpath = test_bin_folder_path + ":" + classpath
        return classpath

    # todo
    def failing_module(self, bug):
        return ""

    def compliance_level(self, bug):
        return self.project_data[bug.project]["complianceLevel"][str(bug.bug_id)]["source"]

    def get_patch_diff(self, bug):
        assert len(self.source_folders(bug)) == 1
        buggy_dir = os.path.join(bug.get_working_dir(), self.source_folders(bug)[0])
        fix_dir = Config.TMP_FIXED_DIR
        fixed_dir = self.checkout_fixed(bug, fix_dir)
        fixed_dir = os.path.join(fixed_dir, self.source_folders(bug)[0])
        patch_diff = Cmd_util.run_cmd(f"diff -Naur {buggy_dir} {fixed_dir}")

        # must delete to save / space!
        logger.info(f"rm tmp_checkout_dir: {fixed_dir}")
        File_util.rm_dir_safe_contain(fixed_dir, '/tmp/')

        return patch_diff