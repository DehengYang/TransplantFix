from transplantfix_parser.utils import Transplantfix_util, Run_util
from utils import File_util, Time_util
import Main_util, Config

import os, logging

logger = logging.getLogger()


def run_single_bug(bug_id,
                   tool_name,
                   dataset_name='defects4j',
                   rerun_tool=False,
                   localizer_name='gzoltar',
                   skip_list=[]):
    dataset = Main_util.get_dataset(dataset_name)
    bug = dataset.get_bug(bug_id)

    if not Config.TMP_OUTPUT_DIR.endswith(f"_{dataset_name}"):
        Config.TMP_OUTPUT_DIR = Config.TMP_OUTPUT_DIR + f"_{dataset_name}"

    apr_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)

    bug = dataset.get_bug(bug_id)

    apr_dir = os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)
    if os.path.isdir(apr_dir) and rerun_tool:
        File_util.rm_dir_safe_contain(apr_dir, f'{tool_name}')

    Main_util.run_single_bug(tool_name, dataset_name, localizer_name, dataset, bug, skip_list)


if __name__ == "__main__":

    dataset_name = 'defects4j2'
    run_single_bug('Compress_15', f'transplantfix_{Config.transplantfix_tool_version}', dataset_name)
    Config.debugMode = False