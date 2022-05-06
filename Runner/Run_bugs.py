import apr
from utils import File_util, Time_util
import Main_util, Config

import os, logging

logger = logging.getLogger()

log_path = os.path.join(Config.PROJECT_PATH, "exceptions.log")


def run_multiple_bugs(tool_name_list, dataset_name, rerun_tool=True, skip_list=[]):
    dataset = Main_util.get_dataset(dataset_name)
    bugs = dataset.get_bugs()

    if not Config.TMP_OUTPUT_DIR.endswith(f"_{dataset_name}"):
        Config.TMP_OUTPUT_DIR = Config.TMP_OUTPUT_DIR + f"_{dataset_name}"

    for bug in bugs:
        proj_name = bug.project.lower()
            
        if bug.get_proj_id().lower() == "lang_8":


            for tool_name in tool_name_list:
                apr_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)


                bug_id = bug.get_proj_id()
                run_single_bug(dataset_name, bug_id, tool_name, rerun_tool, skip_list)
                


def run_single_bug(dataset_name, bug_id, tool_name, rerun_tool, skip_list):
    localizer_name = 'gzoltar'
    dataset = Main_util.get_dataset(dataset_name)
    bug = dataset.get_bug(bug_id)

    apr_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)
    if os.path.isdir(apr_dir) and rerun_tool:
        File_util.rm_dir_safe_contain(apr_dir, f'{tool_name}')


    Main_util.run_single_bug(tool_name, dataset_name, localizer_name, dataset, bug, skip_list)


if __name__ == "__main__":
    tool_name_list = ['mcstudy'] #, 'transplantfix']
    dataset_name = 'defects4j2' #'bears'  #'defects4j'

    Config.rerun_dataset = False
    rerun_tool = True  
    skip_list = ['fl']

    File_util.write_line_to_file(log_path, f"\n\n{Time_util.get_cur_time_str()}")

    run_multiple_bugs(tool_name_list, dataset_name, rerun_tool, skip_list)
