import apr
from transplantfix_parser.utils import Transplantfix_util
from utils import File_util, Time_util
import Main_util, Config, transplantfix_parser.utils.Run_util as Run_util

import os, logging

logger = logging.getLogger()


def run_multiple_bugs(tool_name_list, dataset_name, bug_ids_to_run, tool_version, rerun_tool=True, skip_list=[]):
    dataset = Main_util.get_dataset(dataset_name)

    bug_ids = Run_util.get_all_bug_ids(dataset_name)
    if not Config.TMP_OUTPUT_DIR.endswith(f"_{dataset_name}"):
        Config.TMP_OUTPUT_DIR = Config.TMP_OUTPUT_DIR + f"_{dataset_name}"
    exceptions_file_path = os.path.join(Config.PROJECT_PATH, "bugs_info", 'transplantfix', f"exceptions.txt")
    File_util.write_line_to_file(exceptions_file_path, f"{Time_util.get_cur_time_str()}")

    cnt = 0
    bug_cnt = 0
    for bug_id in bug_ids:
        bug = dataset.get_bug(bug_id)
        if bug.get_proj_id().lower() in bug_ids_to_run:
            bug_cnt += 1
            print(f"No.{bug_cnt} bug to run: {bug}")

            for tool_name in tool_name_list:
                if tool_version != 0:
                    tool_name = tool_name + "_" + str(tool_version)
                apr_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)
                bug_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, bug.name)
                gzoltar_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, 'gzoltar')


                if os.path.exists(apr_dir):
                    if tool_name.startswith("transplantfix"):
                        if Transplantfix_util.find_final_patch(apr_dir):
                            continue
                        if Transplantfix_util.has_method_snippet_info_in_log(bug_dir):
                            if Transplantfix_util.is_finished(bug_dir) or Transplantfix_util.finished_with_no_error(apr_dir):
                                continue

                bug_id = bug.get_proj_id()

                has_exception = False
                try:
                    run_single_bug(dataset_name, bug_id, tool_name, rerun_tool, skip_list)
                except:  
                    has_exception = True
                finally:
                    find_final_patch = Transplantfix_util.find_final_patch(apr_dir)
                    find_partial_fixes = Transplantfix_util.find_partial_fix(apr_dir)
                    File_util.write_line_to_file(
                        exceptions_file_path,
                        f"{tool_name} {bug.get_proj_id()} has_exception: {has_exception} find_final_patch: {find_final_patch} find_partial_fixes: {find_partial_fixes} time: {Time_util.get_cur_time_str()}"
                    )
                    Transplantfix_util.copy_transplantfix_log(bug_dir, apr_dir)


def run_single_bug(dataset_name, bug_id, tool_name, rerun_tool, skip_list):
    localizer_name = 'gzoltar'
    dataset = Main_util.get_dataset(dataset_name)
    bug = dataset.get_bug(bug_id)

    apr_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)
    if os.path.isdir(apr_dir) and rerun_tool:
        File_util.rm_dir_safe_contain(apr_dir, f'{tool_name}')


    Main_util.run_single_bug(tool_name, dataset_name, localizer_name, dataset, bug, skip_list)


if __name__ == "__main__":
    pass




