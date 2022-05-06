from utils import Cmd_util, File_util
import Main_util, Config

import os, logging

logger = logging.getLogger()

def retry_clean_dir(tool_name_list,
                dataset_name,
                localizer_name,
                bug_range=None,
                test_flag=False,
                bug_info_extra_name=False,
                retry=False):
    dataset = Main_util.get_dataset(dataset_name)
    all_bugs = dataset.get_bugs()

    real_range = [0, len(all_bugs)]

    for tool_name in tool_name_list:
        tool_name = tool_name.lower()

        bug_ids_path = os.path.join(Config.PROJECT_PATH, "bugs_info",
                                    dataset_name + ".txt")
        if bug_info_extra_name:
            bug_ids_path = os.path.join(Config.PROJECT_PATH, "bugs_info",
                                        f"{dataset_name}_{tool_name}.txt")
        assert os.path.exists(bug_ids_path)
        bug_ids = File_util.read_file_to_list_strip(bug_ids_path)

        if bug_range is not None:
            real_range[0] = bug_range[0]
            real_range[1] = bug_range[1]
        if real_range[1] >= len(bug_ids):
            real_range[1] = len(bug_ids)
        logger.info(f"real_range: {real_range}")

        for index in range(real_range[0], real_range[1]):
            bug_id = bug_ids[index]
            bug = dataset.get_bug(bug_id)

            print(f"bug index: {index} {bug.name}")
            parent_output_dir = Config.TMP_OUTPUT_DIR
            apr_dir = os.path.join(parent_output_dir, bug.name, tool_name)
            fl_dir = os.path.join(parent_output_dir, bug.name, localizer_name)
            if retry:
                print(
                    f"retry reparing this bug ({bug.name}) with APR tool ({tool_name}) "
                )
                File_util.rm_dir(apr_dir)

                rank_list_file = os.path.join(fl_dir, "ranking_list.txt")
                if os.path.exists(fl_dir):
                    rm_fl_dir_flag = True
                    if os.path.exists(rank_list_file):
                        rank_list = File_util.read_file_to_str(rank_list_file)
                        if len(rank_list.strip()) != 0:
                            rm_fl_dir_flag = False
                    if rm_fl_dir_flag:
                        File_util.backup_dir(fl_dir, os.path.join(parent_output_dir, bug.name, localizer_name + "_bk"))
                        File_util.rm_dir(fl_dir)


def run_dataset(tool_name_list,
                dataset_name,
                localizer_name,
                bug_range=None,
                test_flag=False,
                bug_info_extra_name=False,
                retry=False):
    dataset = Main_util.get_dataset(dataset_name)
    all_bugs = dataset.get_bugs()

    real_range = [0, len(all_bugs)]

    for tool_name in tool_name_list:
        tool_name = tool_name.lower()

        bug_ids_path = os.path.join(Config.PROJECT_PATH, "bugs_info",
                                    dataset_name + ".txt")
        if bug_info_extra_name:
            bug_ids_path = os.path.join(Config.PROJECT_PATH, "bugs_info",
                                        f"{dataset_name}_{tool_name}.txt")
        assert os.path.exists(bug_ids_path)
        bug_ids = File_util.read_file_to_list_strip(bug_ids_path)

        if bug_range is not None:
            real_range[0] = bug_range[0]
            real_range[1] = bug_range[1]
        if real_range[1] >= len(bug_ids):
            real_range[1] = len(bug_ids)
        logger.info(f"real_range: {real_range}")

        for index in range(real_range[0], real_range[1]):
            bug_id = bug_ids[index]
            bug = dataset.get_bug(bug_id)

            print(f"bug index: {index} {bug.name}")
            parent_output_dir = Config.TMP_OUTPUT_DIR
            log_file_path = os.path.join(parent_output_dir, bug.name,
                                         tool_name, Config.LOG_NAME)
            apr_dir = os.path.join(parent_output_dir, bug.name, tool_name)
            dataset_dir = os.path.join(parent_output_dir, bug.name, bug.name)
            if os.path.exists(apr_dir) and len(
                    os.listdir(apr_dir)) > 1 and os.path.exists(log_file_path):
                print(
                    f"skip reparing this bug ({bug.name}) with APR tool ({tool_name}) "
                )
                print(f"rm {dataset_dir}")
                File_util.rm_dir(dataset_dir)
                continue

            Main_util.run_single_bug(tool_name, dataset_name, localizer_name,
                                     dataset, bug)

            kill_apr_proc(tool_name, bug)
            print(f"rm {dataset_dir}")
            File_util.rm_dir(dataset_dir)
            logger.info(f"bug index: {index} {bug.name} {tool_name}")
        if test_flag:
            break


def kill_apr_proc(tool_name, bug):
    logger.info(f"to kill apr proc: {tool_name} {bug.name}")

    cmd = "ps -ef | grep -i java |grep -i /" + tool_name + " |grep -i /" + bug.name + "/ | grep -v grep"
    Cmd_util.run_cmd_with_output(cmd)

    cmd = "ps -ef | grep -i java |grep -i /" + tool_name + " |grep -i /" + bug.name + "/ | grep -v grep | awk '{print $2}' | xargs -r kill -9"
    Cmd_util.run_cmd_with_output(cmd)

    logger.info(f"kill_apr_proc is done")


if __name__ == "__main__":
    tool_name_list = ['dynamoth_1'
                      ]  #"nopol_1", "simfix_0", 'tbar_0', 'dynamoth_1']
    dataset_name = 'defects4j'  #"quixbugs"
    localizer_name = "gzoltar"
    Config.TMP_OUTPUT_DIR = os.path.join(Config.PARENT_PROJECT_PATH,
                                         f"results_{dataset_name}")

    run_dataset(tool_name_list, dataset_name, localizer_name, [0, 50])
