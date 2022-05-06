
import argparse
import sys

import Config
from utils import Logging_util
import Dataset_factory


def init_parser(sys_args):
    parser = argparse.ArgumentParser(prog="dataset.main.py",
                                     description='Process datasets.')
    parser.add_argument('-d',
                        '--dataset',
                        required=True,
                        help='e.g., defects4j')
    parser.add_argument('-b', '--bug_id', required=True, help='e.g., chart_1')
    parser.add_argument('-w',
                        '--working_dir',
                        required=False,
                        help='e.g., ~/test')
    args = parser.parse_args(sys_args)
    return args


def parse_arg(args):
    dataset = Dataset_factory.DatasetFactory().create_dataset(args.dataset)
    bug_id = dataset.get_bug(args.bug_id)
    return dataset, bug_id


if __name__ == "__main__":
    logger = Logging_util.init_logger()

    args = init_parser(sys.argv[1:])
    logger.info("args: {}".format(args))

    dataset, bug = parse_arg(args)
    if args.working_dir is None:
        args.working_dir = Config.TMP_WORKING_DIR
    dataset.checkout(bug, args.working_dir)
    dataset.compile(bug)
    pass