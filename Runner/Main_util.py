
import argparse, sys, os, time
import logging

from utils import Logging_util, Yaml_util, Cmd_util
from dataset import Dataset_factory
from localization import Localizer_factory
from apr import Apr_factory
import Config
from transplantfix_parser.utils import Transplantfix_util

logger = logging.getLogger()


def get_dataset(dataset_name):
    dataset = Dataset_factory.DatasetFactory().create_dataset(dataset_name)
    return dataset


def make_dir(dir):
    if not os.path.exists(dir):
        os.makedirs(dir)


def run_single_bug(tool_name, dataset_name, localizer_name, dataset, bug, skip_list=[]):
    bug_name = bug.get_proj_id()
    init_args = ["-d", dataset_name, "-b", bug_name, "-apr", tool_name, "-localizer", localizer_name]
    args = init_parser(init_args)

    parent_output_dir = Config.TMP_OUTPUT_DIR
    if args.output_dir is not None:
        parent_output_dir = args['output_dir']
    bug_pool_dir = os.path.join(parent_output_dir, "pool", bug.name)
    make_dir(bug_pool_dir)
    bug_copy_dir = os.path.join(parent_output_dir, bug.name, bug.name)
    make_dir(bug_copy_dir)
    args.working_dir = bug_copy_dir
    log_dir = os.path.join(parent_output_dir, bug.name, args.apr_name)
    make_dir(log_dir)
    log_file = os.path.join(log_dir, "execution_framework.log")
    logger = Logging_util.init_logger_with_file(log_file)

    next_args = run_dataset_module(args, dataset, bug, parent_output_dir, bug_pool_dir)

    next_args = run_fl_module(args, next_args, dataset, bug, skip_list)

    next_args['dataset_name'] = dataset_name
    next_args['fault_localizer'] = args.fault_localizer
    next_args['dataset'] = dataset
    next_args['bug'] = bug

    if bug.get_proj_id().lower() == "chart_19":
        Cmd_util.run_cmd("cp /mnt/data/TransplantFix/libs/itext-2.0.6.jar /mnt/data/TransplantFix/results_defects4j/defects4j_Chart_19/defects4j_Chart_19/lib/itext-2.0.6.jar")

    run_apr_module(args, next_args)


def init_parser(sys_args):
    parser = argparse.ArgumentParser(prog="Main", description='Run APR tools for repairing specific bugs/datasets.')
    parser.add_argument('-d', '--dataset', required=True, help='e.g., defects4j')
    parser.add_argument('-b', '--bug_id', required=True, help='e.g., chart_1')
    parser.add_argument('-od', '--output_dir', required=False, help='output folder to save execution results.')
    parser.add_argument('-timeout', '--timeout', required=False, help='repair time budget (in minutes)')
    parser.add_argument('-apr',
                        '--apr_name',
                        required=True,
                        help='available apr tool list: [nopol, simfix, tbar, dynamoth]')
    parser.add_argument('-localizer',
                        '--fault_localizer',
                        required=True,
                        help='available localizer list: [gzoltar_v0.1.1/gzoltar]')  
    args = parser.parse_args(sys_args)
    return args


def run_dataset_module(args, dataset, bug, parent_output_dir, bug_pool_dir):
    output_dir = os.path.join(parent_output_dir, bug.name, "dataset")
    make_dir(output_dir)

    dataset.clean_dir(bug, args.working_dir)  
    dataset.checkout_and_compile(bug, args.working_dir, bug_pool_dir)
    dataset.execute_and_save(bug, output_dir)

    next_args = dataset.get_next_args(bug, args, output_dir)
    dataset.compile(bug)

    return next_args


def run_fl_module(args, fl_args, dataset, bug, skip_list):
    if dataset.name == "defects4j2":
        localizer = Localizer_factory.LocalizerFactory().create_localizer(args.fault_localizer, fl_args)
        logger.warn(f"now pass ranking txt path for d4j2: {bug}")
        next_args = localizer.get_next_args(args, dataset, bug)
        next_args["fl_time_cost"] = 'skiped'
        fl_txt_path = os.path.join(Config.PROJECT_PATH, "../fl_modules", 'location_recoder', 'location2', bug.project,
                                   bug.bug_id, "parsed_ochiai_result")
        if os.path.exists(fl_txt_path):
            next_args["fl_txt_path"] = fl_txt_path
        else:
            logger.error(f'{fl_txt_path} does not exists. now use flacoco fl.')
            fl_txt_path = os.path.join(Config.PROJECT_PATH, "../fl_modules", 'flacoco_experiment',
                                       'results_faultlocalization/defects4jv2/gzoltar',
                                       f'gzoltar_suspicious_{bug.project}{bug.bug_id}.csv')
            if not os.path.exists(fl_txt_path):
                fl_txt_path = os.path.join(Config.PROJECT_PATH, "../fl_modules", 'flacoco_experiment',
                                           'results_faultlocalization/defects4jv2/flacoco',
                                           f'flacoco_suspicious_{bug.project}{bug.bug_id}.csv')
            dst_fl_txt_path = Transplantfix_util.transform_fl_txt_path(fl_txt_path)
            assert os.path.exists(fl_txt_path)
            next_args["fl_txt_path"] = dst_fl_txt_path
    else:
        start_time = time.time()
        localizer = Localizer_factory.LocalizerFactory().create_localizer(args.fault_localizer, fl_args)

        if 'fl' in skip_list:  
            logger.info("Localization is skiped.")
            next_args = localizer.get_next_args(args, dataset, bug)
            next_args["fl_time_cost"] = 'skiped'
            return next_args

        output_yaml = os.path.join(localizer.outputDir, "output_data.yaml")
        if len(os.listdir(localizer.outputDir)) > 0 and os.path.exists(output_yaml):
            logger.info("Localization is already done. Skip localization.")
            pass
        else:
            localizer.clean_dir()
            localizer.localize()

        next_args = localizer.get_next_args(args, dataset, bug)

        logger.info(f"time_cost_of_fl: {(time.time()-start_time)}")

        output_yaml_file = os.path.join(localizer.outputDir, "output_data.yaml")
        next_args["fl_txt_path"] = None
        if os.path.exists(output_yaml_file):
            output_dict = Yaml_util.read_yaml(output_yaml_file)
            next_args["fl_time_cost"] = "{:.4f}".format(
                float(output_dict['time_cost_in_total']) - float(output_dict['time_cost_in_replication']))
        else:
            logger.error(f"Fault localization: output_yaml_file does not exists! ({output_yaml_file})")

    return next_args


def run_apr_module(args, apr_args):
    if "fl_time_cost" not in apr_args:
        if not Config.debugMode:
            logger.info(f"Fault localization failed. Stop repairing this bug with {args.apr_name} now.")
            return

    apr = Apr_factory.AprFactory().create_apr(args.apr_name, apr_args)

    apr.clean_dir()
    apr.repair()


def dataset_init(args):
    dataset = Dataset_factory.DatasetFactory().create_dataset(args.dataset)
    bug = dataset.get_bug(args.bug_id)

    return dataset, bug


if __name__ == "__main__":
    pass








