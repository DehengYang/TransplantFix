import Run_bugs_dataset, ConfigD4j2, Config
from transplantfix_parser.utils import Transplantfix_util
from utils import Dir_util

import os

cur_dir = Dir_util.get_cur_dir(__file__)
bug_ids_or_range_file_path = os.path.join(cur_dir, ConfigD4j2.bugs_id_to_run_file_name)

total_split_cnt = 1
cur_split_cnt = 1

if __name__ == "__main__":
    tool_name_list = ConfigD4j2.debug_tool_name_list 
    dataset_name = 'defects4j2'
    ConfigD4j2.rerun_dataset = True
    rerun_tool = True  
    skip_list = [] 

    bug_ids = Transplantfix_util.get_bug_ids_from_txt(bug_ids_or_range_file_path)
    bug_ids = Transplantfix_util.split_bug_ids(bug_ids, total_split_cnt, cur_split_cnt)

    Run_bugs_dataset.run_multiple_bugs(tool_name_list, dataset_name, bug_ids, Config.transplantfix_tool_version, rerun_tool,
                                       skip_list)
