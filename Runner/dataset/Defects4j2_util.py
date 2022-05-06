import logging, os

logger = logging.getLogger()


def parse_classpath_test(classpath, tmp_checkout_dir):
    cp_list = classpath.split(":")
    classpath_abs = []
    classpath_relative = []
    for cp in cp_list:
        if tmp_checkout_dir in cp:
            index = cp.index(tmp_checkout_dir)
            assert index == 0
            relative_path = cp[index + len(tmp_checkout_dir):]
            classpath_relative.append(relative_path)
        else:
            if cp in classpath_abs:
                logger.warn(f"repeated cp: {cp}")
            else:
                classpath_abs.append(cp)
    return classpath_abs, classpath_relative


def join_classpath(bug_data_dict, bug):
    bug_dir = bug.get_working_dir()
    classpath = ""

    for relative_cp in bug_data_dict['classpath_relative']:
        rel_path = f'{bug_dir}/{relative_cp}'
        if relative_cp.startswith("/"):
            rel_path = f'{bug_dir}{relative_cp}'

        if not os.path.exists(rel_path):
            logger.error(f"d4j2 join_classpath: {rel_path} does not exist!")
            continue
        else:
            classpath += f"{rel_path}:"

    for abs_cp in bug_data_dict['classpath']:
        if not os.path.exists(abs_cp):
            logger.error(f"d4j2 join_classpath: {abs_cp} does not exist!")
            continue
        else:
            classpath += f"{abs_cp}:"
    return classpath
