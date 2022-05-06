import os, Config

from utils import File_util
import Main_util


def set_tmp_output_dir(dataset_name):
    if not Config.TMP_OUTPUT_DIR.endswith(f"_{dataset_name}"):
        Config.TMP_OUTPUT_DIR = Config.TMP_OUTPUT_DIR + f"_{dataset_name}"


def get_all_bug_ids(dataset_name='defects4j', read_uniq = False):
    # for d4j2 unique bugs
    if read_uniq:
        assert dataset_name == Config.D4J2
        assert os.path.exists(Config.d4j2_uniq_bug_id_path)
        all_bug_ids = File_util.read_file_to_list_strip(Config.d4j2_uniq_bug_id_path)
        return all_bug_ids

    bug_ids_path = os.path.join(Config.PROJECT_PATH, "bugs_info", f"{dataset_name}.txt")
    assert os.path.exists(bug_ids_path)
    all_bug_ids = File_util.read_file_to_list_strip(bug_ids_path)
    return all_bug_ids


def get_result_dir(dataset_name='defects4j'):
    if not Config.TMP_OUTPUT_DIR.endswith(f"_{dataset_name}"):
        Config.TMP_OUTPUT_DIR = Config.TMP_OUTPUT_DIR + f"_{dataset_name}"
    return Config.TMP_OUTPUT_DIR


def get_dataset(dataset_name='defects4j'):
    dataset = Main_util.get_dataset(dataset_name)
    return dataset


def get_apr_dir(bug, tool_name):
    return os.path.join(Config.TMP_OUTPUT_DIR, bug.name, tool_name)


def get_bug_dir(bug):
    return os.path.join(Config.TMP_OUTPUT_DIR, bug.name, bug.name)


def get_final_patch_path(apr_dir):
    return os.path.join(apr_dir, 'finalPatch.diff')


def get_repair_log_path(apr_dir):
    if os.path.exists(apr_dir):
        for file_name in os.listdir(apr_dir):
            if file_name.startswith("transplantfix_") and file_name.endswith(".log"):
                return os.path.join(apr_dir, file_name)
    return None