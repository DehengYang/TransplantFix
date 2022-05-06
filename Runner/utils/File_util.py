import shutil
import logging
import os, io

from isort import file

logger = logging.getLogger()

MIN_DIR_LEN = 10

def rm_file(file_path):
    if os.path.exists(file_path) and os.path.isfile(file_path):
        os.remove(file_path)
        logger.info(f'rm file: {file_path}')

def rm_files_in_dir(dir_path):
    if len(dir_path) < MIN_DIR_LEN:
        assert False, "dangerous rm."

    logger.info(f'rm files in dir: {dir_path}')
    if os.path.isdir(dir_path):
        for file_name in os.listdir(dir_path):
            file_path = os.path.join(dir_path, file_name)
            if os.path.isfile(file_path):
                os.remove(file_path)

def rm_all_content_in_dir_except(dir_path, except_file_name):
    logger.info(
        f"remove all content in {dir_path} except file: {except_file_name}")

    if len(dir_path) < MIN_DIR_LEN:
        assert False, "dangerous rm."

    for file_name in os.listdir(dir_path):
        file_path = os.path.join(dir_path, file_name)
        if os.path.isfile(file_path) and file_name != except_file_name:
            os.remove(file_path)
        if os.path.isdir(file_path):
            shutil.rmtree(file_path)


def rm_all_content_in_dir(dir_path):
    logger.info(f"remove all content in {dir_path}")

    if len(dir_path) < MIN_DIR_LEN:
        assert False, "dangerous rm."

    for file_name in os.listdir(dir_path):
        file_path = os.path.join(dir_path, file_name)
        if os.path.isfile(file_path):
            os.remove(file_path)
        if os.path.isdir(file_path):
            shutil.rmtree(file_path)

def backup_dir(src_dir, dst_dir):
    assert os.path.isdir(src_dir)
    if os.path.exists(dst_dir):
        shutil.rmtree(dst_dir)
    shutil.copytree(src_dir, dst_dir)

def rm_dir(dir_path):
    if not os.path.exists(dir_path):
        return

    if len(dir_path) < MIN_DIR_LEN:
        assert False, "dangerous rm."

    if os.path.isdir(dir_path):
        logger.info(f"rm dir: {dir_path}")
        shutil.rmtree(dir_path)

def rm_dir_safe_contain(dir_path, contain_word):
    if not os.path.exists(dir_path):
        return
    
    if contain_word not in dir_path:
        assert False

    if len(dir_path) < MIN_DIR_LEN:
        assert False, "dangerous rm."

    if os.path.isdir(dir_path):
        logger.info(f"rm dir: {dir_path}")
        shutil.rmtree(dir_path)


def mk_dir_from_file_path(file_path):
    dir_path = file_path.rsplit(os.sep, 1)[0]
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)

def mk_dir_from_dir_path(dir_path):
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)

def read_file(file_path):
    return read_file_to_str(file_path)


def read_file_to_str(file_path):
    string = ""
    with io.open(file_path, encoding='utf-8', mode='r') as f:
        string = f.read()
    return string


def read_file_to_list_strip(file_path, skip_empty_line = True):
    assert os.path.exists(file_path)

    stripped_lines = []
    with io.open(file_path, encoding='utf-8', mode='r') as f:
        lines = f.readlines()
        for line in lines:
            if len(line.strip()) == 0:
                logger.info("skip empty line.")
                continue
            stripped_lines.append(line.strip())
    return stripped_lines


def read_file_to_list_no_strip(file_path):
    assert os.path.exists(file_path)
    ori_lines = []
    with io.open(file_path, encoding='utf-8', mode='r') as f:
        lines = f.readlines()
        for line in lines:
            ori_lines.append(line)
    return ori_lines


def write_to_file(file_path, string):
    write_str_to_file(file_path, string, append=False)


def write_str_to_file(file_path, string, append=True):
    mk_dir_from_file_path(file_path)

    mode = 'w'
    if append:
        mode = "a+"

    with open(file_path, mode) as f:
        f.write(string)


def write_line_to_file(file_path, line, line_break=True, append=True):
    mk_dir_from_file_path(file_path)

    if line_break:
        line = line + "\n"

    mode = 'w'
    if append:
        mode = "a+"

    with open(file_path, mode) as f:
        f.write(line)


def write_list_to_file(file_path, lines_list, append=False, line_break=True):
    mk_dir_from_file_path(file_path)

    mode = 'w'
    if append:
        mode = "a+"

    with open(file_path, mode) as f:
        for line in lines_list:
            if line_break:
                line = line + "\n"
            f.write(line)

def get_folder_size(folder_path, unit = 'mb', enable_logging=True):
    assert os.path.exists(folder_path)

    total_size = 0
    if os.path.isfile(folder_path):
        total_size += os.path.getsize(folder_path)
    else:
        for dirpath, dirnames, filenames in os.walk(folder_path):
            for f in filenames:
                fp = os.path.join(dirpath, f)
                if not os.path.islink(fp):
                    total_size += os.path.getsize(fp)

    _kB = 1024
    KB = total_size / _kB**1
    MB = total_size / _kB**2
    GB = total_size / _kB**3

    if unit == 'kb':
        total_size = KB
    elif unit == 'mb':
        total_size = MB
    elif unit == 'gb':
        total_size = GB
    else:
        raise Exception
    
    if enable_logging:
        logger.info("size of {}: {:.2f}kb, {:.2f}mb, {:.2f}gb".format(folder_path, KB, MB, GB))

    return total_size