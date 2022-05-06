import os

import Main_util, Config
from utils import Logging_util

def checkout(dataset_name, bug_name):
    Config.TMP_OUTPUT_DIR = os.path.join(Config.PARENT_PROJECT_PATH,
                                            f"results_{dataset_name}")
    dataset = Main_util.get_dataset(dataset_name)
    all_bugs = dataset.get_bugs()
    cur_bug = None
    for bug in all_bugs:
        if bug.get_proj_id() == bug_name:
            cur_bug = bug
            break
    assert cur_bug is not None

    parent_output_dir = Config.TMP_OUTPUT_DIR
    working_dir = os.path.join(parent_output_dir, bug.name, bug.name)
    bug_pool_dir = os.path.join(parent_output_dir, "pool", bug.name)

    dataset.clean_dir(bug, working_dir)
    dataset.checkout_and_compile(bug, working_dir, bug_pool_dir)
    dataset.compile(bug)

def make_dir(dir):
    if not os.path.exists(dir):
        os.makedirs(dir)

if __name__ == "__main__":
    logger = Logging_util.init_console_logger()

    dataset_name = "defects4j"
    bug_name = "Lang_39"
    checkout(dataset_name, bug_name)