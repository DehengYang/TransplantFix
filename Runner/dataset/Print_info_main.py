import os

import Dataset_factory
from utils import File_util
import Config


def save_datasets_bug_info(dataset_name_list, output_dir):
    for dataset_name in dataset_name_list:
        dataset = Dataset_factory.DatasetFactory().create_dataset(dataset_name)
        bugs = dataset.get_bugs()
        bug_name_list = []
        for bug in bugs:
            bug_name_list.append(bug.get_proj_id())

        print(bug_name_list[0])
        save_path = os.path.join(output_dir, f"{dataset_name}.txt")
        File_util.write_list_to_file(save_path, bug_name_list)


if __name__ == "__main__":
    dataset_name_list = ['defects4j2']
    output_dir = os.path.join(Config.PROJECT_PATH, "bugs_info")
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    save_datasets_bug_info(dataset_name_list, output_dir)