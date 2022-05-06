# -*- encoding: utf-8 -*-
'''
@Description: 
@Date       : 2021/08/12 15:42:04
@Author     : apr
'''

import logging
import os
import re
from subprocess import check_output
from copy import deepcopy

import Dataset
from dataset import Bug, Defects4j2_util
import Config
from utils import Cmd_util, File_util, Yaml_util

logger = logging.getLogger()


class Defects4j2(Dataset.Dataset):
    def __init__(self):
        self.name = self.__class__.__name__.lower()
        self.java_home = Config.JAVA8_HOME
        self.path = os.path.join(Config.DATASET_PATH, "defects4j2/defects4j")
        self.proj_info_dir = os.path.join(self.path, 'framework/projects')
        self.dataset_info_dir = os.path.join(Config.DATASET_PATH, "dataset_info", self.name.lower())
        self.proj_data = {}

        super().__init__(self.name)

    def get_bugs(self):
        if self.bugs is not None:
            return self.bugs

        bugs = []
        proj_list = self.get_proj_names()
        # /mnt/data/2021_11_multi_chunk_repair/APRConfig/datasets/defects4j2/defects4j/framework/projects/Codec
        for proj_name in proj_list:
            proj_info_dir = os.path.join(self.proj_info_dir, proj_name)
            # commit-db
            commit_db_file = os.path.join(proj_info_dir, 'commit-db')
            commit_db_list = File_util.read_file_to_list_strip(commit_db_file)
            for commit in commit_db_list:
                assert "," in commit
                bug_id = commit.split(",")[0]
                bug = Bug.Bug(self, proj_name, bug_id)
                bugs.append(bug)
        return bugs

    def get_proj_names(self):
        proj_list = []
        # /mnt/data/2021_11_multi_chunk_repair/APRConfig/datasets/defects4j2/defects4j/framework/projects/Codec
        for dirname in os.listdir(self.proj_info_dir):
            if os.path.isdir(os.path.join(self.proj_info_dir, dirname)) and dirname != 'lib':
                proj_list.append(dirname)
        return proj_list

    def source_folders(self, bug):
        if bug not in self.proj_data:
            self.set_proj_data(bug)
        source = self.proj_data[bug]["src_java"]
        return [source]

    def set_proj_data(self, bug_ori):
        bug = deepcopy(bug_ori)

        bug_data_file = os.path.join(self.dataset_info_dir, str(bug))
        if os.path.exists(bug_data_file) and not Config.rerun_d4j2_info_loads:
            bug_data_dict = Yaml_util.read_yaml(bug_data_file)
            self.proj_data[bug] = bug_data_dict
        else:
            tmp_output_file = "/tmp/output.txt"
            tmp_checkout_dir = f"/tmp/{bug.name}"
            if os.path.exists(tmp_checkout_dir):
                File_util.rm_files_in_dir(tmp_checkout_dir)
            else:
                os.makedirs(tmp_checkout_dir)
            self.checkout(bug, tmp_checkout_dir)

            bug_data_dict = {}
            classpath_test = self.get_property(bug, 'cp.test', tmp_output_file)
            classpath_compile = self.get_property(bug, 'cp.compile', tmp_output_file)
            src_class = self.get_property(bug, 'dir.src.classes', tmp_output_file)
            bin_class = self.get_property(bug, 'dir.bin.classes', tmp_output_file)
            src_test = self.get_property(bug, 'dir.src.tests', tmp_output_file)
            bin_test = self.get_property(bug, 'dir.bin.tests', tmp_output_file)

            bug_data_dict['src_java'] = src_class
            bug_data_dict['bin_java'] = bin_class
            bug_data_dict['src_test'] = src_test
            bug_data_dict['bin_test'] = bin_test
            bug_data_dict['classpath'] = classpath_test
            bug_data_dict['classpath_compile'] = classpath_compile
            # absolute path & relative path (aligned with tmp buggy dir)
            bug_data_dict['classpath'], bug_data_dict['classpath_relative'] = Defects4j2_util.parse_classpath_test(
                classpath_test, tmp_checkout_dir)
            bug_data_dict['classpath_compile'], bug_data_dict[
                'classpath_compile_relative'] = Defects4j2_util.parse_classpath_test(
                    classpath_compile, tmp_checkout_dir)

            # must delete to save / space!
            logger.info(f"rm tmp_checkout_dir: {tmp_checkout_dir}")
            File_util.rm_dir_safe_contain(tmp_checkout_dir, '/tmp/')

            Yaml_util.write_to_yaml(bug_data_file, bug_data_dict)
            self.proj_data[bug] = bug_data_dict

    def get_property(self, bug, property, output_file):
        cmd = f"""
        {self.set_env()}
        cd {bug.get_working_dir()};
        defects4j export -p {property} -o {output_file};
        """
        Cmd_util.run_cmd(cmd)

        property = File_util.read_file_to_str(output_file)
        return property

    def set_env(self):
        return f"""
export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
export JAVA_HOME="{os.path.join(self.java_home, '..')}";
        """

    def checkout(self, bug, output_dir):
        """
        output_dir is the parent dir of the bug working dir
        """
        self.set_bug_working_dir(bug, output_dir)

        cmd = f"""
        export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(self.java_home, '..')}";
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
        export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(self.java_home, '..')}";
        defects4j checkout -p {bug.project} -v {bug.bug_id}f -w {fixed_dir};
        """

        Cmd_util.run_cmd(cmd)
        return fixed_dir

    def _get_dataset_path(self):
        return os.path.join(self.path, "framework", "bin")

    # d4j2 has no info path
    # def get_info_path(self):
    #     return self.path

    def compile(self, bug):
        cmd = f"""
        export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(self.java_home, '..')}";
        export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
        cd {bug.get_working_dir()};
        defects4j compile;
        """

        Cmd_util.run_cmd(cmd)

    def run_test(self, bug):
        cmd = f"""
        export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(self.java_home, '..')}";
        export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
        cd {bug.get_working_dir()};
        defects4j test;
        """
        Cmd_util.run_cmd(cmd)

    def failing_tests(self, bug):
        cmd = f"""
        export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(self.java_home, '..')}";
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
        export PATH="{self.java_home}:{self._get_dataset_path()}:$PATH";
        export JAVA_HOME="{os.path.join(self.java_home, '..')}";
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

    def test_folders(self, bug):
        if bug not in self.proj_data:
            self.set_proj_data(bug)
        source = self.proj_data[bug]["src_test"]
        return [source]

    def bin_folders(self, bug):
        if bug not in self.proj_data:
            self.set_proj_data(bug)
        source = self.proj_data[bug]["bin_java"]
        return [source]

    def test_bin_folders(self, bug):
        if bug not in self.proj_data:
            self.set_proj_data(bug)
        source = self.proj_data[bug]["bin_test"]
        return [source]

    def classpath(self, bug):
        if bug not in self.proj_data:
            self.set_proj_data(bug)
        classpath = Defects4j2_util.join_classpath(self.proj_data[bug], bug)
        return classpath

    # todo
    def failing_module(self, bug):
        return ""

    def compliance_level(self, bug):
        return 8

    def get_patch_diff(self, bug):
        assert len(self.source_folders(bug)) == 1
        buggy_dir = os.path.join(bug.get_working_dir(), self.source_folders(bug)[0])
        fix_dir = Config.TMP_FIXED_DIR
        fixed_dir = self.checkout_fixed(bug, fix_dir)
        ori_fixed_dir = fixed_dir
        fixed_dir = os.path.join(fixed_dir, self.source_folders(bug)[0])
        patch_diff = Cmd_util.run_cmd(f"diff -Naur {buggy_dir} {fixed_dir}")

        # must delete to save / space!
        logger.info(f"rm tmp_checkout_dir: {fixed_dir}")
        File_util.rm_dir_safe_contain(ori_fixed_dir, '/tmp/')

        return patch_diff