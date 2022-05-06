from nis import match
import os, re, time
from sqlite3 import Time
import shutil

from utils import File_util, Regex_util, Time_util
from transplantfix_parser.utils import Run_util


def transform_fl_txt_path(fl_txt_path):
    # from: org.apache.commons.cli.TypeHandler,100,0.707
    # to: org.jsoup.helper.DataUtil#120 1.0
    dst_fl_txt_path = fl_txt_path + ".transplantfix"
    stmt_list = File_util.read_file_to_list_strip(fl_txt_path)
    new_stmt_lists = []
    for stmt in stmt_list:
        list = stmt.split(",")
        assert len(list) == 3
        new_stmt = f"{list[0]}#{list[1]} {list[2]}"
        new_stmt_lists.append(new_stmt)
    File_util.write_list_to_file(dst_fl_txt_path, new_stmt_lists, False)
    return dst_fl_txt_path


def find_final_patch(apr_dir):
    final_patch_path = os.path.join(apr_dir, "finalPatch.diff")
    if os.path.exists(final_patch_path):
        return True
    return False


def get_valid_patches(apr_dir):
    valid_patch_path = os.path.join(apr_dir, "allValidPatches.diff")
    # [2] --- /mnt
    if os.path.exists(valid_patch_path):
        file_string = File_util.read_file_to_str(valid_patch_path)
        patch_indices = Regex_util.findall(r'\[(.*?)\] --- /mnt', file_string)
        return patch_indices
    return []


def find_partial_fix(apr_dir):
    partial_fix_dir = os.path.join(apr_dir, "partialFixes")
    if os.path.exists(partial_fix_dir):
        for file_name in os.listdir(partial_fix_dir):
            if file_name.startswith("partial_fix_"):
                return True
    return False


def finished_with_no_error(apr_dir):
    error_log_path = os.path.join(apr_dir, "error.log")
    if os.path.exists(error_log_path):
        file_string_list = File_util.read_file_to_list_no_strip(error_log_path)
        for file_string in file_string_list:
            if file_string.startswith("Picked up JAVA_TOOL_OPTIONS: ") or file_string.startswith("SLF4J: "):
                # do nothing
                pass
            else:
                # print(f"find error at {apr_dir}: {file_string}")
                return False
        return True
    else:
        return False


def parse_candidate_patches(apr_dir):
    # /mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Time_3/transplantfix_sfl/patch/candidatePatches.diff
    candidate_path = os.path.join(apr_dir, 'patch/candidatePatches.diff')
    if not os.path.exists(candidate_path):
        return None, None

    candidate_string = File_util.read_file_to_str(candidate_path)
    # ========= 2022-05-02 23:23:06 Patch Index: (suspM: 20)-(donorM: 36) 4678 (compile:
    # all_susp_methdos = []
    # all_candidates = []
    matches = Regex_util.findall(r'========= .* Patch Index: \(suspM: (.*?)\)-\(donorM: .*\) (.*?) \(compile: ',
                                 candidate_string)
    return matches[-1][0], matches[-1][1]


def get_error_log_info(apr_dir):
    error_log_path = os.path.join(apr_dir, "error.log")
    return File_util.read_file_to_str(error_log_path)


def get_transplantfix_log_path_from_apr_dir(apr_dir):
    if os.path.exists(apr_dir):
        for file_name in os.listdir(apr_dir):
            if file_name.startswith("transplantfix_") and file_name.endswith(".log"):
                return os.path.join(apr_dir, file_name)
    return None


def get_transplantfix_log_path_from_bug_dir(bug_dir):
    log_dir = os.path.join(bug_dir, "logs")
    if os.path.exists(log_dir):
        for file_name in os.listdir(log_dir):
            if file_name.startswith("transplantfix_") and file_name.endswith(".log"):
                return os.path.join(log_dir, file_name)
    return None


def copy_transplantfix_log(bug_dir, apr_dir):
    log_path = get_transplantfix_log_path_from_bug_dir(bug_dir)
    if log_path is not None and os.path.exists(log_path):
        shutil.copy(log_path, apr_dir)


def has_method_snippet_info_in_log(apr_dir):
    log_path = get_transplantfix_log_path_from_apr_dir(apr_dir)
    if log_path is None or not os.path.exists(log_path):
        return False
    file_string = File_util.read_file_to_str(log_path)
    if "snippet" in file_string:
        return True
    return False


def parse_time_cost(apr_dir, timeout):
    log_path = get_transplantfix_log_path_from_apr_dir(apr_dir)
    if log_path is not None:
        log_string = File_util.read_file_to_str(log_path)
        # [13:52:23 DirUtil:33 INFO ]
        matches = Regex_util.findall(r'\[(.*?) .*? \]', log_string)
        if len(matches) > 1:  # at least has two dates
            start_time_str = matches[0]
            start_time = Time_util.parse_time_for_hms(start_time_str)
            end_time = Time_util.parse_time_for_hms(matches[-1])

            time_cost = Time_util.get_time_cost(start_time, end_time)
            if time_cost < 0:
                end_time = Time_util.add_a_day(end_time)
                time_cost = Time_util.get_time_cost(start_time, end_time)

            # 1hours
            if time_cost > timeout:
                return time_cost, True
            else:
                return time_cost, False
    return None, None


# execution_framework.log
def parse_time_cost_from_framework_log(apr_dir, timeout):
    log_path = os.path.join(apr_dir, 'execution_framework.log')
    if os.path.exists(log_path):
        log_string = File_util.read_file_to_str(log_path)
        matches = Regex_util.findall(r'--isPerfectFLMode (.*)', log_string, re.S)

        if len(matches) == 0:
            # print(f'{apr_dir} is still running')
            return None, None, False

        # cmd execution time:
        time_costs = Regex_util.findall(r'cmd execution time: (.*?)\n', matches[0])
        if len(time_costs) == 0:
            # print(f'{apr_dir} is still running')
            return None, None, False
        time_cost = float(time_costs[0])
        if time_cost > timeout:
            return time_cost, True, True
        else:
            return time_cost, False, True
    return None, None, False


def is_finished(apr_dir):
    # if "closure_80" in bug_dir.lower():
    #     print()

    log_path = get_transplantfix_log_path_from_apr_dir(apr_dir)
    if log_path is None or not os.path.exists(log_path):
        return False

    file_string_list = File_util.read_file_to_list_no_strip(log_path)
    if len(file_string_list) > 2:
        if 'DEBUG] Exit.' in file_string_list[-1] and 'Main time cost: ' in file_string_list[-2]:
            return True
    return False


def has_buggy_methods(apr_dir):
    repair_log_path = Run_util.get_repair_log_path(apr_dir)
    repair_log_str = File_util.read_file_to_str(repair_log_path)
    perfect_sm_list_size = int(re.findall(r'perfect sm list size: (.*?)\n', repair_log_str)[0])
    return perfect_sm_list_size != 0


def get_bug_ids_from_txt(bug_ids_or_range_file_path):
    # if "bugs_id_to_run" in bug_ids_or_range_file_path:
    bug_ids = File_util.read_file_to_list_strip(bug_ids_or_range_file_path)
    lower_bug_ids = []
    for bug_id in bug_ids:
        lower_bug_ids.append(bug_id.lower())
    return lower_bug_ids
    # else:
    #     raise Exception


def split_bug_ids(bug_ids, total_split_cnt, cur_split_cnt):
    split_size = int(len(bug_ids) / total_split_cnt) + 1
    split_list = []
    for i in range(0, len(bug_ids), split_size):
        split_list.append(bug_ids[i:i + split_size])
    return split_list[cur_split_cnt - 1]
